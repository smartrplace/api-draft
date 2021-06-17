package org.ogema.timeseries.eval.simple.mon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.AggregationModeProvider;
import org.ogema.devicefinder.util.DPUtil;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries2;
import org.ogema.timeseries.eval.simple.mon3.TimeseriesSetProcMultiToSingle3;
import org.smartrplace.gateway.device.MemoryTimeseriesPST;
import org.smartrplace.tissue.util.logconfig.PerformanceLog;
import org.smartrplace.tissue.util.logconfig.PerformanceLog.GwSubResProvider;

import de.iwes.timeseries.eval.api.TimeSeriesData;

public abstract class TimeseriesSimpleProcUtilBase {
	protected final Map<String, TimeseriesSetProcessor> knownProcessors = new HashMap<>();
	public TimeseriesSetProcessor getProcessor(String procID) {
		return knownProcessors.get(procID);
	}
	
	protected final ApplicationManager appMan;
	public final DatapointService dpService;
	
	public TimeseriesSimpleProcUtilBase(ApplicationManager appMan, DatapointService dpService) {
		this.appMan = appMan;
		this.dpService = dpService;
		
		TimeseriesSetProcMultiToSingle3.tsSingleLog = PerformanceLog.getInstance(true, appMan, TimeseriesSetProcMultiToSingle3.class.getName()+"_TSI",
				new GwSubResProvider() {
					
					@Override
					public FloatResource getEventResource(MemoryTimeseriesPST device) {
						return device.pstMultiToSingleEvents();
					}
					
					@Override
					public FloatResource getCounterResource(MemoryTimeseriesPST device) {
						return device.pstMultiToSingleCounter();
					}
				});
		TimeseriesSetProcMultiToSingle3.aggregateLog = PerformanceLog.getInstance(true, appMan, TimeseriesSetProcMultiToSingle3.class.getName()+"_AGG",
				new GwSubResProvider() {
					
					@Override
					public FloatResource getEventResource(MemoryTimeseriesPST device) {
						return device.pstMultiToSingleAggregations();
					}
					@Override
					public FloatResource getCounterResource(MemoryTimeseriesPST device) {
						return device.pstMultiToSingleAggregationsCounter();
					}
				});

		ProcessedReadOnlyTimeSeries.lockLog = PerformanceLog.getInstance(true, appMan, ProcessedReadOnlyTimeSeries.class.getName()+"_LCK",
				new GwSubResProvider() {
					
					@Override
					public FloatResource getEventResource(MemoryTimeseriesPST device) {
						return device.pstBlockingSingeEvents();
					}
					
					@Override
					public FloatResource getCounterResource(MemoryTimeseriesPST device) {
						return device.pstBlockingCounter();
					}
				});
		ProcessedReadOnlyTimeSeries.subTsBuildLog = PerformanceLog.getInstance(true, appMan, ProcessedReadOnlyTimeSeries.class.getName()+"_SUB",
				new GwSubResProvider() {
					
					@Override
					public FloatResource getEventResource(MemoryTimeseriesPST device) {
						return device.pstSubTsBuild();
					}
					
					@Override
					public FloatResource getCounterResource(MemoryTimeseriesPST device) {
						return device.pstSubTsBuildCounter();
					}
				});
		ProcessedReadOnlyTimeSeries2.uv2Log = PerformanceLog.getInstance(true, appMan, ProcessedReadOnlyTimeSeries2.class.getName()+"_UV2",
				new GwSubResProvider() {
					
					@Override
					public FloatResource getEventResource(MemoryTimeseriesPST device) {
						return device.pstUpdateValuesPS2();
					}
					
					@Override
					public FloatResource getCounterResource(MemoryTimeseriesPST device) {
						return device.pstUpdateValuesPS2Counter();
					}
				});
		TimeSeriesServlet.tsServletLog = PerformanceLog.getInstance(true, appMan, TimeSeriesServlet.class.getName()+"_TSS",
				new GwSubResProvider() {
					
					@Override
					public FloatResource getEventResource(MemoryTimeseriesPST device) {
						return device.pstTSServlet();
					}
					
					@Override
					public FloatResource getCounterResource(MemoryTimeseriesPST device) {
						return device.pstTSServletCounter();
					}
				});
	}
	public List<Datapoint> process(String tsProcessRequest, List<Datapoint> input) {
		TimeseriesSetProcessor proc = knownProcessors.get(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		return proc.getResultSeries(input, dpService);
	}
	
	/** Regarding calculation notes see {@link TimeseriesSetProcMultiToSingle3}
	 * 
	 * @param tsProcessRequest
	 * @param input
	 * @return
	 */
	public Datapoint processMultiToSingle(String tsProcessRequest, List<Datapoint> input) {
		TimeseriesSetProcessor proc = knownProcessors.get(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		List<Datapoint> resultTs = proc.getResultSeries(input, dpService);
		if(resultTs != null && !resultTs.isEmpty())
			return resultTs.get(0);
		return null;
	}
	
	public Datapoint processSingle(String tsProcessRequest, Datapoint dp) {
		TimeseriesSetProcessor proc = getProcessor(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(new Datapoint[] {dp}), dpService);
		if(resultTs != null && !resultTs.isEmpty())
			return resultTs.get(0);
		return null;
	}
	
	/** Variant of {@link #processSingle(String, Datapoint)} allowing to pass an object containing parameters
	 * that shall be processed
	 * @param tsProcessRequest must reference to a TimeseriesSetProcessorArg (taking argument object)
	 * @param dp
	 * @param params
	 * @return
	 */
	public <T> Datapoint processSingle(String tsProcessRequest, Datapoint dp, T params) {
		TimeseriesSetProcessor procRaw = getProcessor(tsProcessRequest);
		if(procRaw == null || (!(procRaw instanceof TimeseriesSetProcessorArg)))
			throw new IllegalArgumentException("Unknown or unfitting timeseries processor: "+tsProcessRequest);
		@SuppressWarnings("unchecked")
		TimeseriesSetProcessorArg<T> proc = (TimeseriesSetProcessorArg<T>) procRaw;
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(new Datapoint[] {dp}), dpService, params);
		if(resultTs != null && !resultTs.isEmpty())
			return resultTs.get(0);
		return null;
	}

	/** For processing of two or more aligned time series that are not interchangeable like difference, division,...*/
	public Datapoint processArgs(String tsProcessRequest, Datapoint... dp) {
		TimeseriesSetProcessor proc = getProcessor(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(dp), dpService);
		if(resultTs != null && !resultTs.isEmpty())
			return resultTs.get(0);
		return null;
	}

	public List<Datapoint> processSingleToMulti(String tsProcessRequest, Datapoint dp) {
		List<Datapoint> result = new ArrayList<>();
		TimeseriesSetProcessor proc = getProcessor(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(new Datapoint[] {dp}), dpService);
		if(resultTs != null && !resultTs.isEmpty())
			result.addAll(resultTs);
		
		return result;
	}

	public List<TimeSeriesData> processTSD(String tsProcessRequest, List<TimeSeriesData> input,
			TimeSeriesNameProvider nameProvider, AggregationModeProvider aggProv) {
		return DPUtil.getTSList(process(tsProcessRequest, DPUtil.getDPList(input, nameProvider, aggProv)), null);
	}
}
