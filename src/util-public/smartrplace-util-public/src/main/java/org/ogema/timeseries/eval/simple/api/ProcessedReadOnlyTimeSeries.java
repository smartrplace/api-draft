package org.ogema.timeseries.eval.simple.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DpUpdateAPI.DpGap;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.devicefinder.util.DatapointImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.tissue.util.logconfig.PerformanceLog;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

/** Implementation for {@link ReadOnlyTimeSeries} that is based on some input provided via {@link #updateValues(long, long)}.
 * The class requests any unknown values from the implementation via this method. In in standard
 * operation it assumes that for a given interval all values in the interval are returned on the first request
 * and that the values in the interval do not change. The class has been extended, though, to take care of changes in input
 * already processed:<br>
 *  * By setting knownEndUpdateInterval intervals that go beyond the current time or that range into
 *    a current aligned interval (like the current day) are reprocessed regularly (see next Section on  on this).<br>
 *  * By providing data in {@link #addIntervalToUpdate(DpGap)} or overwriting {@link #getIntervalsToUpdate(long, long)}
 *    intervals can be provided that need recalculation e.g. because of manual input<br>   
 * Note that if knownEndUpdateInterval is null the limits of values provided via updateValues and the data within intervals that have been
 * requested before are NOT updated later on. If the interval is set then after the interval is finished the 
 * knownEnd is reset to the time of the last update meaning that it is assumed that everything behind this last
 * update is unknown.<br>
 * In practice the caching mechanism can lead to several problems. It is still decisive to be able to process a
 * larger quantity of input data. If you set knownEndUpdateInterval to a very short time (e.g. 10 seconds) and
 * absoluteTiming to AbsoluteTiming.ANY_RANGE then almost no caching should take place anymore (to be tested).<br>
 * <br>
 * There are also several ways to limit the frequency of recalculations to avoid performance issues:<br>
 *   * property org.ogema.timeseries.eval.simple.api.knownIntervalUpdate sets knownEndUpdateInterval to the value (in seconds). Take
 *     effect on all instances of ProcessedReadOnlyTimeseries if not set explicitly otherwise in the constructor (which is not
 *     used at the time of this writing). The default value is 7200 (2 hours, usually this has to be set to a shorter interval)
 *   * property org.ogema.timeseries.eval.simple.api.intervalsToUpdateProcessingInterval sets the limit for processing getIntervalsToUpdate.
 *     Also takes effect on all ProcessedReadOnlyTimeseries. If null all changes are processed immediately (default).
 *   * Setting *minIntervalForReCalc* in the constructor of an instance of TimeseriesSimpleProcUtil, which is put forward into
 *     the instances of ProcessedReadOnlyTimeSeries generated by it. Inside the limit given here the timeseries returns
 *     just the values calculated before and does not check for any updates or recalculations. This is an effective limit in
 *     some online situations when the source data is written very frequently, but can lead to unexpected behavior with
 *     complex interdependencies of timeseries. So this cannot be set for all timseries with a single property.
 *   * Note that the property flag evaldebug0 is especially foreseen to enable performance debugging
 * <br>
 * Debugging:<br>
 * Especially the page with TS-ANY Datapoints is important to see the current status of datapoints for which
 * timeseries are calculated via this evaluation. To track the call stacks of the evaluation you should enable
 * the property "evaldebug", which leads to console outputs with a label usually identifiying the time series
 * that is processed at this point. If the result is written into slotsDB then take care of
 * Debugging issues with Virtual SlotsDB Data.
 * <br>
 * Background with Open Issues:<br>
 * TODO 1: Currently for all data provided via {@link #updateValues(long, long)} is checked whether the result overlaps
 * with existing data and any existing data in the range is deleted if this is the case. This avoids that the internal
 * list of SampledValues ({@link #values}) gets unordered etc. In some cases this may not be necessary and very
 * resource-costly, but in most typical cases this is the intended behavior.<br>
 * TODO 2: The caching management is especially critical for TimeseriesSetProcMultiToSingle
 * that take several time series as input, oftentimes ProcessedReadOnlyTimeSeries themselves. Currently
 * we operate all such time series in updateMode=4, which means that any reported change in the input
 * data will lead to a complete recalculation. In typical online operation this is usually no problem and a
 * reasonable trade-off to manage the caching issues, but in offline evaluation or in larger settings
 * (especially if a lot of high resolution metering data is collected over time) this
 * may need a change. For input time series that do not generate a note on their datapoint when 
 * data changes it is still an issue to make sure that any new data is taken care of (now usually by the
 * absoluteTiming parameter).<br> 
 * TODO 3: If the result is aligned and the full input data is available from the beginning the calculation may be
 * not very efficient. When on aligend result is calculated taking into account all input data until
 * the next aligned interval then knownEnd may be set to
 * some time at the result currently calculated or somewhere shortly behind unaligned, but before the next aligned result that may
 * be requested by the upper level evaluation in the next step. Then the previous aligned interval result is calculated once again
 * together with the new result. If the base input is flowing in via data logging then this behavior may make sense as the last
 * aligned aggregated value is updated all the time until the next aligned interval begins. NOTE that in many cases
 * this is not an issue as the entire time range is usually calculated e.g. when the size method is called. For
 * large offline evaluations this issue has to be considered, though.<br>
 * <br>
 * Background information:<br>
 * Access synchronization: The getValues method should not be processed for the same object of type {@link ProcessedReadOnlyTimeSeries}
 * twice in parallel as this would trigger a double recalculation and setting the known limits would be quite unpredictable. This
 * requires that the implementation of updateValues does not read the time series again via getValues. It could read via
 * {@link #getValuesWithoutUpdate(long, long)}, but this is without update, of course. We have implemented this with
 * a ReentrantLock. For the proc and cons of ReentrantLock see the table in https://www.geeksforgeeks.org/lock-framework-vs-thread-synchronization-in-java/.
 * */
