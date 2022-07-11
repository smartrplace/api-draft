package org.smartrplace.util.frontend.servlet;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.timeseries.eval.simple.api.TimeProcPrint;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.util.frontend.servlet.UserServlet.JSONVarrRes;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

public class ServletTimeseriesProvider implements ServletValueProvider {
	public static final long MILLIS_TO_SERAPATE_DAY = (13*60*60000);
	protected final float deleteValue;
	protected final Float minValue;
	
	//Special evaluations
	protected final ReadOnlyTimeSeries timeSeries;
	protected final String evaluationMode;
	
	protected final ApplicationManager appMan;
	protected final Map<String, String[]> paramMap;
	protected final UserServletParamData pData;
	protected final Long startTime;
	protected final long endTime;
	
	//set from outside if necessary
	public String unit = null;
	public String label = null;
	public String align = null;
	public boolean addUTCOffset = false;
	
	//First factor, then offset are applied to gateway values on reading (GET)
	//On write a reverse transformation is applied, but not tested yet
	//Does not work if evaluationMode != null
	public Float factor = null;
	public Float offset = null;
	
	/** Set this to get notifications, only relevant if WriteMode==ANY*/
	public Datapoint datapointForChangeNotification = null;
	
	//Overwrite if POST shall not always be accepted
	public boolean acceptPOST(String user, String key, String value) {return true;}
	//set to true if only integers shall be written
	public boolean integerOnly = false;

	public static enum DownSamplingMode {
		AVERAGE,
		/** If more than one value is used to calculate a downsampled value the minimum and the maximum are both included*/
		MINMAX,
	}
	
	public enum WriteMode {
		ANY,
		/** In mode ONE_PER_DAY only one value per day is permitted, if a new value for a day is 
		 * written, then any existing values for the day are deleted*/
		ONE_PER_DAY,
		/** Like ONE_PER_DAY, but two values for morning and afternoon are supported. The day is separated around 13:00*/
		TWO_PER_DAY
	}
	/** Only relevant if startTime and endTime are not provided. This is especially important for automated pre-setting of
	 * manual entry values*/
	public enum PreviousDayMode {
		//do not transmit previous day values
		NONE,
		//transmit all previous day values
		ANY,
		/** In mode ONE_PER_DAY only the last value for the previous day is transmitted*/
		ONE_PER_DAY,
	}
	protected final WriteMode writeMode;
	protected final PreviousDayMode previousDayMode;
	
	public ServletTimeseriesProvider(String name, ReadOnlyTimeSeries timeseries, ApplicationManager appMan,
			String evaluationMode, Map<String, String[]> paramMap) {
		this(name, timeseries, appMan, evaluationMode, null, null, paramMap);
	}

	public ServletTimeseriesProvider(String name, ReadOnlyTimeSeries timeseries, ApplicationManager appMan,
			String evaluationMode, WriteMode writeMode, PreviousDayMode previousDayMode,
			Map<String, String[]> paramMap) {
		this(name, timeseries, appMan, evaluationMode, writeMode, previousDayMode,
				paramMap, 0, 0f);
	}
	public ServletTimeseriesProvider(String name, ReadOnlyTimeSeries timeseries, ApplicationManager appMan,
			String evaluationMode, WriteMode writeMode, PreviousDayMode previousDayMode,
			Map<String, String[]> paramMap,
			float deleteValue, Float minValue) {
		this.appMan = appMan;
		this.writeMode = writeMode;
		this.previousDayMode = null;
		this.name = name;
		this.timeSeries = timeseries;
		this.evaluationMode = evaluationMode;
		this.paramMap = paramMap;
		this.pData = new UserServletParamData(paramMap);
		this.deleteValue = deleteValue;
		this.minValue = minValue;
		Long start;
		long end = -1;
		try {
			start = Long.parseLong(UserServlet.getParameter("startTime", paramMap));
			end = Long.parseLong(UserServlet.getParameter("endTime", paramMap));
		} catch(NumberFormatException | NullPointerException e) {
			start = null;
		}
		this.startTime = start;
		this.endTime = end;
	}
	
	protected String name;
	public String getName() {
		return name;
	}
	
	@Override
	public ValueMode getValueMode() {
		return ValueMode.JSON;
	}
	
