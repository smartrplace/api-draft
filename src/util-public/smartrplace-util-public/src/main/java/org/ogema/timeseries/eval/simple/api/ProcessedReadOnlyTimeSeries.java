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

import de.iwes.util.format.StringFormatHelper;

/** Implementation for {@link ReadOnlyTimeSeries} that is based on some input provided via {@link #updateValues(long, long)}.
 * The class requests any unknown values from the implementation via this method. It assumes that for a given interval all values
 * in the interval are returned on the first request and that the values in the interval do not change. An exception are intervals
 * that go beyond the current time (see next Section on knownEndUpdateInterval on this).<br>
 * Note that if knownEndUpdateInterval is null the limits of values provided via updateValues and the data within intervals that have been
 * requested before are NOT updated later on. If the interval is set then after the interval is finished the 
 * knownEnd is reset to the time of the last update meaning that it is assumed that everything behind this last
 * update is unknown.<br>
 * TODO: If the result is aligned and the full input data is available from the beginning the calculation may be not very efficient. When one
 * aligend result is calculated taking into account all input data until the next aligned interval then knownEnd may be set to
 * some time at the result currently calculated or somewhere shortly behind unaligned, but before the next aligned result that may
 * be requested by the upper level evaluation in the next step. Then the previous aligned interval result is calculated once again
 * together with the new result. If the base input is flowing in via data logging then this behavior may make sense as the last
 * aligned aggregated value is updated all the time until the next aligned interval begins.<br>
 * Access synchronization: The getValues method should not be processed for the same object of type {@link ProcessedReadOnlyTimeSeries}
 * twice in parallel as this would trigger a double recalculation and setting the known limits would be quite unpredictable. This
 * requires that the implementation of updateValues does not read the time series again via getValues. It could read via
 * {@link #getValuesWithoutUpdate(long, long)}, but this is without update, of course. We have implemented this with
 * a ReentrantLock. For the proc and cons of ReentrantLock see the table in https://www.geeksforgeeks.org/lock-framework-vs-thread-synchronization-in-java/.
 * */
public abstract class ProcessedReadOnlyTimeSeries implements ReadOnlyTimeSeries {
	
	protected abstract List<SampledValue> updateValues(long start, long end);
	
	/** Only relevant if updateFinalValue is active (default is every two hours)*/
	protected abstract long getCurrentTime();

	private List<SampledValue> values = null;
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
	long lastKnownEndUpdate = -1;
	
	protected final long creationTime;
	
	protected final InterpolationMode interpolationMode;
	
	final static protected Logger logger = LoggerFactory.getLogger("ProcessedReadOnlyTimeSeries");

	public ProcessedReadOnlyTimeSeries(InterpolationMode interpolationMode) {
		this(interpolationMode, TimeProcUtil.HOUR_MILLIS*2);
	}
	
	private final Lock updateLock = new ReentrantLock();
	/** Set this to get notifications, only relevant if WriteMode==ANY*/
	public Datapoint datapointForChangeNotification = null;
	
	
	/** Constructor
	 * 
	 * @param interpolationMode currently only InterpolationMode.NONE is supported
	 * @param knownEndUpdateInterval if not null this specifys a duration after which the interval for which the time series
	 * 		values are assumed to be known is reset to the current time
	 */
	public ProcessedReadOnlyTimeSeries(InterpolationMode interpolationMode, Long knownEndUpdateInterval) {
		this.interpolationMode = interpolationMode;
		this.knownEndUpdateInterval = knownEndUpdateInterval;
		this.creationTime = getCurrentTime();
		this.intervalToUpdate.start = -1;
	}

