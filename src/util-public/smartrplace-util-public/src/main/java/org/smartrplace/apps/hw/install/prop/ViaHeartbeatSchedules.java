package org.smartrplace.apps.hw.install.prop;

import java.util.List;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries2;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatInfoProvider.StringProvider;
import org.smartrplace.util.frontend.servlet.ServletTimeseriesProvider;

import de.iwes.util.format.StringFormatHelper;

/** Basic version is just for reading*/
public class ViaHeartbeatSchedules implements StringProvider {
	protected final ReadOnlyTimeSeries rot;
	protected boolean doClean = false;
	protected long lastValueSent = -1;
	protected final String alias;

	public ViaHeartbeatSchedules(ReadOnlyTimeSeries rot, String alias) {
		this.rot = rot;
		this.alias = alias;
	}

	@Override
	public void received(String strValue, long now) {
		throw new IllegalStateException("Use ViaHeartbeatSchedulesWrite if you need to receive values!");
	}

	@Override
	public String getStringToSend(long now) {
		JSONObject json = new JSONObject();
		List<SampledValue> vals = rot.getValues(lastValueSent+1);
//if(rot instanceof ProcessedReadOnlyTimeSeries2 && ((ProcessedReadOnlyTimeSeries2)rot).getInputDp().id().startsWith("EnergyServerReadings_ESE/ESE_location_39")) //39/connection/energyDaily/reading"))
//System.out.println("vals#:"+vals.size()+" lastValueSent:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastValueSent));
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
		if(lastExisting == null)
			return false;
		return lastExisting.getTimestamp() > lastValueSent;
	}
	
	public void resendAllOnNextOccasion() {
		doClean = true;
		lastValueSent = 0;
	}
	
	public static class ViaHeartbeatSchedulesWrite extends ViaHeartbeatSchedules {
		protected final Schedule sched;
		
		public ViaHeartbeatSchedulesWrite(Schedule sched, String alias) {
			super(sched, alias);
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
			float val = -999;
			for(int i=0; i<len; i++) {
				JSONObject in = arr.getJSONObject(i);
				long ts;
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
			if(len > 0) {
				Resource parent = sched.getParent();
				if(parent != null && (parent instanceof FloatResource))
					((FloatResource)parent).setValue(val);
			}
		}
	}
	
	public static ViaHeartbeatSchedules registerDatapointForHeartbeatDp2Schedule(Datapoint dp) {
		Set<String> als = dp.getAliases();
		return registerDatapointForHeartbeatDp2Schedule(dp, als.isEmpty()?null:als.iterator().next());
	}
	public static ViaHeartbeatSchedules registerDatapointForHeartbeatDp2Schedule(Datapoint dp, String alias) {
		ViaHeartbeatSchedules schedProv = new ViaHeartbeatSchedules(dp.getTimeSeries(), alias);
		// Both datapoints can be addressed via heartbeat and will return the same data
		dp.setParameter(Datapoint.HEARTBEAT_STRING_PROVIDER_PARAM, schedProv);
		return schedProv;
	}

	@Override
	public String getAlias() {
		return alias;
	}
}
