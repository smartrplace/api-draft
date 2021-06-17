package org.ogema.timeseries.eval.simple.api;

import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.devicefinder.util.DPUtil;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.smartrplace.tissue.util.logconfig.PerformanceLog;

import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.util.format.StringFormatHelper;

/** Implementation of {@link ProcessedReadOnlyTimeSeries} that has an input time series and assumes that the 
 * {@link ProcessedReadOnlyTimeSeries} as result shall have exactly one resulting SampledValue for each input
 * time stamp. The result values are requested via {@link #getResultValues(ReadOnlyTimeSeries, long, long, AggregationMode).
 * Further input series may be taken into account by the implementation, but the timestamps must be provided by
 * the input time series.<br>
 * Note that the class is also used to provide results when several input SampledValues are aggregated into
 * one result SampledValue. This usually requires to overwrite some methods.<br>
 * This implementation also provides a resulting TimeSeriesDataImpl and a resulting Datapoint. The label for the
 * new timeseries is created based on #getShortId() and #getLabelPostfix()<br>
 * It is very important to understand that calls to #getResultValues(ReadOnlyTimeSeries, long, long, AggregationMode) and
 * further calculations are NOT triggered by calling #getResultSeries(), but by calling the #getValues(long, long) method of
 * the timeseries (triggered potentially by any access to the standard ReadOnlyTimeseries API).
 * 
 * @author dnestle
 *
 */
public abstract class ProcessedReadOnlyTimeSeries2 extends ProcessedReadOnlyTimeSeries {
	
	public static PerformanceLog uv2Log;