	@Override
	public Value getValue(String user, String key) {
		throw new UnsupportedOperationException("Use getJSON!!!");
	}
	@Override
	public JSONVarrRes getJSON(String user, String key) {
		JSONObject json = new JSONObject();
		Integer valueDist = UserServlet.getInteger("valueDist", paramMap);
		//json.put("name", name); //res.name().getValue());
		long[] startEnd;
		if(startTime != null) {
			startEnd = new long[]{startTime, endTime};
		} else
			startEnd = getDayStartEnd(paramMap, appMan, key); //getDayStartEnd(key);
		final List<SampledValue> vals;
		if(evaluationMode != null) {
			//TODO
			vals = timeSeries.getValues(startEnd[0], startEnd[1]);
			json.put("value", vals);
			
			JSONVarrRes realResult = new JSONVarrRes();
			realResult.result = json;
			if(unit != null)
				json.put("unit", unit);
			if(align != null)
				json.put("align", align);
			if(label != null)
				json.put("label", label);
			if(addUTCOffset) {
				ZoneOffset utcOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
				json.put("UTCoffset", ""+utcOffset.getTotalSeconds()*1000);
			}
			return realResult;
		} else
			vals = timeSeries.getValues(startEnd[0], startEnd[1]);
		boolean shortXY = UserServlet.getBoolean("shortXY", paramMap);
		boolean structList = pData.structureList;
		if(!structList) {
			String structureStr = UserServlet.getParameter("structure", paramMap);
			if(structureStr != null && structureStr.equals("tslist"))
				structList = true;
		}
		SampledToJSonResult mainRes = smapledValuesToJson(vals, valueDist, valueDist==null?null:DownSamplingMode.MINMAX,
				structList, shortXY, pData.suppressNan, factor, offset, startEnd[1]+30*TimeProcUtil.DAY_MILLIS);
		json.put("values", mainRes.arr);
		if(mainRes.errorTs != null) {
			String loc;
			if(datapointForChangeNotification!=null) {
				loc = datapointForChangeNotification.getLocation();
			} else if(label != null)
				loc = label+" / "+TimeProcPrint.getTimeseriesName(timeSeries, true);
			else
				loc = TimeProcPrint.getTimeseriesName(timeSeries, true);
			appMan.getLogger().warn("Faulty timestamp "+mainRes.errorTs+" in "+loc);
			ValueResourceHelper.setCreate(
					ResourceHelper.getLocalDevice(appMan).logFileCheckNotification(), 1);
		}
		if(unit != null)
			json.put("unit", unit);
		if(align != null)
			json.put("align", align);
		if(label != null)
			json.put("label", label);
		if(addUTCOffset) {
			ZoneOffset utcOffset = ZoneOffset.systemDefault().getRules().getOffset(Instant.now());
			json.put("UTCoffset", ""+utcOffset.getTotalSeconds()*1000);
		}

		JSONVarrRes realResult = new JSONVarrRes();
		realResult.result = json;
		return realResult;
	}