	@Override
	public List<SampledValue> getValues(long startTime, long endTime) {
		if(startTime < 0)
			startTime = 0;
		boolean isFree = updateLock.tryLock();
		if(!isFree) {
			System.out.println("Waiting for lock in "+this.toString()+"...");
			updateLock.lock();
		}
		try {
		if(knownEndUpdateInterval != null) {
			long now = getCurrentTime();
			if(now - lastKnownEndUpdate > knownEndUpdateInterval) {
				knownEnd = lastKnownEndUpdate;
				lastKnownEndUpdate = now;
			}
		}
		List<DpGap> toUpdate = getIntervalsToUpdate(startTime, endTime);
		if(toUpdate != null) for(DpGap intv: toUpdate) {
			if((knownStart < 0) || (startTime < knownStart && endTime > knownEnd)) {
				values = updateValues(startTime, endTime);
				isOwnList = false;
			} else {
				List<SampledValue> prevVals = getValuesWithoutUpdate(intv.start, intv.end);
				List<SampledValue> newVals = updateValues(intv.start, intv.end);
				if(!isOwnList) {
					List<SampledValue> concat = new ArrayList<SampledValue>(values);
					concat.removeAll(prevVals);
					concat.addAll(newVals);
					values = concat;
					isOwnList = true;					
				} else {
					values.removeAll(prevVals);
					values.addAll(newVals);
				}
				//addValues(newVals);
			}
			if(intv.start < 0 || intv.end > knownEnd)
				knownEnd = intv.end;
			if(intv.start < 0 || intv.start < knownStart)
				knownStart = intv.start;
		}
		if((toUpdate != null) && (!toUpdate.isEmpty()) && (datapointForChangeNotification != null)) {
			DpUpdated updTotal = DatapointImpl.getStartEndForUpdList(toUpdate);
			datapointForChangeNotification.notifyTimeseriesChange(updTotal.start, updTotal.end);
		}
		if((knownStart < 0) || (startTime < knownStart && endTime > knownEnd)) {
			values = updateValues(startTime, endTime);
			isOwnList = false;
			knownStart = startTime;
			knownEnd = endTime;
			updateValueLimits();
		/*} else if(startTime < knownStart && endTime > knownEnd) {
			values = updateValues(startTime, endTime);
			isOwnList = false;
			knownStart = startTime;
			knownEnd = endTime;			
			updateValueLimits();*/
		} else if(startTime < knownStart) {
			List<SampledValue> newVals = updateValues(startTime, knownStart);
			addValues(newVals);
			//List<SampledValue> concat = new ArrayList<SampledValue>(newVals);
			//concat.addAll(values);
			/*List<SampledValue> concat = new ArrayList<SampledValue>(values);
			values = concat;
			values.addAll(newVals);
			isOwnList = true;*/
			knownStart = startTime;	
			updateValueLimits();
		} else if(endTime > knownEnd) {
//logger.error("Greater endTime PROT1 knownEnd:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(knownEnd)+" endTime:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(endTime));
			List<SampledValue> newVals = updateValues(knownEnd, endTime);
//logger.error("Found new vals:"+values.size());
			addValues(newVals);
			/*if(isOwnList) {
				try {
					values.addAll(newVals);
				} catch(UnsupportedOperationException e) {
					//TODO: Should not occur
					List<SampledValue> concat = new ArrayList<SampledValue>(values);
					concat.addAll(newVals);
					values = concat;
				}
			} else {
				List<SampledValue> concat = new ArrayList<SampledValue>(values);
				concat.addAll(newVals);
				values = concat;
				isOwnList = true;
			}*/
			knownEnd = endTime;			
			updateValueLimits();
		}
		} finally {
			updateLock.unlock();
		}
		return getValuesWithoutUpdate(startTime, endTime);
	}

	protected List<SampledValue> getValuesWithoutUpdate(long startTime, long endTime) {
		if(startTime > lastValueInList)
			return Collections.emptyList();
		if(endTime < firstValueInList)
			return Collections.emptyList();
		if(startTime <= firstValueInList && endTime > lastValueInList) {
			return new ArrayList<SampledValue>(values);
		}
		if(values.isEmpty())
			return Collections.emptyList();
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
		return result;		
	}
	
	protected void addValues(List<SampledValue> newVals) {
		List<SampledValue> existing = null;
		if(!newVals.isEmpty()) {
			long newFirst = newVals.get(0).getTimestamp();
			long newLast = newVals.get(newVals.size()-1).getTimestamp();
			List<SampledValue> existingLoc = getValuesWithoutUpdate(newFirst, newLast);
			if(!existingLoc.isEmpty()) {
				logger.error("   !!!! Overwriting values without registration in getIntervalsToUpdate !!!");
				existing = existingLoc;
			}
		}		
		if(isOwnList) {
			try {
				if(existing != null)
					values.removeAll(existing);
				values.addAll(newVals);
			} catch(UnsupportedOperationException e) {
				//TODO: Should not occur
				List<SampledValue> concat = new ArrayList<SampledValue>(values);
				if(existing != null)
					concat.removeAll(existing);
				concat.addAll(newVals);
				values = concat;
			}
		} else {
			List<SampledValue> concat = new ArrayList<SampledValue>(values);
			if(existing != null)
				concat.removeAll(existing);
			concat.addAll(newVals);
			values = concat;
			isOwnList = true;
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

	@Override
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
			return result ;			
		}
	}
}
