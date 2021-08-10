package org.ogema.timeseries.eval.simple.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.devicefinder.util.DPUtil;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSetProcMultiToSingle;
import org.ogema.timeseries.eval.simple.mon3.ProcTs3PersistentData;
import org.ogema.timeseries.eval.simple.mon3.SampledValueSimple;

import de.iwes.util.timer.AbsoluteTimeHelper;

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
	
	public abstract Long getFirstTimeStampInSource();

	/**Overwrite this to load data into the timeseries initially, e.g. by reading
	 * from a file
	 */
	public void loadInitData() {}
	
	/** Implement one of these three OR use constructor that sets input type SINGLE directly*/
	public Datapoint getInputDp() {return dpInSingle;}
	protected List<Datapoint> getInputDps() {return null;}
	protected String getResultLocation() {return null;}
	
	/** Override usually **/
	protected String getLabelPostfix() {return "";}

	/** May override*/
	/** change startTime and endTime of parameter if necessary*/
	protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {};
	
	/** Override for NONE and MULTI*/
	public String resultLabel() {
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

	/*protected long lastUpdateTime = -1;
	public long getLastUdpateTime() {
		return lastUpdateTime;
	}*/
	protected long lastEndTime = -1;
	public long getLastEndTime() {
		return lastEndTime;
	}

	public enum InputType {
		NoneOrImplicit,
		SINGLE,
		MULTI
	}
	private InputType inputType = null;
	private Datapoint dpInSingle = null;
	private List<Datapoint> dpsInMulti = null;
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
		this(dpInput, null);
	}
	public ProcessedReadOnlyTimeSeries3(Datapoint dpInput, Map<String, Datapoint> dependentTimeseries) {
		this(null, dpInput.info().getAggregationMode());
		inputType = InputType.SINGLE;
		dpInSingle = dpInput;
		this.dependentTimeseries = dependentTimeseries;
	}
	public ProcessedReadOnlyTimeSeries3(Datapoint dpInput, Integer absoluteTiming, Long minIntervalForReCalc) {
		this(dpInput, absoluteTiming, minIntervalForReCalc, null);
	}
	public ProcessedReadOnlyTimeSeries3(Datapoint dpInput, Integer absoluteTiming, Long minIntervalForReCalc,
			Map<String, Datapoint> dependentTimeseries) {
		this(null, dpInput.info().getAggregationMode(),
				!Boolean.getBoolean("org.ogema.timeseries.eval.simple.api.noUpdateLastTimestampInSource"), absoluteTiming);
		inputType = InputType.SINGLE;
		dpInSingle = dpInput;
		this.dependentTimeseries = dependentTimeseries;
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
	
	protected long lastIntervalCalculated = -1;
	public List<SampledValue> updateValuesStoredAligned(long start, long end, boolean force) {
		if(absoluteTiming != null) {
			long startItv = AbsoluteTimeHelper.getIntervalStart(start, absoluteTiming);
			if(lastIntervalCalculated < 0)
				//at the beginning we just calculate from the current interval
				lastIntervalCalculated = startItv;
			if(startItv == lastIntervalCalculated)
				return updateValuesStored(startItv, end, force);
			else {
				//new interval
				List<SampledValue> result = updateValuesStored(lastIntervalCalculated, end, force);
				lastIntervalCalculated = startItv;
				return result;
			}
		}
		return updateValuesStored(start, end, force);
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
		lastReCalc = getCurrentTime();
		lastEndTime = end;
		if(newVals != null)
			addValues(newVals);
		else
			System.out.println("Warning: newVals null!");
		return newVals;
	}
	
	public List<SampledValue> updateValuesStoredForcedForDependentTimeseries(long end, List<SampledValue> newVals) {
		lastReCalc = getCurrentTime();
		lastEndTime = end;
		if(newVals != null)
			addValues(newVals);
		else
			System.out.println("Warning: newVals null!");
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
		if(inputType == InputType.SINGLE)
			DPUtil.copyExistingDataRoomDevice(getInputDp(), result);
		result.info().setAggregationMode(AggregationMode.Consumption2Meter);
		result.info().setInterpolationMode(InterpolationMode.NONE);
		datapointForChangeNotification = result;
		return result ;
	}
	/*public static void copyExistingDataRoomDevice(DPRoom room, Datapoint dest) {
		//if(source.getDeviceResource() != null)
		//	dest.setDeviceResource(source.getDeviceResource());
		if(room != null)
			dest.setRoom(room);
	}*/

	/** Dependent timeseries are added by the {@link ProcessedReadOnlyTimeSeries3} itself. Get the timeseries via a
	 * known ID that is defined for each type by the implementation of the {@link ProcessedReadOnlyTimeSeries3}.
	 */
	protected Map<String, Datapoint> dependentTimeseries;
	public Datapoint getDependentTimeseries(String id) {
		return dependentTimeseries.get(id);
	};
	public Collection<Datapoint> getAllDependentTimeseries() {
		return dependentTimeseries.values();
	}

	public void initValuesFromFile(ProcTs3PersistentData fromFile) {
		values = new ArrayList<>();
		for(SampledValueSimple simple: fromFile.values) {
			values.add(new SampledValue(new FloatValue(simple.v), simple.t, Quality.GOOD));
		}
		lastEndTime = fromFile.lastEndTime;
		knownStart = fromFile.knownStart;
		knownEnd = fromFile.knownEnd;
		firstValueInList = fromFile.firstValueInList;
		lastValueInList = fromFile.lastValueInList;		
	}
	
	public ProcTs3PersistentData getPersistentData() {
		if(values == null)
			return null;
		ProcTs3PersistentData result = new ProcTs3PersistentData();
		int size = values.size();
		result.values = new SampledValueSimple[size];
		for(int i=0; i<size; i++) {
			SampledValue sv = values.get(i);
			result.values[i] = new SampledValueSimple();
			result.values[i].t = sv.getTimestamp();
			result.values[i].v = sv.getValue().getFloatValue();
		}
		result.lastEndTime = lastEndTime;
		result.knownStart = knownStart;
		result.knownEnd = knownEnd;
		result.firstValueInList = firstValueInList;
		result.lastValueInList = lastValueInList;
		result.dpLocation = datapointForChangeNotification.getLocation();
		return result;
	}
}