public abstract class ProcessedReadOnlyTimeSeries implements ReadOnlyTimeSeries {
	
	public static PerformanceLog lockLog;
	public static PerformanceLog subTsBuildLog;
	
	protected abstract List<SampledValue> updateValues(long start, long end);
	private List<SampledValue> updateValues2(long start, long end, long now) {
		return updateValues(start, end);
	}
	
	/** Only relevant if updateFinalValue is active (default is every two hours)*/
	protected abstract long getCurrentTime();

	public String dpLabel() {
		String result = getClass().getSimpleName();
		if(!result.isEmpty())
			return result;
		if(datapointForChangeNotification != null)
			return datapointForChangeNotification.getLocation();
		return getClass().getName();
	};
	
	protected List<SampledValue> values = null;
	//For Debugging only!
	public List<SampledValue> getCurrentValues() {
		return values;
	}
	
	protected long knownStart = -1;
	protected long knownEnd;
	protected long firstValueInList = Long.MAX_VALUE;
	protected long lastValueInList = -1;
	private boolean isOwnList = false;

	final Long knownEndUpdateInterval;
	final Long intervalsToUpdateProcessingInterval;
	final Long minIntervalForReCalc;
	long lastKnownEndUpdate = -1;
	long lastIntervalToUpdateProc = -1;
	long lastReCalc = -1;
	
	/** Used for the update when knownEndUpdateInterval is triggered
	 */
	final Integer absoluteTiming;
	public Integer absoluteTiming() {
		return absoluteTiming;
	}
	protected final long creationTime;
	
	protected final InterpolationMode interpolationMode;
	
	final static protected Logger logger = LoggerFactory.getLogger("ProcessedReadOnlyTimeSeries");

	private final Lock updateLock = new ReentrantLock();
	/** Set this to get notifications, only relevant if WriteMode==ANY*/
	public Datapoint datapointForChangeNotification = null;
	
	
	public ProcessedReadOnlyTimeSeries(InterpolationMode interpolationMode) {
		this(interpolationMode, null, null);
	}
	public ProcessedReadOnlyTimeSeries(InterpolationMode interpolationMode, Integer absoluteTiming) {
		this(interpolationMode, absoluteTiming, null);
	}
	public ProcessedReadOnlyTimeSeries(InterpolationMode interpolationMode, Integer absoluteTiming, Long minIntervalForReCalc) {
		this(interpolationMode, 1000*Long.getLong("org.ogema.timeseries.eval.simple.api.knownIntervalUpdate", 7200),
				1000*Long.getLong("org.ogema.timeseries.eval.simple.api.intervalsToUpdateProcessingInterval", 1),
				minIntervalForReCalc, absoluteTiming);
	}
		
