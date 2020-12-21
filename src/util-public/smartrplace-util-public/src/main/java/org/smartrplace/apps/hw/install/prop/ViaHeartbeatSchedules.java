package org.smartrplace.apps.hw.install.prop;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatInfoProvider.StringProvider;
import org.smartrplace.util.frontend.servlet.ServletTimeseriesProvider;

/** Basic version is just for reading*/
public class ViaHeartbeatSchedules implements StringProvider {
	protected final ReadOnlyTimeSeries rot;
	protected boolean doClean = false;
	protected long lastValueSent = -1;

	public ViaHeartbeatSchedules(ReadOnlyTimeSeries rot) {
		this.rot = rot;
	}

	@Override
	public void received(String strValue, long now) {
		throw new IllegalStateException("Use ViaHeartbeatSchedulesWrite if you need to receive values!");
	}

	@Override
	public String getStringToSend(long now) {
		JSONObject json = new JSONObject();
		List<SampledValue> vals = rot.getValues(lastValueSent+1);
		JSONArray arr = ServletTimeseriesProvider.smapledValuesToJson(vals, null, null, true, false, true);
		if(!vals.isEmpty())
			lastValueSent = vals.get(vals.size()-1).getTimestamp();
		json.put("values", arr);
		if(doClean) {
			json.put("clean", doClean);
			doClean = false;
		}
		return json.toString();
	}

	@Override
	public boolean hasNewValue(long now) {
		SampledValue lastExisting = rot.getPreviousValue(Long.MAX_VALUE);
		return lastExisting.getTimestamp() > lastValueSent;
	}
	
	public void resendAllOnNextOccasion() {
		doClean = true;
		lastValueSent = 0;
	}
	
	public static class ViaHeartbeatSchedulesWrite extends ViaHeartbeatSchedules {
		protected final Schedule sched;
		
		public ViaHeartbeatSchedulesWrite(Schedule sched) {
			super(sched);
			this.sched = sched;
		}
		
		@Override
		public void received(String strValue, long now) {
			JSONObject json = new JSONObject(strValue);
			if(json.has("clean")) {
				boolean doClean = json.getBoolean("clean");
				if(doClean)
					sched.deleteValues();
			}
			JSONArray arr = json.getJSONArray("values");
			int len = arr.length();
			for(int i=0; i<len; i++) {
				JSONObject in = arr.getJSONObject(i);
				long ts;
				float val;
				if(in.has("x")) {
					ts = in.getLong("x");
					val = (float)(in.getDouble("y"));					
				} else {
					if(!in.has("time"))
						continue;
					ts = in.getLong("time");
					val = (float)(in.getDouble("value"));
				}
				sched.addValue(ts, new FloatValue(val));
			}
			
		}
	}
}
