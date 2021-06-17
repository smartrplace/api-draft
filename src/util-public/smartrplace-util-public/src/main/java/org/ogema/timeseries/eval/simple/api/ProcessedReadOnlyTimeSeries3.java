package org.ogema.timeseries.eval.simple.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.devicefinder.util.DPUtil;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSetProcMultiToSingle;

public abstract class ProcessedReadOnlyTimeSeries3 extends ProcessedReadOnlyTimeSeries {
	/** This method is called to obtain the calculated values. The method may be called again for the same
	 * time stamps. This version is used for input type NoneOrImplicit and SINGLE
	 * @param timeSeries
	 * @param start
	 * @param end
	 * @param mode
	 * @return
	 */
	protected abstract List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start, long end,
			AggregationMode mode);
	
	/** This version is used for input type MULTI
	 * 
	 * @param timeSeries
	 * @param start
	 * @param end
	 * @param mode
	 * @return
	 */
	protected abstract List<SampledValue> getResultValuesMulti(List<ReadOnlyTimeSeries> timeSeries, long start, long end,
			AggregationMode mode);
	
	/** Implement one of these three OR use constructor that sets input type SINGLE directly*/
	protected Datapoint getInputDp() {return dpInSingle;}
	protected List<Datapoint> getInputDps() {return null;}
	protected String getResultLocation() {return null;}
	
	/** Override usually **/
	protected String getLabelPostfix() {return "";}

	/** May override*/
	/** change startTime and endTime of parameter if necessary*/
	protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {};
	
	/** Override for NONE and MULTI*/
	protected String resultLabel() {
		if(getInputType() == InputType.SINGLE) {
			return getInputDp().label(null)+getLabelPostfix();
		} else {
			throw new IllegalStateException("For NONE and MULTI resultLabel() needs to be overriden!");
		}
	}
	
	/** only relevant if dp == null*/
	final protected TimeSeriesNameProvider nameProvider;
	
	//final protected ProcessedReadOnlyTimeSeries newTs;
	final protected AggregationMode mode;

	/** For {@link ProcessedReadOnlyTimeSeries2} this is always true*/
	private boolean updateLastTimestampInSourceOnEveryCall;

	protected long lastUpdateTime = -1;
	public long getLastUdpateTime() {
		return lastUpdateTime;
	}
	
	public enum InputType {
		NoneOrImplicit,
		SINGLE,
		MULTI
	}
	private InputType inputType = null;
	private Datapoint dpInSingle = null;
	private List<Datapoint >dpsInMulti = null;
	protected final InputType getInputType() {
		if(inputType == null) {
			Datapoint dp = getInputDp();
			if(dp != null) {
				inputType = InputType.SINGLE;
				dpInSingle = dp;
			} else {
				List<Datapoint> dps = getInputDps();
				if(dps == null || dps.isEmpty()) {
					inputType = InputType.NoneOrImplicit;
				} else {
					inputType = InputType.MULTI;
					dpsInMulti = dps;
				}
			}
		}
		return inputType;
	}
	protected final Datapoint getSingleInputDp() {
		if(inputType == null)
			getInputType();
		return dpInSingle;
	}
	protected final List<Datapoint> getMultiInputDp() {
		if(inputType == null)
			getInputType();
		return dpsInMulti;
	}
	
	public ProcessedReadOnlyTimeSeries3(TimeSeriesNameProvider nameProvider,
			AggregationMode mode) {
		this(nameProvider, mode, !Boolean.getBoolean("org.ogema.timeseries.eval.simple.api.noUpdateLastTimestampInSource"));
		
	}
	public ProcessedReadOnlyTimeSeries3(TimeSeriesNameProvider nameProvider,
			AggregationMode mode,
			boolean updateLastTimestampInSourceOnEveryCall) {
		this(nameProvider, mode, updateLastTimestampInSourceOnEveryCall, null);
	}

	public ProcessedReadOnlyTimeSeries3(TimeSeriesNameProvider nameProvider,
			AggregationMode mode,
			boolean updateLastTimestampInSourceOnEveryCall,
			Integer absoluteTiming) {
		super(InterpolationMode.NONE,
				null, 1000*Long.getLong("org.ogema.timeseries.eval.simple.api.intervalsToUpdateProcessingInterval", 1),
				Long.MAX_VALUE,
				absoluteTiming);
		this.nameProvider = nameProvider;
		this.mode = mode;
		this.updateLastTimestampInSourceOnEveryCall = updateLastTimestampInSourceOnEveryCall;
		knownStart = 0;
		knownEnd = Long.MAX_VALUE;
		lastReCalc = getCurrentTime();
	}
	public ProcessedReadOnlyTimeSeries3(Datapoint dpInput) {
		this(null, dpInput.info().getAggregationMode());
		inputType = InputType.SINGLE;
		dpInSingle = dpInput;
	}
	public ProcessedReadOnlyTimeSeries3(Datapoint dpInput, Integer absoluteTiming, Long minIntervalForReCalc) {
		this(null, dpInput.info().getAggregationMode(),
				!Boolean.getBoolean("org.ogema.timeseries.eval.simple.api.noUpdateLastTimestampInSource"), absoluteTiming);
		inputType = InputType.SINGLE;
		dpInSingle = dpInput;
	}
	
	
	
	@Override
	protected List<SampledValue> updateValues(long start, long end) {
		return Collections.emptyList();
	}

	protected String getDpLocation() {
		Datapoint dpIn = getInputDp();
		if(dpIn != null)
			return ProcessedReadOnlyTimeSeries2.getDpLocation(dpIn, getLabelPostfix());
		List<Datapoint> allInp = getInputDps();
		if(allInp == null || allInp.isEmpty())
			return ProcessedReadOnlyTimeSeries2.getDpLocation(getResultLocation(), getLabelPostfix());
		String baseLoc = TimeseriesSetProcMultiToSingle.getResultDpLocation(
				allInp, this.getClass().getName());
		return ProcessedReadOnlyTimeSeries2.getDpLocation(baseLoc, getLabelPostfix());
	}
	
	/** Call this to update or add values that are stored internally
	 * @return sampled values calculated (usually should not be neeeded to be used)*/
	public List<SampledValue> updateValuesStored(long start, long end, boolean force) {
		if(force)
			return updateValuesStoredForced(start, end);
		//TODO check input, time since last update
		//Initially we should not need this if we call a reasonable timer
		return updateValuesStoredForced(start, end);
	}
	/** Call this to update or add values that are stored internally
	 * @return sampled values calculated (usually should not be neeeded to be used)*/
	public List<SampledValue> updateValuesStoredForced(long start, long end) {
		List<SampledValue> newVals;
		switch(getInputType()) {
		case NoneOrImplicit:
			newVals = getResultValues(null, start, end, mode);
			break;
		case SINGLE:
			ReadOnlyTimeSeries ts = getSingleInputDp().getTimeSeries();
			newVals = getResultValues(ts, start, end, mode);
			break;
		case MULTI:
			List<Datapoint> dps = getMultiInputDp();
			List<ReadOnlyTimeSeries> tss = new ArrayList<>();
			for(Datapoint dp: dps) {
				tss.add(dp.getTimeSeries());
			}
			newVals = getResultValuesMulti(tss, start, end, mode);
			break;
		default:
			throw new IllegalStateException("Unknonw input type: "+getInputType());
		}
		lastUpdateTime = lastReCalc = getCurrentTime();
		addValues(newVals);
		return newVals;
	}
	
	public DatapointImpl getResultSeriesDP(DatapointService dpService, String location) {
		DatapointImpl result;
		if(location != null) {
			result = (DatapointImpl) dpService.getDataPointStandard(location);
		} else {
			String tsLocationOrBaseId;
			tsLocationOrBaseId = getDpLocation();
			if(dpService == null)
				throw new IllegalStateException("Operation without DatapointService not supported anymore(2)!");
				//result = new DatapointImpl(this, tsLocationOrBaseId, label, false);
			else {
				result = (DatapointImpl) dpService.getDataPointStandard(tsLocationOrBaseId);
			}
		}
		String label = resultLabel();
		result.setLabel(label, null);
		result.setTimeSeries(this, false);
		DPUtil.copyExistingDataRoomDevice(getInputDp(), result);
		result.info().setAggregationMode(AggregationMode.Consumption2Meter);
		result.info().setInterpolationMode(InterpolationMode.NONE);
		datapointForChangeNotification = result;
		return result ;
	}

}
