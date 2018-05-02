package org.ogema.util.directresourcegui.kpi;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.alignedinterval.StatisticalAggregation;
import org.ogema.serialization.jaxb.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.analysis.backup.parser.api.GatewayBackupAnalysis;

import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.extended.MultiEvaluationInstance;
import de.iwes.timeseries.eval.api.extended.MultiResult;
import de.iwes.timeseries.eval.api.extended.util.AbstractSuperMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoEvalProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiResult;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiResult.RoomData;
import de.iwes.timeseries.eval.garo.api.base.GaRoPreEvaluationProvider;
import de.iwes.timeseries.eval.garo.api.jaxb.GenericGaRoMultiProviderJAXB;
import de.iwes.timeseries.eval.garo.helper.jaxb.GaRoEvalHelperJAXB;
import de.iwes.timeseries.eval.garo.helper.jaxb.GaRoEvalHelperJAXB.CSVArchiveExporter;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProvider;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProviderPreEvalRequesting;
import de.iwes.timeseries.eval.garo.multibase.GaRoSingleEvalProviderPreEvalRequesting.PreEvaluationRequested;
import de.iwes.util.timer.AbsoluteTimeHelper;

public class KPIStatisticsUtil {
	//Default implementation, not taking into account other modes than average
	protected Map<String, Float> getKPIs(GaRoMultiResult singleTimeStepResult) {
		Map<String, Float> result = new HashMap<>();
		class AggData {
			float sum = 0;
			int countGw = 0;
		}
		Map<String, AggData> aggData = new HashMap<>();
		for(RoomData rd: singleTimeStepResult.roomEvals) {
			for(Entry<String, String> resData: rd.evalResults.entrySet()) {
				try {
					float val = Float.parseFloat(resData.getValue());
					if(Float.isNaN(val)) continue;
					AggData ad = aggData.get(resData.getKey());
					if(ad == null) {
						ad = new AggData();
						aggData.put(resData.getKey(), ad);
					}
					ad.sum += val;
					ad.countGw++;
				} catch(NumberFormatException e) {
					//do nothing
				}
			}
		}
		for(Entry<String, AggData> ad: aggData.entrySet()) {
			final float value;
			if(ad.getValue().countGw == 0) value = Float.NaN;
			else value = ad.getValue().sum /  ad.getValue().countGw;
			result.put(ad.getKey(), value);
		}
		return result ;
	};
	
	public final ResultType resultType;
	public final GaRoSingleEvalProvider provider;
	protected final StatisticalAggregation sAgg;
	protected final boolean forceCalculations;
	
	protected Map<String, KPIStatisticsUtil> moreResultsOfProvider = new HashMap<>();
	protected final GatewayBackupAnalysis gatewayParser;

	public KPIStatisticsUtil(ResultType resultType, GaRoSingleEvalProvider provider,
			StatisticalAggregation sAgg, boolean forceCalculations, GatewayBackupAnalysis gatewayParser) {
		this.sAgg = sAgg;
		this.forceCalculations = forceCalculations;
		this.resultType = resultType;
		this.provider = provider;
		this.gatewayParser = gatewayParser;
	}
	
	public Schedule getIntervalSchedule(int intervalType) {
		FloatResource parent = AbsoluteTimeHelper.getIntervalTypeStatistics(intervalType, sAgg);
		if(parent == null) return null;
		if(!parent.exists()) {
			parent.create();
			parent.historicalData().create();
			parent.activate(true);
		} else if(!parent.historicalData().exists()) {
			parent.historicalData().create();
			parent.activate(true);			
		}
		return parent.historicalData();
	}
	