	protected long[] getDayStartEnd(String key) {
		return getDayStartEnd(key, appMan);
	}
	public static long[] getDayStartEnd(String key, ApplicationManager appMan) {
		long now;
		if(key == null || (!key.contains(UserServlet.TIMEPREFIX)))
			now = appMan.getFrameworkTime();
		else
			now = Long.parseLong(key.substring(key.indexOf(UserServlet.TIMEPREFIX)+UserServlet.TIMEPREFIX.length()));
		long start = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.DAY);
		long end = AbsoluteTimeHelper.addIntervalsFromAlignedTime(start, 1, AbsoluteTiming.DAY);
		return new long[] {start, end};
	}
	public static long[] getDayStartEnd(Map<String, String[]> paramMap, ApplicationManager appMan) {
		return getDayStartEnd(paramMap, appMan, null);
	}
	public static long[] getDayStartEnd(Map<String, String[]> paramMap, ApplicationManager appMan, String key) {
		String align = UserServlet.getParameter("align", paramMap);
		long start = -1;
		try {
			start = Long.parseLong(UserServlet.getParameter("startTime", paramMap));
			long end = Long.parseLong(UserServlet.getParameter("endTime", paramMap));
			if(align != null && align.equals("day")) {
				long startDay = AbsoluteTimeHelper.getIntervalStart(start, AbsoluteTiming.DAY);
				long endDay = AbsoluteTimeHelper.getNextStepTime(end, AbsoluteTiming.DAY)-1;
				return new long[] {startDay, endDay};
			}
			return new long[] {start, end};
		} catch(NumberFormatException | NullPointerException e) {
			if(align != null && align.equals("day") && start > 0) {
				long startDay = AbsoluteTimeHelper.getIntervalStart(start, AbsoluteTiming.DAY);
				long endDay = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startDay, 1, AbsoluteTiming.DAY)-1;
				return new long[] {startDay, endDay};
			}
			return getDayStartEnd(key, appMan);
		}		
	}
	
	protected long lastTimestamp = -1;
	
	/** TODO: Currently we only supprt structureList=true and shortXY=false*/
	@Override
	public void setValue(String user, String key, String value) {
		String align = UserServlet.getParameter("align", paramMap);
		try  {
			if(!(timeSeries instanceof Schedule))
				throw new IllegalStateException("Writing only possible for time series of type schedule!");
			Schedule sched = (Schedule) timeSeries;
			if(!acceptPOST(user, key, value)) {
				throw new IllegalStateException("POST not accepted for "+sched.getLocation());
			}
			JSONObject in = new JSONObject(value);
			if(writeMode == WriteMode.ANY) {
				setValueAny(sched, value, align != null && align.equals("day"));
				return;
			}			
			long ts = in.getLong("timestamp");
			if(Math.abs(ts - lastTimestamp) < 90000)
				return;
			lastTimestamp = ts;
			float val = (float)(in.getDouble("value"));
			if(val == deleteValue || (minValue != null && val < minValue))
				return;
			if(writeMode == WriteMode.ONE_PER_DAY) {
				long start = AbsoluteTimeHelper.getIntervalStart(ts, AbsoluteTiming.DAY);
				long end = AbsoluteTimeHelper.addIntervalsFromAlignedTime(start, 1, AbsoluteTiming.DAY);
				sched.deleteValues(start, end);
			} else if(writeMode == WriteMode.TWO_PER_DAY) {
				long start = AbsoluteTimeHelper.getIntervalStart(ts, AbsoluteTiming.DAY);
				final long end;
				if(ts-start > MILLIS_TO_SERAPATE_DAY) {
					//afternoon
					start += MILLIS_TO_SERAPATE_DAY;
					end = AbsoluteTimeHelper.addIntervalsFromAlignedTime(start, 1, AbsoluteTiming.DAY);
				} else {
					//morning
					end = start + MILLIS_TO_SERAPATE_DAY;
				}
				sched.deleteValues(start, end);
			}
			sched.addValue(ts, new FloatValue(val));
		} catch(NumberFormatException e) {
			//do nothing
		}
	}
	
	public void setValueAny(Schedule sched, String value, boolean alignedDay) {
		try  {
			JSONObject in = new JSONObject(value);
			if(in.has("values")) {
				JSONArray values = in.getJSONArray("values");
				long firstTs = Long.MAX_VALUE;
				long lastTs = -1;
				for(int i=0; i<values.length(); i++) {
					JSONObject jval = values.getJSONObject(i);
					long tsloc = jval.getLong("timestamp");
					float val = (float)(jval.getDouble("value"));
					if(val == deleteValue) {
						if(alignedDay) {
							long startDay = AbsoluteTimeHelper.getIntervalStart(tsloc, AbsoluteTiming.DAY);
							long endDay = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startDay, 1, AbsoluteTiming.DAY);
							sched.deleteValues(startDay, endDay);
							if(datapointForChangeNotification != null)
								datapointForChangeNotification.notifyTimeseriesChange(startDay, endDay);
						} else {
							sched.deleteValues(tsloc, tsloc+1);
							if(datapointForChangeNotification != null)
								datapointForChangeNotification.notifyTimeseriesChange(tsloc, tsloc+1);
						}
						continue;
					}
					sched.addValue(tsloc, new FloatValue(val));
					if(tsloc < firstTs)
						firstTs = tsloc;
					if(tsloc > lastTs)
						lastTs = tsloc;
				}
				if(lastTs > 0 && datapointForChangeNotification != null)
					datapointForChangeNotification.notifyTimeseriesChange(firstTs, lastTs);
				return;
			}
			long ts = in.getLong("timestamp");
			if(Math.abs(ts - lastTimestamp) < 90000)
				return;
			lastTimestamp = ts;
			float val;
			try {
				val = (float)(in.getDouble("value"));
			} catch(Exception e) {
				val = deleteValue;
			}
			boolean doDelete = false;
			if(integerOnly)
				val = Math.round(val);
			if(val == deleteValue || (minValue != null && val < minValue))
				if(writeMode != WriteMode.ANY)
					doDelete = true;
				else
					return;
			if(writeMode == WriteMode.ONE_PER_DAY) {
				long start = AbsoluteTimeHelper.getIntervalStart(ts, AbsoluteTiming.DAY);
				long end = AbsoluteTimeHelper.addIntervalsFromAlignedTime(start, 1, AbsoluteTiming.DAY);
				sched.deleteValues(start, end);
			} else if(writeMode == WriteMode.TWO_PER_DAY) {
				long start = AbsoluteTimeHelper.getIntervalStart(ts, AbsoluteTiming.DAY);
				final long end;
				if(ts-start > MILLIS_TO_SERAPATE_DAY) {
					//afternoon
					start += MILLIS_TO_SERAPATE_DAY;
					end = AbsoluteTimeHelper.addIntervalsFromAlignedTime(start, 1, AbsoluteTiming.DAY);
				} else {
					//morning
					end = start + MILLIS_TO_SERAPATE_DAY;
				}
				sched.deleteValues(start, end);
			}
			if(doDelete)
				return;
			if(offset != null)
				val = val - offset;
			if(factor != null)
				val = factor/val;
			sched.addValue(ts, new FloatValue(val));
		} catch(NumberFormatException e) {
			//do nothing
		}
	}
	
	public void deleteValues(long start, long end) {
		Schedule sched = (Schedule) timeSeries;
		sched.deleteValues(start, end);
	}

	/** Structure for passing on downsampling information from one input sample to the next one*/
	static class DownSamplingData {
		long lastTs = -1; //time stamp of last SampledValue written
		Long lastTsCollected = null; //time stamp of last input SampledValue processed IF it is not part of an output value yet
		float maxVal = -Float.MAX_VALUE;
		float minVal = -Float.MAX_VALUE;
		boolean suppressNaN;
	}

	public static class SampledToJSonResult {
		public JSONArray arr;
		public Long errorTs = null;
	}
	/** Provides timeseries as JSON.
	 * Performs downsampling with different modes if desired 
	 * @param vals all values in the time series in the selected time range before downsampling
	 * @param valueDist expected time in milliseconds between values returned. If null then then no
	 * 		downsampling is performed, in this case mode is not relevant
	 * @param mode if valueDist is not null then this must be DownSamplingMode.MINMAX. For more modes
	 * 		to be foreseen for the future see {@link DownSamplingMode}.
	 * @param structureList if true then the result will be in the form of "time":_timeStamp_, "value":_value_,
	 * 		otherwise _timestamp_:_value_
	 * @param shortXY only relevant if structureList is true. If this is true then "time" is replaced by "x",
	 * 		"value" is replaced by "y".
	 * @param suppressNaN if true then any NaN and infinity values are removed from the result
	 * @return JSON to be returned as value of the object result
	 */
	public static SampledToJSonResult smapledValuesToJson(List<SampledValue> vals, Integer valueDist, DownSamplingMode mode,
			boolean structureList, boolean shortXY, boolean suppressNaN, Float factor, Float offset, long maxTsAllowed) {
		if(valueDist != null && mode != DownSamplingMode.MINMAX)
			throw new UnsupportedOperationException("Downsampling mode AVERAGE not implemented yet!");
		SampledToJSonResult mainRes = new SampledToJSonResult();
		JSONArray result = new JSONArray();
		mainRes.arr = result;
		
		DownSamplingData data = new DownSamplingData();
		data.suppressNaN = suppressNaN;
		
		long lastBaseTs = -1;
		int idx = -1;
		for(SampledValue sv: vals) {
			idx++;
			if(sv.getTimestamp() < lastBaseTs) {
				//System.out.println("BASE Timestamp["+idx+"] in wrong order:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(sv.getTimestamp())+
				//		" given after:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastBaseTs));
				if(mainRes.errorTs != null && lastBaseTs > mainRes.errorTs)
					mainRes.errorTs = lastBaseTs;
				continue;
			} else if(sv.getTimestamp() > maxTsAllowed) {
				mainRes.errorTs = lastBaseTs;
				continue;
			} else
				lastBaseTs = sv.getTimestamp();
			LinkedHashMap<Long, Float> svMap = new LinkedHashMap<>();
			Float fval = UserServletUtil.getJSONValue(sv.getValue().getFloatValue());
			if(fval != null) {
				if(factor != null)
					fval = factor*fval;
				if(offset != null)
					fval = fval+offset;
				if(valueDist != null) {
					long tsNow = sv.getTimestamp();
					processMinMaxDownSampling(data , tsNow, valueDist, fval, svMap);
				} else //if valueDist != null
					svMap.put(sv.getTimestamp(), fval);
			}
			if(structureList) {
				for(Entry<Long, Float> ts: svMap.entrySet()) {
					JSONObject sub = new JSONObject();
					if(shortXY) {
						sub.put("x", ts.getKey());
						sub.put("y", ts.getValue());						
					} else {
						sub.put("time", ts.getKey());
						sub.put("value", ts.getValue());
					}
					result.put(sub);
				}
			} else if(!svMap.isEmpty()) {
				if(svMap.size() > 1) {
					for(Entry<Long, Float> e: svMap.entrySet()) {
						LinkedHashMap<Long, Float> newMap = new LinkedHashMap<Long, Float>();
						newMap.put(e.getKey(), e.getValue());
						result.put(newMap);
					}
				} else
					result.put(svMap);
			}
		}
		return mainRes;
	}
	
	/** Make sure all values that are put into svMap have at least a distance of valueDist. If more than one value
	 * is aggregated put minimum and maximum if they differ
	 * 
	 * @param data
	 * @param tsNow
	 * @param valueDist
	 * @param fval
	 * @param svMap can contain between 0 and 3 pairs of Long, Float (sampled values)
	 */
	protected static void processMinMaxDownSampling(DownSamplingData data, long tsNow, int valueDist, float fval,
			LinkedHashMap<Long, Float> svMap) {
		if(data.lastTsCollected == null) {
			if((tsNow - data.lastTs) < valueDist) {
				data.maxVal = fval;
				data.minVal = fval;
				data.lastTsCollected = tsNow;
			} else {
				//we just write out the input value
				data.lastTs = putDownSampledTs(tsNow, fval, svMap, data.suppressNaN, data);
			}
		} else {
			if((tsNow - data.lastTs) < valueDist) {
				if(fval > data.maxVal)
					data.maxVal = fval;
				else if(fval < data.minVal)
					data.minVal = fval;
				data.lastTsCollected = tsNow;
			} else {
				data.lastTs = putDownSampledTs(data.lastTsCollected,data.minVal, svMap, data.suppressNaN, data);
				if(data.maxVal != data.minVal)
					putDownSampledTs(data.lastTsCollected, data.maxVal, svMap, data.suppressNaN, data);
				data.lastTsCollected = null;
				//The new input value has not been processed. We process it with condition lastTsCollected=null
				processMinMaxDownSampling(data, tsNow, valueDist, fval, svMap);
			}
		}		
	}
	
	/** Add new SampledValue to map if value fulfills NaN reuqirements
	 * 
	 * @param timeStamp time stamp of SampledValue
	 * @param fval value of SampledValue
	 * @param svMap
	 * @param suppressNaN
	 * @return timeStamp
	 */
	protected static long putDownSampledTs(long timeStamp, float fval,
			LinkedHashMap<Long, Float> svMap, boolean suppressNaN,
			DownSamplingData data) {
		if(timeStamp < data.lastTs) {
			System.out.println("Timestamp in wrong order:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(timeStamp));
			return data.lastTs;
		}
		if(suppressNaN && (Float.isNaN(fval) || Float.isInfinite(fval) || (fval == Float.MAX_VALUE) || (fval == -Float.MAX_VALUE)))
			return timeStamp;
		svMap.put(timeStamp, fval);
		return timeStamp;		
	}
}