	/** This method is called to obtain the calculated values. The method may be called again for the same
	 * time stamps.
	 * @param timeSeries
	 * @param start
	 * @param end
	 * @param mode
	 * @return
	 */
	protected abstract List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start, long end,
			AggregationMode mode);
	
	/** change startTime and endTime of parameter if necessary*/
	protected abstract void alignUpdateIntervalFromSource(DpUpdated updateInterval);
	
	protected String getLabelPostfix() {return "";}
	/** The result is appended to the id of the input time series to create the output datapoint location*/
	protected String getLocationPostifx() {return getLabelPostfix();}
	
	//final protected MonitoringController controller;
	final private TimeSeriesDataImpl tsdi;
	final private Datapoint dpInput;
	
	/** only relevant if dp == null*/
	final protected TimeSeriesNameProvider nameProvider;
	
	//final protected ProcessedReadOnlyTimeSeries newTs;
	final protected AggregationMode mode;
	
	private Long firstTimestampInSource = null;
	private Long lastTimestampInSource = null;
	/** For {@link ProcessedReadOnlyTimeSeries2} this is always true*/
	private boolean updateLastTimestampInSourceOnEveryCall;
	public Long getTimeStampInSourceInternal(boolean first) {
		if(first)
			return firstTimestampInSource;
		return lastTimestampInSource;
	}

	protected long lastGetValues = 0;
	
	@Override
	public List<SampledValue> getValues(long startTime, long endTime) {
		DpUpdated changed = dpInput.getSingleIntervalChanged(lastGetValues);
		if(changed != null) {
			addIntervalToUpdate(changed);
		}
		lastGetValues = getCurrentTime();
		return super.getValues(startTime, endTime);
	}
	
	/*public ProcessedReadOnlyTimeSeries2(TimeSeriesDataImpl tsdi, TimeSeriesNameProvider nameProvider,
			MonitoringController controller) {
		this(tsdi, nameProvider, getMode(controller, tsdi.label(null)));
	}*/
	public ProcessedReadOnlyTimeSeries2(TimeSeriesNameProvider nameProvider,
			AggregationMode mode) {
		this(nameProvider, mode, null);
	}
	public ProcessedReadOnlyTimeSeries2(TimeSeriesNameProvider nameProvider,
			AggregationMode mode, Datapoint dpInput) {
		this(nameProvider, mode, dpInput, !Boolean.getBoolean("org.ogema.timeseries.eval.simple.api.noUpdateLastTimestampInSource"));
		
	}
	public ProcessedReadOnlyTimeSeries2(TimeSeriesNameProvider nameProvider,
			AggregationMode mode, Datapoint dpInput,
			boolean updateLastTimestampInSourceOnEveryCall) {
		this(nameProvider, mode, dpInput, updateLastTimestampInSourceOnEveryCall, null, null);
	}
	/**
	 * 
	 * @param tsdi
	 * @param nameProvider
	 * @param mode
	 * @param dp
	 * @param updateLastTimestampInSourceOnEveryCall if false (default) then the input time series is only read once to determine start and end
	 * 		otherwise the end is udpated on every call, the start is not updated, though
	 */
	public ProcessedReadOnlyTimeSeries2(TimeSeriesNameProvider nameProvider,
			AggregationMode mode, Datapoint dpInput,
			boolean updateLastTimestampInSourceOnEveryCall,
			Integer absoluteTiming,
			Long minIntervalForReCalc) {
		super(InterpolationMode.NONE, absoluteTiming, minIntervalForReCalc);
		this.nameProvider = nameProvider;
		this.tsdi = dpInput.getTimeSeriesDataImpl(null); //tsdi;
		this.dpInput = dpInput;
		this.mode = mode;
		this.setUpdateLastTimestampInSourceOnEveryCall(updateLastTimestampInSourceOnEveryCall);
	}

	public ProcessedReadOnlyTimeSeries2(Datapoint dpInput) {
		this(null, dpInput.info().getAggregationMode(), dpInput);
	}
	public ProcessedReadOnlyTimeSeries2(Datapoint dpInput, Integer absoluteTiming, Long minIntervalForReCalc) {
		this(null, dpInput.info().getAggregationMode(), dpInput,
				!Boolean.getBoolean("org.ogema.timeseries.eval.simple.api.noUpdateLastTimestampInSource"), absoluteTiming,
				minIntervalForReCalc);
	}
	
	/*static AggregationMode getMode(MonitoringController controller, String label) {
		final String cparam = controller.getConfigParam(label);
		if(cparam != null && cparam.contains(AggregationMode.Consumption2Meter.name()))
			return AggregationMode.Consumption2Meter;
		else
			return AggregationMode.Meter2Meter;		
	}*/

	@Override
	protected List<SampledValue> updateValues(long start, long end) {
long startCalc = getCurrentTime();
		if(Boolean.getBoolean("evaldebug")) System.out.println("updateValues for  "+dpLabel()+" "+TimeProcPrint.getFullTime(start)+" : "+TimeProcPrint.getFullTime(end));
		ReadOnlyTimeSeries ts = tsdi.getTimeSeries();
		if(firstTimestampInSource == null) {
			SampledValue sv = ts.getNextValue(0);
			if(sv != null)
				firstTimestampInSource = sv.getTimestamp();
			else
				return Collections.emptyList();
		}
		if(lastTimestampInSource == null || updateLastTimestampInSourceOnEveryCall) {
long startCalc2 = getCurrentTime();
if(uv2Log != null) uv2Log.logEvent(startCalc2-startCalc, "Calculation of INI "+getShortId()+" took");
			SampledValue sv = ts.getPreviousValue(Long.MAX_VALUE);
			if(sv != null) {
				lastTimestampInSource = sv.getTimestamp();
				logger.info("Found last input time stamp:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastTimestampInSource)+" for "+dpLabel());
				if(ts instanceof RecordedData) {
					RecordedData rec = (RecordedData) ts;
					logger.info("Read from "+rec.getPath());
				} else
					logger.info("Read from no-RecordedData values: "+dpLabel());
long endCalc2 = getCurrentTime();
if(uv2Log != null) uv2Log.logEvent(endCalc2-startCalc2, "Calculation of RED "+getShortId()+" took");
			} else if(lastTimestampInSource == null)
				return Collections.emptyList();
		}
		if(end < firstTimestampInSource) {
			if(Boolean.getBoolean("evaldebug")) System.out.println("end < firstTimestampInSource for  "+dpLabel()+" firstTsInS:"+TimeProcPrint.getFullTime(firstTimestampInSource));
			return Collections.emptyList();
		}
		if(start > lastTimestampInSource) {
			if(Boolean.getBoolean("evaldebug")) System.out.println("start > lastTimestampInSource for  "+dpLabel()+" lastsInS:"+TimeProcPrint.getFullTime(lastTimestampInSource));
			return Collections.emptyList();
		}
		if(end > lastTimestampInSource)
			end = lastTimestampInSource;
		if(start < firstTimestampInSource)
			start = firstTimestampInSource;
logger.info("Starting getResultValues for PROT:"+getInputDp().label(null)+getLabelPostfix()+" from "+StringFormatHelper.getFullTimeDateInLocalTimeZone(start)+
		" to "+StringFormatHelper.getFullTimeDateInLocalTimeZone(end+1));
		//we want end inclusive, so we have to add 1
long startCalc3 = getCurrentTime();
		List<SampledValue> result = getResultValues(ts, start, end+1, mode);
long endCalc = getCurrentTime();
if(uv2Log != null) uv2Log.logEvent(endCalc-startCalc3, "Calculation of REV "+getShortId()+" took");
if(uv2Log != null) uv2Log.logEvent(endCalc-startCalc, "Calculation of UV2 "+getShortId()+" took");
		return result;
	}

	@Override
	public String dpLabel() {
		if(datapointForChangeNotification != null) {
			String loc = datapointForChangeNotification.getLocation();
			Matcher matcher = Pattern.compile("\\d+").matcher(loc);
			matcher.find();
			try {
				String num = matcher.group();
				if(num != null)
					return datapointForChangeNotification.label(null)+"_"+num;
			} catch(IllegalStateException e) {}
			return datapointForChangeNotification.label(null);
		}
		return getShortId();
	}
	
	public String getShortId() {
		return getShortId(tsdi, nameProvider, getInputDp());
	}
	
	public static String getShortId(TimeSeriesDataImpl tsdi, TimeSeriesNameProvider nameProvider,
			Datapoint dp) {
		String shortId = tsdi.label(null);
		if(tsdi instanceof TimeSeriesDataExtendedImpl) {
			TimeSeriesDataExtendedImpl tse = (TimeSeriesDataExtendedImpl) tsdi;
			if(tse.type != null && tse.type instanceof GaRoDataTypeI) {
				GaRoDataTypeI dataType = (GaRoDataTypeI) tse.type;
				if(nameProvider != null)
					shortId = nameProvider.getShortNameForTypeI(dataType, tse);
				else
					shortId = dp.label(null);
			}
		}
		return shortId;		
	}
	
	public TimeSeriesDataExtendedImpl getResultSeries() {
		return new TimeSeriesDataExtendedImpl(this,
				getShortId()+getLabelPostfix(), tsdi.description(null)+getLabelPostfix(), InterpolationMode.NONE);
	}
	
	//public DatapointImpl getResultSeriesDP() {
	//	return getResultSeriesDP(null);
	//}
	public DatapointImpl getResultSeriesDP(DatapointService dpService, String location) {
		DatapointImpl result;
		if(location != null) {
			result = (DatapointImpl) dpService.getDataPointStandard(location);
		} else {
			String tsLocationOrBaseId;
			if(getInputDp() != null) {
				tsLocationOrBaseId = getDpLocation(getInputDp(), getLabelPostfix()); //getDp().getLocation()+getLocationPostifx();
			} else {
				tsLocationOrBaseId = tsdi.id()+getLocationPostifx();
			}
			if(dpService == null)
				throw new IllegalStateException("Operation without DatapointService not supported anymore(2)!");
				//result = new DatapointImpl(this, tsLocationOrBaseId, label, false);
			else {
				result = (DatapointImpl) dpService.getDataPointStandard(tsLocationOrBaseId);
			}
		}
		String label;
		if(getInputDp() != null) {
			label = getInputDp().label(null)+getLabelPostfix();
		} else {
			label = getShortId()+getLabelPostfix();
		}
		result.setLabel(label, null);
		result.setTimeSeries(this, false);
		DPUtil.copyExistingDataRoomDevice(getInputDp(), result);
		result.info().setAggregationMode(AggregationMode.Consumption2Meter);
		result.info().setInterpolationMode(InterpolationMode.NONE);
		datapointForChangeNotification = result;
		return result ;
	}
	
	/** The dpInput is only used to determine changes on the input time series and potentially to provide a scale. For the general calculation
	 * 		tsdi is used.
	 * 		Deprecated note as tsdi cannot be set via contructor anymore: If the constructor taking only a datapoint as input is used then tsdi is the timeseries
	 * 		of the datapoint, but via other constructors these can be separated and dpInput does not need to be set at all.
	 * @return
	 */
	public Datapoint getInputDp() {
		return dpInput;
	}
	
	public TimeSeriesDataImpl getTSDI() {
		return tsdi;
	}
	
	public static String getDpLocation(Datapoint dpSource, String locationPostfix) {
if(dpSource == null || dpSource.getLocation() == null || locationPostfix == null)
System.out.println(dpSource.getLocation());	
		return getDpLocation(dpSource.getLocation(), locationPostfix);
	}
	public static String getDpLocation(String dpSource, String locationPostfix) {
		return dpSource+locationPostfix;
	}
	/** See constructor documentation*/
	public boolean isUpdateLastTimestampInSourceOnEveryCall() {
		return updateLastTimestampInSourceOnEveryCall;
	}
	/** See constructor documentation*/
	public void setUpdateLastTimestampInSourceOnEveryCall(boolean updateLastTimestampInSourceOnEveryCall) {
		this.updateLastTimestampInSourceOnEveryCall = updateLastTimestampInSourceOnEveryCall;
	}
}
