package org.ogema.timeseries.eval.simple.api;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.ogema.devicefinder.api.DpUpdateAPI.DpGap;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.ogema.devicefinder.util.DPUtil;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSetProcMultiToSingle;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

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
	
	public TimedJobMemoryData timedJob = null;
	public TimeseriesSetProcessor3 proc = null;
	public TimeseriesUpdateListener listener = null;
	@Override
	protected void addValues(List<SampledValue> newVals) {
		super.addValues(newVals);
		if(listener != null)
			listener.updated(newVals);
	}
	@Override
	protected void addValues(List<SampledValue> newVals, long removeFirst, long removeLast) {
		super.addValues(newVals, removeFirst, removeLast);
		if(listener != null)
			listener.updated(newVals);
	}
	
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
	//private boolean updateLastTimestampInSourceOnEveryCall;

	/*protected long lastUpdateTime = -1;
	public long getLastUdpateTime() {
		return lastUpdateTime;
	}*/
	protected long lastEndTime = -1;
	public long getLastEndTime() {
		return lastEndTime;
	}
	public void initLastEndTime(long lastEndTime) {
		this.lastEndTime = lastEndTime;
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
			AggregationMode mode, Integer absoluteTiming) {
		this(nameProvider, mode, !Boolean.getBoolean("org.ogema.timeseries.eval.simple.api.noUpdateLastTimestampInSource"), absoluteTiming);
		
	}
	/*public ProcessedReadOnlyTimeSeries3(TimeSeriesNameProvider nameProvider,
			AggregationMode mode,
			boolean updateLastTimestampInSourceOnEveryCall) {
		this(nameProvider, mode, updateLastTimestampInSourceOnEveryCall, null);
	}*/

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
		//this.updateLastTimestampInSourceOnEveryCall = updateLastTimestampInSourceOnEveryCall;
		knownStart = 0;
		knownEnd = Long.MAX_VALUE;
		lastReCalc = getCurrentTime();
	}
	public ProcessedReadOnlyTimeSeries3(Datapoint dpInput) {
		this(dpInput, null, null);
	}
	public ProcessedReadOnlyTimeSeries3(Datapoint dpInput, Map<String, Datapoint> dependentTimeseries, Integer absoluteTiming) {
		this(null, dpInput.info().getAggregationMode(), absoluteTiming);
		inputType = InputType.SINGLE;
		dpInSingle = dpInput;
		this.dependentTimeseries = dependentTimeseries;
	}
	/*public ProcessedReadOnlyTimeSeries3(Datapoint dpInput, Integer absoluteTiming, Long minIntervalForReCalc) {
		this(dpInput, absoluteTiming, minIntervalForReCalc, null);
	}
	public ProcessedReadOnlyTimeSeries3(Datapoint dpInput, Integer absoluteTiming, Long minIntervalForReCalc,
			Map<String, Datapoint> dependentTimeseries) {
		this(null, dpInput.info().getAggregationMode(),
				!Boolean.getBoolean("org.ogema.timeseries.eval.simple.api.noUpdateLastTimestampInSource"), absoluteTiming);
		inputType = InputType.SINGLE;
		dpInSingle = dpInput;
		this.dependentTimeseries = dependentTimeseries;
	}*/
	
	
	
	@Override
	protected List<SampledValue> updateValues(long start, long end) {
		return Collections.emptyList();
	}

	@Override
	public List<SampledValue> getValues(long startTime, long endTime) {
		List<SampledValue> result = getValuesWithoutUpdate(startTime, endTime);
		List<DpGap> toUpdate = getIntervalsToUpdate(startTime, endTime);
		if((toUpdate != null) && (!toUpdate.isEmpty()) && (datapointForChangeNotification != null)) {
			DpUpdated updTotal = DatapointImpl.getStartEndForUpdList(toUpdate);
			datapointForChangeNotification.notifyTimeseriesChange(updTotal.start, updTotal.end);
		}
if(Boolean.getBoolean("evaldebug")) System.out.println("returning "+result.size()+" vals for "+dpLabel()+" "+TimeProcPrint.getFullTime(startTime)+" : "+TimeProcPrint.getFullTime(endTime));
		return result;
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
				return updateValuesStored(startItv, end, force).newVals;
			else {
				//new interval
				UpdateValuesStoredResult result = updateValuesStored(lastIntervalCalculated, end, force);
				boolean allInputIsNewOrFarBehind = true;
				if(result.lastInputTimestamp != null) for(Long last: result.lastInputTimestamp) {
					if((last <= startItv) && (last >= lastIntervalCalculated)) {
						allInputIsNewOrFarBehind = false;
						break;
					}
				}
				if(allInputIsNewOrFarBehind)
					lastIntervalCalculated = startItv;
				/*if((!result.newVals.isEmpty())) {
					SampledValue lastSv = result.get(result.size()-1);
					long lastTs = lastSv.getTimestamp();
					if(lastTs >= startItv && !Float.isNaN(lastSv.getValue().getFloatValue()))
						lastIntervalCalculated = startItv;
				}*/
				return result.newVals;
			}
		}
		return updateValuesStored(start, end, force).newVals;
	}
	
	public static class UpdateValuesStoredResult {
		List<SampledValue> newVals;
		/** list of valid last input timestamps found*/
		List<Long> lastInputTimestamp;
	}
	
	/** Call this to update or add values that are stored internally
	 * @return sampled values calculated (usually should not be neeeded to be used)*/
	public UpdateValuesStoredResult updateValuesStored(long start, long end, boolean force) {
		if(force)
			return updateValuesStoredForced(start, end);
		//TODO check input, time since last update
		//Initially we should not need this if we call a reasonable timer
		return updateValuesStoredForced(start, end);
	}
	/** Call this to update or add values that are stored internally
	 * @return sampled values calculated (usually should not be neeeded to be used)*/
	public UpdateValuesStoredResult updateValuesStoredForced(long start, long end) {
		//List<SampledValue> newVals;
		UpdateValuesStoredResult result = new UpdateValuesStoredResult();
		switch(getInputType()) {
		case NoneOrImplicit:
			result.newVals = getResultValues(null, start, end, mode);
			break;
		case SINGLE:
			ReadOnlyTimeSeries ts = getSingleInputDp().getTimeSeries();
			result.newVals = getResultValues(ts, start, end, mode);
			if(ts != null) {
				SampledValue svLast = ts.getPreviousValue(Long.MAX_VALUE);
				if(svLast != null)
					result.lastInputTimestamp = Arrays.asList(new Long[] {svLast.getTimestamp()});
				else
					result.lastInputTimestamp = Arrays.asList(new Long[] {-1l});
			}
			break;
		case MULTI:
			List<Datapoint> dps = getMultiInputDp();
			List<ReadOnlyTimeSeries> tss = new ArrayList<>();
			result.lastInputTimestamp = new ArrayList<>();
			if(dps != null) for(Datapoint dp: dps) {
				ts = dp.getTimeSeries();
				tss.add(ts);
				SampledValue svLast = ts.getPreviousValue(Long.MAX_VALUE);
				if(svLast != null) {
					result.lastInputTimestamp.add(svLast.getTimestamp());
				}
			}
			result.newVals = getResultValuesMulti(tss, start, end, mode);
			break;
		default:
			throw new IllegalStateException("Unknonw input type: "+getInputType());
		}
		lastReCalc = getCurrentTime();
		lastEndTime = end;
		if(result.newVals != null)
			addValues(result.newVals, start, end);
		else
			System.out.println("Warning: newVals null!");
		return result;
	}
	
	public List<SampledValue> updateValuesStoredForcedForDependentTimeseries(long start, long end, List<SampledValue> newVals) {
		lastReCalc = getCurrentTime();
		lastEndTime = end;
		if(newVals != null)
			addValues(newVals, start, end);
		else
			System.out.println("Warning: newVals null!");
		return newVals;		
	}

	public DatapointImpl getResultSeriesDP(DatapointService dpService, String location) {
		if(datapointForChangeNotification != null)
			return (DatapointImpl) datapointForChangeNotification;
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
		if(fromFile.values.length > 0) {
			values = new ArrayList<>();
			for(SampledValueSimple simple: fromFile.values) {
				values.add(new SampledValue(new FloatValue(simple.v), simple.t, Quality.GOOD));
			}
		}
		lastEndTime = fromFile.lastEndTime;
		knownStart = fromFile.knownStart;
		knownEnd = fromFile.knownEnd;
		firstValueInList = fromFile.firstValueInList;
		lastValueInList = fromFile.lastValueInList;		
	}
	
	public ProcTs3PersistentData getPersistentData() {
		ProcTs3PersistentData result = new ProcTs3PersistentData();
		if(values == null) {
			result.values = new SampledValueSimple[0];
		} else {
			int size = values.size();
			result.values = new SampledValueSimple[size];
			for(int i=0; i<size; i++) {
				SampledValue sv = values.get(i);
				result.values[i] = new SampledValueSimple();
				result.values[i].t = sv.getTimestamp();
				result.values[i].v = sv.getValue().getFloatValue();
			}
		}
		result.lastEndTime = lastEndTime;
		result.knownStart = knownStart;
		result.knownEnd = knownEnd;
		result.firstValueInList = firstValueInList;
		result.lastValueInList = lastValueInList;
		result.dpLocation = datapointForChangeNotification.getLocation();
		return result;
	}
	
	/** Here we allow to add values also from external calculations. In this case the automated recalculation should
	 * be disabled (minIntervalForReCalc with very long interval).*/
	public void addValuesPublic(List<SampledValue> newVals) {
		addValues(newVals);
	}
	
	@Override
	public String getSummaryColumn() {
		String result = StringFormatHelper.getTimeDateInLocalTimeZone(knownStart)+"/"+
				(knownEnd != Long.MAX_VALUE ? StringFormatHelper.getTimeDateInLocalTimeZone(knownEnd) : " MAX ")+
				"/"+StringFormatHelper.getTimeDateInLocalTimeZone(lastEndTime);
		if(proc != null && proc.getAbsoluteTiming() != null) {
			String name = AbsoluteTiming.INTERVAL_NAME_MAP.get(""+proc.getAbsoluteTiming());
			if(name != null)
				result += "/"+name;
			else
				result += "/ (?"+proc.getAbsoluteTiming()+"?)";
			result += "/"+StringFormatHelper.getFormattedValue(proc.getMinIntervalForReCalc());
		} return result;
	}

}
