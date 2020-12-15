package org.ogema.timeseries.eval.simple.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
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
 * aligned aggregated value is updated all the time until the next aligned interval begins.*/
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
	}

	@Override
	public List<SampledValue> getValues(long startTime, long endTime) {
		if(knownEndUpdateInterval != null) {
			long now = getCurrentTime();
			if(now - lastKnownEndUpdate > knownEndUpdateInterval) {
				knownEnd = lastKnownEndUpdate;
				lastKnownEndUpdate = now;
			}
		}
		if(knownStart < 0) {
			values = updateValues(startTime, endTime);
			isOwnList = false;
			knownStart = startTime;
			knownEnd = endTime;
			updateValueLimits();
		} else if(startTime < knownStart && endTime > knownEnd) {
			values = updateValues(startTime, endTime);
			isOwnList = false;
			knownStart = startTime;
			knownEnd = endTime;			
			updateValueLimits();
		} else if(startTime < knownStart) {
			List<SampledValue> newVals = updateValues(startTime, knownStart);
			List<SampledValue> concat = new ArrayList<SampledValue>(newVals);
			concat.addAll(values);
			values = concat;
			isOwnList = true;
			knownStart = startTime;			
			updateValueLimits();
		} else if(endTime > knownEnd) {
logger.error("Greater endTime PROT1 knownEnd:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(knownEnd)+" endTime:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(endTime));
			List<SampledValue> newVals = updateValues(knownEnd, endTime);
logger.error("Found new vals:"+values.size());
			if(isOwnList) {
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
			}
			knownEnd = endTime;			
			updateValueLimits();
		} else
logger.error("No new values in PROT1 knownEnd:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(knownEnd));
		if(startTime > lastValueInList)
			return Collections.emptyList();
		if(endTime < firstValueInList)
			return Collections.emptyList();
		if(startTime <= firstValueInList && endTime >= lastValueInList) {
			return values;
		}
		if(values.isEmpty())
			return values;
		int fromIndex = -1;
		int toIndex = -1;
		for(int i=0; i<values.size(); i++) {
			if(values.get(i).getTimestamp() >= startTime) {
				fromIndex = i;
				break;
			}
		}
		if(fromIndex < 0)
			throw new IllegalStateException("Should not occur!");
		if(endTime == startTime)
			toIndex = fromIndex;
		else for(int i=values.size()-1; i>=fromIndex; i--) {
			if(values.get(i).getTimestamp() <= endTime) {
				toIndex = i;
				break;
			}
		}
		if(fromIndex > toIndex)
			return Collections.emptyList();
		List<SampledValue> result = values.subList(fromIndex, toIndex+1);
		return result;
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
		List<SampledValue> asList = getValues(time, Long.MAX_VALUE);
		if(asList.isEmpty())
			return null;
		else
			return asList.get(0);
	}

	@Override
	public SampledValue getPreviousValue(long time) {
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

}