	//TODO: The efficiency of the following methods could be improved by storing relevant aligned times
	//TODO: When calculation takes place check whether data from higher resolution aggregation can be used
	//  to avoid touching raw data again
	public SampledValue getValue(int intervalType, long alignedNow, int intervalsIntoPast, long realNow) {
		Schedule sched = getIntervalSchedule(intervalType);
		if(sched == null) return null;
		long destTime = AbsoluteTimeHelper.addIntervalsFromAlignedTime(alignedNow, -intervalsIntoPast, intervalType);
		SampledValue val = sched.getValue(destTime);
		if(val == null && forceCalculations) {
			calculateForTimeSpan(destTime, realNow, intervalType, sched);
			/*AbstractSuperMultiResult<MultiResult> result = performGenericMultiEvalOverAllDataBlocking(provider.getClass(),
					gatewayParser,
					destTime, realNow,
					getIntervalTypeChronoUnit(intervalType),
					null, true, null, provider.resultTypes(), null, null);
			long resultTime = destTime;
			while(resultTime <= realNow) {
				boolean found = false;
				for(MultiResult ir: result.intervalResults) {
					if(ir.getStartTime() == resultTime) {
						if(!(ir instanceof GaRoMultiResult)) throw new IllegalStateException("No GaRo result!");
						GaRoMultiResult irGR = (GaRoMultiResult)ir;
						Map<String, Float> resultForTimeStep = getKPIs(irGR);
						Float newVal = setAllKPIs(resultForTimeStep , resultTime, intervalType);
						if(newVal == null) break; //NaN will be set
						sched.addValue(resultTime, new FloatValue(newVal));
						found = true;
						break;
					}
				}
				if(!found) sched.addValue(resultTime, new FloatValue(Float.NaN));
				resultTime = AbsoluteTimeHelper.getNextStepTime(resultTime, intervalType);
			}*/
		}
		return val ;
	}
	
	public void calculateForTimeSpan(long startTime, long endTime, int intervalType,
			Schedule destinationSchedule) {
		if(destinationSchedule == null) destinationSchedule = getIntervalSchedule(intervalType);
		AbstractSuperMultiResult<MultiResult> result = performGenericMultiEvalOverAllDataBlocking(provider.getClass(),
				gatewayParser,
				startTime, endTime,
				getIntervalTypeChronoUnit(intervalType),
				null, true, null, provider.resultTypes(), null, null);
		long resultTime = startTime;
		while(resultTime <= endTime) {
			boolean found = false;
			for(MultiResult ir: result.intervalResults) {
				if(ir.getStartTime() == resultTime) {
					if(!(ir instanceof GaRoMultiResult)) throw new IllegalStateException("No GaRo result!");
					GaRoMultiResult irGR = (GaRoMultiResult)ir;
					Map<String, Float> resultForTimeStep = getKPIs(irGR);
					Float newVal = setAllKPIs(resultForTimeStep , resultTime, intervalType);
					if(newVal == null) break; //NaN will be set
					destinationSchedule.addValue(resultTime, new FloatValue(newVal));
					found = true;
					break;
				}
			}
			if(!found) destinationSchedule.addValue(resultTime, new FloatValue(Float.NaN));
			resultTime = AbsoluteTimeHelper.getNextStepTime(resultTime, intervalType);
		}		
	}
	
	public SampledValue getValueNonAligned(int intervalType, long nonAlignedNow, int intervalsIntoPast) {
		long alignedNow = AbsoluteTimeHelper.getIntervalStart(nonAlignedNow, intervalsIntoPast);
		return getValue(intervalType, alignedNow, intervalsIntoPast, nonAlignedNow);
	}

	public void addKPIHelperForSameProvider(KPIStatisticsUtil otherUtil) {
		this.moreResultsOfProvider.put(otherUtil.resultType.id(), otherUtil);
	}
	
	public void addValue(float value, long timeStamp, int intervalType) {
		Schedule sched = getIntervalSchedule(intervalType);
		sched.addValue(timeStamp, new FloatValue(value));
	}
	
	/**
	 * 
	 * @param resultForTimeStep
	 * @param timeStamp
	 * @return The value relevant to this result KPI helper
	 */
	private Float setAllKPIs(Map<String, Float> resultForTimeStep, long timeStamp, int intervalType) {
		for(Entry<String, KPIStatisticsUtil> other: moreResultsOfProvider.entrySet()) {
			Float res = resultForTimeStep.get(other.getKey());
			if(res == null) other.getValue().addValue(Float.NaN, timeStamp, intervalType);
			else other.getValue().addValue(res, timeStamp, intervalType);
		}
		return resultForTimeStep.get(resultType.id());
	}
	
