package org.smartrplace.util.frontend.servlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

public class ServletTimeseriesProvider implements ServletValueProvider {
	public static final long MILLIS_TO_SERAPATE_DAY = (13*60*60000);
	protected final float deleteValue;
	protected final Float minValue;
	
	//Special evaluations
	protected final ReadOnlyTimeSeries bareTimeSeries;
	protected final String evaluationMode;
	
	protected final ApplicationManager appMan;
	protected final Map<String, String[]> paramMap;
	protected final Long startTime;
	protected final long endTime;
	
	public enum WriteMode {
		ANY,
		/** In mode ONE_PER_DAY only one value per day is permitted, if a new value for a day is 
		 * written, then any existing values for the day are deleted*/
		ONE_PER_DAY,
		/** Like ONE_PER_DAY, but two values for morning and afternoon are supported. The day is separated around 13:00*/
		TWO_PER_DAY
	}
	/** Only relevant if startTime and endTime are not provided*/
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
	
	public ServletTimeseriesProvider(String name, ReadOnlyTimeSeries bareTimeseries, ApplicationManager appMan,
			String evaluationMode, Map<String, String[]> paramMap) {
		this(name, bareTimeseries, appMan, evaluationMode, null, null, paramMap);
	}

	public ServletTimeseriesProvider(String name, ReadOnlyTimeSeries bareTimeseries, ApplicationManager appMan,
			String evaluationMode, WriteMode writeMode, PreviousDayMode previousDayMode,
			Map<String, String[]> paramMap) {
		this(name, bareTimeseries, appMan, evaluationMode, writeMode, previousDayMode,
				paramMap, 0, 0f);
	}
	public ServletTimeseriesProvider(String name, ReadOnlyTimeSeries bareTimeseries, ApplicationManager appMan,
			String evaluationMode, WriteMode writeMode, PreviousDayMode previousDayMode,
			Map<String, String[]> paramMap,
			float deleteValue, Float minValue) {
		this.appMan = appMan;
		this.writeMode = null;
		this.previousDayMode = null;
		this.name = name;
		this.bareTimeSeries = bareTimeseries;
		this.evaluationMode = evaluationMode;
		this.paramMap = paramMap;
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
	public JSONObject getJSON(String user, String key) {
		JSONObject json = new JSONObject();
		json.put("name", name); //res.name().getValue());
		long[] startEnd;
		if(startTime != null) {
			startEnd = new long[]{startTime, endTime};
		} else
			startEnd = getDayStartEnd(key);
		final List<SampledValue> vals;
		if(evaluationMode != null) {
			//TODO
			vals = bareTimeSeries.getValues(startEnd[0], startEnd[1]);
			json.put("value", vals);
			return json;
		} else
			vals = bareTimeSeries.getValues(startEnd[0], startEnd[1]);
		json.put("todaysValues", smapledValuesToJson(vals));
		if(previousDayMode != null && previousDayMode != PreviousDayMode.NONE) {
			long ydayStart = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startEnd[0], -1, AbsoluteTiming.DAY);
			final List<SampledValue> yvals = bareTimeSeries.getValues(ydayStart, startEnd[0]-1); //sched.getValues(ydayStart, startEnd[0]-1);
			if(previousDayMode == PreviousDayMode.ONE_PER_DAY && yvals.size() > 1) {
				final List<SampledValue> singleValList = new ArrayList<>();
				singleValList.add(yvals.get(yvals.size()-1));
				json.put("yesterdayValues", singleValList);
			} else
				json.put("yesterdayValues", yvals);
			if(!vals.isEmpty()) {
				final List<SampledValue> singleValList = new ArrayList<>();
				singleValList.add(vals.get(vals.size()-1));
				json.put("preSetValue", singleValList);
			} else if(!yvals.isEmpty()) {
				final List<SampledValue> singleValList = new ArrayList<>();
				singleValList.add(yvals.get(yvals.size()-1));
				json.put("preSetValue", singleValList);
			} else
				json.put("preSetValue", yvals);
		}
		return json;
	}

	protected long[] getDayStartEnd(String key) {
		long now;
		if(key == null || (!key.contains(UserServlet.TIMEPREFIX)))
			now = appMan.getFrameworkTime();
		else
			now = Long.parseLong(key.substring(key.indexOf(UserServlet.TIMEPREFIX)+UserServlet.TIMEPREFIX.length()));
		long start = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.DAY);
		long end = AbsoluteTimeHelper.addIntervalsFromAlignedTime(start, 1, AbsoluteTiming.DAY);
		return new long[] {start, end};
	}
	
	protected long lastTimestamp = -1;
	
	@Override
	public void setValue(String user, String key, String value) {
		try  {
			if(!(bareTimeSeries instanceof Schedule))
				throw new IllegalStateException("Writing only possible for time series of type schedule!");
			Schedule sched = (Schedule) bareTimeSeries;
			JSONObject in = new JSONObject(value);
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
	
	protected JSONArray smapledValuesToJson(List<SampledValue> vals) {
		JSONArray result = new JSONArray();
		for(SampledValue sv: vals) {
			//JSONObject svObj = new JSONObject();
			Map<Long, Float> svMap = new HashMap<>();
			svMap.put(sv.getTimestamp(), sv.getValue().getFloatValue());
			result.put(svMap);
		}
		return result;
	}
}