	public ProcessedReadOnlyTimeSeries(InterpolationMode interpolationMode,
			Long knownEndUpdateInterval, Long intervalsToUpdateProcessingInterval,
			Integer absoluteTiming) {
		this(interpolationMode, knownEndUpdateInterval, intervalsToUpdateProcessingInterval, null, absoluteTiming);
	}
	/** Constructor
	 * 
	 * @param interpolationMode currently only InterpolationMode.NONE is supported
	 * @param knownEndUpdateInterval if not null this specifys a duration after which the interval for which the time series
	 * 		values are assumed to be known is reset to lastKnownEndUpdate. If absoluteInterval is set then it will
	 * 		even be set back to the beginning of the interval of lastKnownEndUpdate
	 */
	public ProcessedReadOnlyTimeSeries(InterpolationMode interpolationMode,
			Long knownEndUpdateInterval, Long intervalsToUpdateProcessingInterval, Long minIntervalForReCalc,
			Integer absoluteTiming) {
		this.interpolationMode = interpolationMode;
		this.knownEndUpdateInterval = knownEndUpdateInterval;
		this.minIntervalForReCalc = minIntervalForReCalc;
		this.absoluteTiming = absoluteTiming;
		this.intervalsToUpdateProcessingInterval = intervalsToUpdateProcessingInterval;
		this.creationTime = getCurrentTime();
		this.intervalToUpdate.start = -1;
	}

	@Override
	public List<SampledValue> getValues(long startTime, long endTime) {
if(Boolean.getBoolean("evaldebug")) System.out.println("getValues for  "+dpLabel()+" "+TimeProcPrint.getFullTime(startTime)+" : "+TimeProcPrint.getFullTime(endTime));

		if(startTime < 0)
			startTime = 0;
		boolean isFree = updateLock.tryLock();
		if(!isFree) {
			//System.out.println("Waiting for lock for "+dpLabel()+"...");
			long startWait = getCurrentTime();
			updateLock.lock();
			//System.out.println("Acquired lock for "+dpLabel()+" after "+(getCreationTime() - startWait)+" msec.");			
			long endOfAgg =  getCurrentTime();
if(lockLog != null) lockLog.logEvent((endOfAgg-startWait), "Acquired lock for "+dpLabel()+" after");
		}
		final List<DpGap> toUpdate;
		try {
		long now = getCurrentTime();
		if(minIntervalForReCalc != null && ((now - lastReCalc) < minIntervalForReCalc))
			return getValuesWithoutUpdate(startTime, endTime);
		lastReCalc = now;
		if(knownEndUpdateInterval != null) {
			if(now - lastKnownEndUpdate > knownEndUpdateInterval) {
				if(absoluteTiming != null) {
					if(absoluteTiming == AbsoluteTiming.ANY_RANGE) {
						reset(null);
					} else
						knownEnd = AbsoluteTimeHelper.getIntervalStart(lastKnownEndUpdate, absoluteTiming)-1;
				} else
					knownEnd = lastKnownEndUpdate;
				lastKnownEndUpdate = now;
			}
		}
		if((now - lastIntervalToUpdateProc) > intervalsToUpdateProcessingInterval) {
			toUpdate = getIntervalsToUpdate(startTime, endTime);
			if(toUpdate != null) for(DpGap intv: toUpdate) {
				if((knownStart < 0) || (startTime < knownStart && endTime > knownEnd)) {
					continue;
					//values = updateValues(startTime, endTime);
	//if(Boolean.getBoolean("evaldebug")) System.out.println("return-updateVals1:  "+dpLabel()+" "+TimeProcPrint.getSummary(values));
					//isOwnList = false;
				} else {
					//List<SampledValue> prevVals = getValuesWithoutUpdate(intv.start, intv.end);
					List<SampledValue> newVals = updateValues2(intv.start, intv.end, now);
	if(Boolean.getBoolean("evaldebug")) System.out.println("return-updateVals2:  "+dpLabel()+" "+TimeProcPrint.getSummary(newVals));
					addValues(newVals);
				}
				if(intv.start < 0 || intv.end > knownEnd)
					knownEnd = intv.end;
				if(intv.start < 0 || intv.start < knownStart)
					knownStart = intv.start;
			}
			lastIntervalToUpdateProc = now;
		} else
			toUpdate = null;
		if((knownStart < 0) || (startTime < knownStart && endTime > knownEnd)) {
			values = updateValues2(startTime, endTime, now);
if(Boolean.getBoolean("evaldebug")) System.out.println("return-updateVals3:  "+dpLabel()+" "+TimeProcPrint.getSummary(values));
			isOwnList = false;
			knownStart = startTime;
			knownEnd = endTime;
			updateValueLimits();
		} else if(startTime < knownStart) {
			List<SampledValue> newVals = updateValues2(startTime, knownStart, now);
if(Boolean.getBoolean("evaldebug")) System.out.println("return-updateVals4:  "+dpLabel()+" "+TimeProcPrint.getSummary(newVals));
			addValues(newVals);
			knownStart = startTime;	
			//updateValueLimits();
		} else if(endTime > knownEnd) {
//logger.error("Greater endTime PROT1 knownEnd:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(knownEnd)+" endTime:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(endTime));
			List<SampledValue> newVals = updateValues2(knownEnd, endTime, now);
if(Boolean.getBoolean("evaldebug")) System.out.println("return-updateVals5:  "+dpLabel()+" "+TimeProcPrint.getSummary(newVals));
//logger.error("Found new vals:"+values.size());
			addValues(newVals);
			knownEnd = endTime;			
			//updateValueLimits();
		}
		} finally {
			updateLock.unlock();
		}
		List<SampledValue> result = getValuesWithoutUpdate(startTime, endTime);
		if((toUpdate != null) && (!toUpdate.isEmpty()) && (datapointForChangeNotification != null)) {
			DpUpdated updTotal = DatapointImpl.getStartEndForUpdList(toUpdate);
			datapointForChangeNotification.notifyTimeseriesChange(updTotal.start, updTotal.end);
		}
if(Boolean.getBoolean("evaldebug")) System.out.println("returning "+result.size()+" vals for "+dpLabel()+" "+TimeProcPrint.getFullTime(startTime)+" : "+TimeProcPrint.getFullTime(endTime));
		return result;
	}