	public static ChronoUnit getIntervalTypeChronoUnit(int intervalType) {
		switch(intervalType) {
		case 1:
			return ChronoUnit.YEARS;
		case 3:
			return  ChronoUnit.MONTHS;
		case 6:
			return  ChronoUnit.WEEKS;
		case 10:
			return  ChronoUnit.DAYS;
		case 100:
			return  ChronoUnit.HOURS;
		case 101:
			return  ChronoUnit.MINUTES;
		case 102:
			return  ChronoUnit.SECONDS;
		default:
			throw new UnsupportedOperationException("Interval type "+intervalType+" not supported as ChronoUnit!");
		}
	}
	
	public static <T extends MultiResult, P extends GaRoSingleEvalProvider> AbstractSuperMultiResult<T> performGenericMultiEvalOverAllDataBlocking(Class<P> singleEvalProvider,
			GatewayBackupAnalysis gatewayParser, long startTime,
			long endTime, ChronoUnit resultStepSize, CSVArchiveExporter doExportCSV, boolean doBasicEval,
			GaRoPreEvaluationProvider[] preEvalProviders, List<ResultType> resultsRequested, List<String> gwIds,
			String resultFileName) {
		try {
			P singleProvider = singleEvalProvider.newInstance();
			if(singleProvider instanceof GaRoSingleEvalProviderPreEvalRequesting) {
				GaRoSingleEvalProviderPreEvalRequesting preEval = (GaRoSingleEvalProviderPreEvalRequesting)singleProvider;
				int i= 0;
				for(PreEvaluationRequested req: preEval.preEvaluationsRequested()) {
					preEval.preEvaluationProviderAvailable(i, req.getSourceProvider(), preEvalProviders[i]);
					i++;
				}
			}
			AbstractSuperMultiResult<T> result = performEvaluation((
					new GenericGaRoMultiProviderJAXB<P>(singleProvider, doBasicEval)),
				gatewayParser, startTime, endTime, resultStepSize,
				resultFileName!=null?resultFileName:singleEvalProvider.getSimpleName()+"Result.json", doExportCSV, resultsRequested, gwIds);
			return result;
		} catch(InstantiationException | IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends MultiResult> AbstractSuperMultiResult<T> performEvaluation(GaRoEvalProvider<Resource, ?> garoEval, GatewayBackupAnalysis gatewayParser, long startTime,
			long endTime, ChronoUnit resultStepSize,
			String jsonOutFileName, CSVArchiveExporter doExportCSV,
			List<ResultType> resultsRequested, List<String> gwIds) {

		MultiEvaluationInstance<Resource, T> eval =
		(MultiEvaluationInstance<Resource, T>) GaRoEvalHelperJAXB.startGaRoMultiEvaluationOverAllData(startTime, endTime, resultStepSize,
    				garoEval, gatewayParser, resultsRequested, gwIds, null);
        AbstractSuperMultiResult<T> result = eval.getResult();
        
        System.out.printf("evaluation runs done: %d\n", result.intervalResults.size());
		return result;
	}
	
	public static StatisticalAggregation getCreateStatAgg(String resultName, ResourceList<StatisticalAggregation> resList) {
		for(StatisticalAggregation sAgg: resList.getAllElements()) {
			if(sAgg.name().getValue().equals(resultName)) return sAgg;
		}
		StatisticalAggregation result = resList.addDecorator(ResourceUtils.getValidResourceName(resultName), StatisticalAggregation.class);
		result.name().<StringResource>create().setValue(resultName);
		result.activate(true);
		return result;
	}
	
	public static List<KPIResultType> setupKPIUtilsForProvider(GaRoSingleEvalProvider provider,
			GatewayBackupAnalysis gatewayParser, ResourceList<StatisticalAggregation> resultKPIs) {
		List<KPIResultType> kpiList = new ArrayList<>();
	    for(ResultType resultType: provider.resultTypes()) {
			StatisticalAggregation sAgg = KPIStatisticsUtil.getCreateStatAgg(resultType.id(), resultKPIs);
			KPIResultType kpi = new KPIResultType(resultType, provider, sAgg, true, gatewayParser);
			kpiList.add(kpi);
			kpiList.add(kpi);
		}
	    for(KPIResultType kpi: kpiList) {
	    	ArrayList<KPIResultType> newList = new ArrayList<>(kpiList);
	    	newList.remove(kpi);
	    	for(KPIResultType other: newList) kpi.addKPIHelperForSameProvider(other);
	    }
		return kpiList;
	}
}
