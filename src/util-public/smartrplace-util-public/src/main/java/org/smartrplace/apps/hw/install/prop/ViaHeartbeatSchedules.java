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
import org.ogema.timeseries.eval.simple.api.TimeProcPrint;
import org.ogema.tools.resource.util.TimeSeriesUtils;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatInfoProvider.StringProvider;
import org.smartrplace.util.frontend.servlet.ServletTimeseriesProvider;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;

/** Basic version is just for reading, the derived class {@link ViaHeartbeatSchedulesWrite} below in this file is
 * also for receiving data.<br>
 * Usually only additional values are sent that are added to the schedule. If the clean flag is set in the JSON
 * transmitted then the entire schedule is deleted before new values are written. This is triggered by calling
 * {@link #resendAllOnNextOccasion()} on the source system. By default this occurs when the reference time is changed
 * or when a datapoint change notification is detected in getValues or setValue (more details to be provided). This is
 * NOT triggered on startup of a gateway.*/
public class ViaHeartbeatSchedules implements StringProvider {
	protected final ReadOnlyTimeSeries rot;
	protected boolean doClean = false;
	protected long lastValueSent = -1;
	protected long lastValueUpdateSent = -1;
	protected long lastClean = -1;
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
		if(!vals.isEmpty()) {
			lastValueSent = vals.get(vals.size()-1).getTimestamp();
			lastValueUpdateSent = now;
		}
		json.put("values", arr);
		if(doClean) {
			json.put("clean", doClean);
			doClean = false;
			lastClean = now;
			System.out.println("Sending clean for schedule "+alias!=null?alias:TimeProcPrint.getTimeseriesName(rot, true));
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
		protected long lastValueReceiveTime = -1;
		
		public ViaHeartbeatSchedulesWrite(Schedule sched, String alias) {
			super(sched, alias);
			this.sched = sched;
		}
		
		@Override
		public void received(String strValue, long now) {
			JSONObject json = new JSONObject(strValue);
			if(json.has("clean")) {
				boolean doClean = json.getBoolean("clean");
				if(doClean) {
					sched.deleteValues();
					System.out.println("Received clean for schedule "+alias!=null?alias:TimeProcPrint.getTimeseriesName(rot, true));
					lastClean = now;
				}
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
				lastValueReceiveTime = now;
			}
		}

		//just for debugging
		public long getLastRecvTime() {
			return lastValueReceiveTime;
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
	
	//just for debugging
	public long getLastValueWritten() {
		return lastValueSent;
	}
	public long getLastUpdateSent() {
		return lastValueUpdateSent;
	}

	@Override
	public long getLastClean() {
		return lastClean;
	}	
}