	protected List<SampledValue> getValuesWithoutUpdate(long startTime, long endTime) {
		if(startTime > lastValueInList)
			return Collections.emptyList();
		if(endTime < firstValueInList)
			return Collections.emptyList();
		if(values == null)
			return Collections.emptyList();
		if(startTime <= firstValueInList && endTime > lastValueInList) {
			return new ArrayList<SampledValue>(values);
		}
		if(values.isEmpty())
			return Collections.emptyList();
long startCalc =  getCurrentTime();
		int fromIndex = -1;
		int toIndex = -1;
		for(int i=0; i<values.size(); i++) {
			if(values.get(i).getTimestamp() >= startTime) {
				fromIndex = i;
				break;
			}
		}
		if(fromIndex < 0)
			throw new IllegalStateException("Should not occur! startTime requested:"+startTime+" lastValueInList:"+lastValueInList);
		if(endTime == startTime)
			toIndex = fromIndex;
		else for(int i=values.size()-1; i>=fromIndex; i--) {
			if(values.get(i).getTimestamp() < endTime) {
				toIndex = i;
				break;
			}
		}
		if(fromIndex > toIndex)
			return Collections.emptyList();
		List<SampledValue> result = values.subList(fromIndex, toIndex+1);
		long endOfAgg =  getCurrentTime();
if(subTsBuildLog != null) subTsBuildLog.logEvent((endOfAgg-startCalc), "Calculation of SUB "+dpLabel()+" took");
		return result;		
	}
	
