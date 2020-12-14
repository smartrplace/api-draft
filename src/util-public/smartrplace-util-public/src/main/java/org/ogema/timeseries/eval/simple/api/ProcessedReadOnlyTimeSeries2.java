package org.ogema.timeseries.eval.simple.api;

import java.util.Collections;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DPUtil;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;

import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;

/** Implementation of {@link ProcessedReadOnlyTimeSeries} that has an input time series and assumes that the 
 * {@link ProcessedReadOnlyTimeSeries} as result shall have exactly one resulting SampledValue for each input
 * time stamp. The result values are requested via {@link #getResultValues(ReadOnlyTimeSeries, long, long, AggregationMode).
 * Further input series may be taken into account by the implementation, but the timestamps must be provided by
 * the input time series.<br>
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
	
	protected abstract List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start, long end,
			AggregationMode mode);
	protected String getLabelPostfix() {return "";}
	/** The result is appended to the id of the input time series to create the output datapoint location*/
	protected String getLocationPostifx() {return getLabelPostfix();}
	
	//final protected MonitoringController controller;
	final protected TimeSeriesDataImpl tsdi;
	private final Datapoint dp;
	
	/** only relevant if dp == null*/
	final protected TimeSeriesNameProvider nameProvider;
	
	//final protected ProcessedReadOnlyTimeSeries newTs;
	final protected AggregationMode mode;
	
	private Long firstTimestampInSource = null;
	private Long lastTimestampInSource = null;
	private boolean updateLastTimestampInSourceOnEveryCall;
	public Long getTimeStampInSourceInternal(boolean first) {
		if(first)
			return firstTimestampInSource;
		return lastTimestampInSource;
	}

	/*public ProcessedReadOnlyTimeSeries2(TimeSeriesDataImpl tsdi, TimeSeriesNameProvider nameProvider,
			MonitoringController controller) {
		this(tsdi, nameProvider, getMode(controller, tsdi.label(null)));
	}*/
	public ProcessedReadOnlyTimeSeries2(TimeSeriesDataImpl tsdi, TimeSeriesNameProvider nameProvider,
			AggregationMode mode) {
		this(tsdi, nameProvider, mode, null);
	}
	public ProcessedReadOnlyTimeSeries2(TimeSeriesDataImpl tsdi, TimeSeriesNameProvider nameProvider,
			AggregationMode mode, Datapoint dp) {
		this(tsdi, nameProvider, mode, dp, false);
		
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
	public ProcessedReadOnlyTimeSeries2(TimeSeriesDataImpl tsdi, TimeSeriesNameProvider nameProvider,
			AggregationMode mode, Datapoint dp,
			boolean updateLastTimestampInSourceOnEveryCall) {
		super(InterpolationMode.NONE);
		this.nameProvider = nameProvider;
		this.tsdi = tsdi;
		this.dp = dp;
		this.mode = mode;
		this.setUpdateLastTimestampInSourceOnEveryCall(updateLastTimestampInSourceOnEveryCall);
	}

	public ProcessedReadOnlyTimeSeries2(Datapoint dp) {
		this(dp.getTimeSeriesDataImpl(null), null, dp.info().getAggregationMode(), dp);
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
		ReadOnlyTimeSeries ts = tsdi.getTimeSeries();
		if(firstTimestampInSource == null) {
			SampledValue sv = ts.getNextValue(0);
			if(sv != null)
				firstTimestampInSource = sv.getTimestamp();
			else
				return Collections.emptyList();
		}
		if(lastTimestampInSource == null && updateLastTimestampInSourceOnEveryCall) {
			SampledValue sv = ts.getPreviousValue(Long.MAX_VALUE);
			if(sv != null)
				lastTimestampInSource = sv.getTimestamp();
			else if(lastTimestampInSource == null)
				return Collections.emptyList();
		}
		if(end < firstTimestampInSource)
			return Collections.emptyList();
		if(start > lastTimestampInSource)
			return Collections.emptyList();
		if(end > lastTimestampInSource)
			end = lastTimestampInSource;
		if(start < firstTimestampInSource)
			start = firstTimestampInSource;
logger.error("Starting getResultValues for PROT:"+dp.getLocation());
		return getResultValues(ts, start, end, mode);
	}

	public String getShortId() {
		return getShortId(tsdi, nameProvider, getDp());
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
	public DatapointImpl getResultSeriesDP(DatapointService dpService) {
		String label;
		String tsLocationOrBaseId;
		if(getDp() != null) {
			label = getDp().label(null)+getLabelPostfix();
			tsLocationOrBaseId = getDpLocation(getDp(), getLabelPostfix()); //getDp().getLocation()+getLocationPostifx();
		} else {
			label = getShortId()+getLabelPostfix();
			tsLocationOrBaseId = tsdi.id()+getLocationPostifx();
		}
		DatapointImpl result;
		if(dpService == null)
			result = new DatapointImpl(this, tsLocationOrBaseId, label, false);
		else {
			result = (DatapointImpl) dpService.getDataPointStandard(tsLocationOrBaseId);
			result.setTimeSeries(this, false);
			result.setLabel(label, null);
		}
		DPUtil.copyExistingDataRoomDevice(getDp(), result);
		result.info().setAggregationMode(AggregationMode.Consumption2Meter);
		result.info().setInterpolationMode(InterpolationMode.NONE);
		return result ;
	}
	public Datapoint getDp() {
		return dp;
	}
	
	public static String getDpLocation(Datapoint dpSource, String locationPostfix) {
		return dpSource.getLocation()+locationPostfix;
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