	/** Remove all existing values between first and last values in newVals and removeLast and add newVals*/
	protected void addValues(List<SampledValue> newVals) {
		if(newVals.isEmpty())
			return;
		if(Boolean.getBoolean("evaldebug")) {
			TimeProcUtil.checkConsistency(newVals, "NEW::"+dpLabel());
		}
		long newFirst = newVals.get(0).getTimestamp();
		long newLast = newVals.get(newVals.size()-1).getTimestamp();
		addValues(newVals, newFirst, newLast);
	}
	/** Remove all existing values between removeFirst and removeLast and add newVals*/
	protected void addValues(List<SampledValue> newVals, long removeFirst, long removeLast) {
		long startCalc =  getCurrentTime();
		List<SampledValue> existing = null;
		List<SampledValue> existingLoc = getValuesWithoutUpdate(removeFirst, removeLast+1);
		if(!existingLoc.isEmpty()) {
if(Boolean.getBoolean("evaldebug")) System.out.println("  Overwriting values for "+dpLabel()+" without registration in getIntervalsToUpdate - now accepted");
			existing = existingLoc;
		}
		if(Boolean.getBoolean("evaldebug") && values != null) {
			TimeProcUtil.checkConsistency(values, "PRE::"+dpLabel());
		}
		if(isOwnList) {
			try {
				if(existing != null)
					values.removeAll(existing);
				//values.addAll(newVals);
				insertNewValues(newVals, removeFirst);
			} catch(UnsupportedOperationException e) {
				//TODO: Should not occur
				List<SampledValue> concat = new ArrayList<SampledValue>(values);
				if(existing != null)
					concat.removeAll(existing);
				values = concat;
				insertNewValues(newVals, removeFirst);
			}
		} else {
			List<SampledValue> concat = (values!=null)?new ArrayList<SampledValue>(values):new ArrayList<>();
			if(existing != null)
				concat.removeAll(existing);
			//concat.addAll(newVals);
			values = concat;
			insertNewValues(newVals, removeFirst);
			isOwnList = true;
		}
		updateValueLimits();
long endCalc =  getCurrentTime();
if(subTsBuildLog != null) subTsBuildLog.logEvent((endCalc-startCalc), "Calculation of ADD "+dpLabel()+" took");
if(Boolean.getBoolean("evaldebug")) {
	TimeProcUtil.checkConsistency(values, "PRE::"+dpLabel());
}
	}
	
	protected void insertNewValues(List<SampledValue> newVals, long newFirst) {
		if(newVals.isEmpty())
			return;
		if(values.isEmpty()) {
			values.addAll(newVals);
			return;
		}
		long lastInList = values.get(values.size()-1).getTimestamp();
		int idx = -999;
		if(newFirst < lastInList) {
			//we have to insert at the right position
			idx = values.size()-2;
			while(idx >= 0) {
				if(newFirst > values.get(idx).getTimestamp()) {
					break;
				}
				idx--;
			}
			List<SampledValue> concat;
			if(idx >= 0)
				concat = new ArrayList<>(values.subList(0, idx+1));
			else
				concat = new ArrayList<>();
			concat.addAll(newVals);
			if(idx >= 0)
				concat.addAll(values.subList(idx+1, values.size()));
			else
				concat.addAll(values);
			values = concat;
		} else
			values.addAll(newVals);
		if(Boolean.getBoolean("evaldebug")) {
			TimeProcUtil.checkConsistency(values, "POST::"+dpLabel());
		}
	}
	
	protected void updateValueLimits() {
		if(!values.isEmpty()) {
			firstValueInList = values.get(0).getTimestamp();
			lastValueInList = values.get(values.size()-1).getTimestamp();
		}
	}
	
	@Override
	public List<SampledValue> getValues(long startTime) {
		return getValues(startTime, Long.MAX_VALUE);
	}

	@Override
	public SampledValue getValue(long time) {
		if(interpolationMode != InterpolationMode.NONE)
			throw new UnsupportedOperationException("Interpolation only for NONE supported yet!");
		List<SampledValue> asList = getValues(time, time);
		if(asList.isEmpty())
			return null;
		else
			return asList.get(0);
	}

	@Override  //TODO: We need a more efficient implementation here
	public SampledValue getNextValue(long time) {
		if(time < 0)
			time = 0;
		List<SampledValue> asList = getValues(time, Long.MAX_VALUE);
		if(asList.isEmpty())
			return null;
		else
			return asList.get(0);
	}

	@Override
	public SampledValue getPreviousValue(long time) {
		if(time < 0)
			time = 0;
		List<SampledValue> asList = getValues(0, time);
		if(asList.isEmpty())
			return null;
		else {
			return asList.get(asList.size()-1);
		}
	}

	@Override
	public InterpolationMode getInterpolationMode() {
		return interpolationMode;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public boolean isEmpty(long startTime, long endTime) {
		return getValues(startTime, endTime).isEmpty();
	}

	@Override
	public int size() {
		return getValues(0, Long.MAX_VALUE).size();
	}

	@Override
	public int size(long startTime, long endTime) {
		return getValues(startTime, endTime).size();
	}

	@Override
	public Iterator<SampledValue> iterator() {
		return getValues(0, Long.MAX_VALUE).listIterator();
	}

	@Override
	public Iterator<SampledValue> iterator(long startTime, long endTime) {
		return getValues(startTime, endTime).listIterator();
	}

	@Override
	public Long getTimeOfLatestEntry() {
		return getPreviousValue(Long.MAX_VALUE).getTimestamp();
	}
	
	public List<SampledValue> getValuesInternal() {
		return values;
	}
	
	public long getCreationTime() {
		return creationTime;
	}
	
	
	public static String getSummaryHeader() {
		return "knownStart/End/lastUpd/interval";
	}
	
	public String getSummaryColumn() {
		String result = StringFormatHelper.getTimeDateInLocalTimeZone(knownStart)+"/"+StringFormatHelper.getTimeDateInLocalTimeZone(knownEnd)+
				"/"+StringFormatHelper.getTimeDateInLocalTimeZone(lastKnownEndUpdate);
		if(knownEndUpdateInterval != null)
			result += "/"+knownEndUpdateInterval/60000+"min";
		return result;
	}
	
	/** Set back knownEnd. This should be used if the values shall be re-calculated from a certain time.<br>
	 * TODO: This method is NOT Thread-safe
	 * 
	 * @param newKnownEnd
	 * @param force if true it is possible to set knownEnd to a later time. This means that for the increased intervals
	 * 		no calculations will be performed, which usually is not intended
	 * @return true if knownEnd was set to newKnownEnd
	 */
	public boolean resetKnownEnd(long newKnownEnd, boolean force) {
		if(knownEnd < newKnownEnd && (!force))
			return false;
		knownEnd = newKnownEnd;
		return true;
	}

	/** Delete all values and recalculate everything
	 * 
	 * @param reCalcUntil if not null a recalculation from zero until this time will be triggered immediately, otherwise on the
	 * 		next call to getValues
	 * @return
	 */
	public void reset(Long reCalcUntil) {
		knownStart = -1;
		knownEnd = 0;
		values = null;
		if(reCalcUntil != null) {
			getValues(0, reCalcUntil);
		}
	}
	
	private final DpGap intervalToUpdate = new DpGap();
	
	/** TODO: In the future separate updateIntervals should be stored and returned in
	 * {@link #getIntervalsToUpdate(long)}
	 * @param newInterval
	 * @return
	 */
	public DpGap addIntervalToUpdate(DpGap newInterval) {
		synchronized (intervalToUpdate) {
			if(intervalToUpdate.start < 0) {
				intervalToUpdate.start = newInterval.start;
				intervalToUpdate.end = newInterval.end;
			} else {
				if(newInterval.start < intervalToUpdate.start)
					intervalToUpdate.start = newInterval.start;
				if(newInterval.end > intervalToUpdate.end)
					intervalToUpdate.end = newInterval.end;
			}
			return intervalToUpdate;
		}
	}
	
	/** Get all gaps to be filled since last call of this method. The intervals should not be overlapping
	 * as we do not test on this here - this would lead to a double re-calculation.<br>
	 * NOTE: All values that shall be replaced MUST be overwritten using this method. Do not
	 * try to overwrite a value just providing the same time stamp again via {@link #updateValues(long, long)}
	 * as this will just add two entries for the same time stamp or lead to badly ordered time stamps.*/
	protected List<DpGap> getIntervalsToUpdate(long startTime, long endTime) {
		synchronized (intervalToUpdate) {
			if(intervalToUpdate.start < 0)
				return Collections.emptyList();
			DpGap inResult = new DpGap();
			inResult.start = intervalToUpdate.start;
			inResult.end = intervalToUpdate.end;
			List<DpGap> result = Arrays.asList(new DpGap[] {inResult});
			intervalToUpdate.start = -1;
			return result;			
		}
	}
}
