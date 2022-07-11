package org.smartrplace.apps.hw.install.prop;

import java.util.Collections;
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
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.timeseries.eval.simple.api.TimeProcPrint;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatInfoProvider.StringProvider;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIMgmt;
import org.smartrplace.util.frontend.servlet.ServletTimeseriesProvider;
import org.smartrplace.util.frontend.servlet.ServletTimeseriesProvider.SampledToJSonResult;

import de.iwes.util.timer.AbsoluteTimeHelper;

/** Basic version is just for reading, the derived class {@link ViaHeartbeatSchedulesWrite} below in this file is
 * also for receiving data.<br>
 * Usually only additional values are sent that are added to the schedule. If the clean flag is set in the JSON
 * transmitted then the entire schedule is deleted before new values are written. This is triggered by calling
 * {@link #resendAllOnNextOccasion()} on the source system. By default this occurs when the reference time is changed
 * or when a datapoint change notification is detected in getValues or setValue (more details to be provided). This is
 * NOT triggered on startup of a gateway, but doClean is set initially so the schedule schould be rewritten on the server
 * on the first data sending after restart of the gateway. We do not perform a resending after restart of the server,
 * but this sould not be necessary as all data that has not been sent to the server is sent then.<br>
 * TODO: A gap may arise if the HTTP connection is closed on the server before it is fully processed. But this gap
 * should be filled when the gateway is restarted.*/
public class ViaHeartbeatSchedules implements StringProvider {

	protected final ReadOnlyTimeSeries rot;
	
	/** On initial schedule sending we reset data on server*/
	protected boolean doClean = true;
	protected long lastValueSent = -1;
	protected long lastValueUpdateSent = -1;
	protected final Integer absoluteTiming;
	protected Double lastValueFloat = null;
	
	protected long lastClean = -1;
	protected final String alias;

	public ViaHeartbeatSchedules(ReadOnlyTimeSeries rot, String alias, Integer absoluteTiming) {
		this.rot = rot;
		this.alias = alias;
		this.absoluteTiming = absoluteTiming;
	}

	@Override
	public void received(String strValue, long now) {
		throw new IllegalStateException("Use ViaHeartbeatSchedulesWrite if you need to receive values!");
	}

	@Override
	public String getStringToSend(long now) {
		JSONObject json = new JSONObject();
		long newTime;
		if(absoluteTiming != null)
			newTime = AbsoluteTimeHelper.getIntervalStart(lastValueSent, absoluteTiming);
		else
			newTime = lastValueSent+1;
		List<SampledValue> vals = rot.getValues(newTime);
		if((absoluteTiming != null) && vals.size() == 1) {
			SampledValue sv = vals.get(0);
			if(sv.getTimestamp() == lastValueSent && (sv.getValue().getDoubleValue() == lastValueFloat))
				vals = Collections.emptyList();
		}
//if(rot instanceof ProcessedReadOnlyTimeSeries2 && ((ProcessedReadOnlyTimeSeries2)rot).getInputDp().id().startsWith("EnergyServerReadings_ESE/ESE_location_39")) //39/connection/energyDaily/reading"))
//System.out.println("vals#:"+vals.size()+" lastValueSent:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastValueSent));
		SampledToJSonResult mainRes = ServletTimeseriesProvider.smapledValuesToJson(vals, null, null, true, false, true, null, null, now+30*TimeProcUtil.DAY_MILLIS);
		if(!vals.isEmpty()) {
			SampledValue sv = vals.get(vals.size()-1);
			lastValueSent = sv.getTimestamp();
			lastValueUpdateSent = now;
			if(absoluteTiming != null)
				lastValueFloat = sv.getValue().getDoubleValue();
		}
		json.put("values", mainRes.arr);
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
		if(absoluteTiming != null)
			return  lastExisting.getTimestamp() >= AbsoluteTimeHelper.getIntervalStart(lastValueSent, absoluteTiming);
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
			super(sched, alias, null);
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
	
	/** Call this method on gateway for KPI datapoints with schedule transfer that are identified
	 * via alias 
	 * @param dp
	 * @param absoluteTiming
	 * @param alias
	 * @param label
	 * @return
	 */
	public static ViaHeartbeatSchedules registerDatapointForHeartbeatDp2ScheduleWithAlias(
			Datapoint dp, Integer absoluteTiming,
			String alias, String label,
			DatapointGroup dpGroup) {
		if(label != null)
			dp.setLabelDefault(label);
		dp.addAlias(alias);
		
		if(dpGroup != null)
			dpGroup.addDatapoint(dp);	
		return registerDatapointForHeartbeatDp2Schedule(dp, alias, absoluteTiming);
	}
	/** Call this method on the gateway for all datapoints for which schedule transfer shall be enabled
	 * from gateway to server. For special versions see:<br>
	 * - {@link #registerDatapointForHeartbeatDp2ScheduleWithAlias(Datapoint, Integer, String, String, DatapointGroup)}
	 * - {@link VirtualSensorKPIMgmt#registerEnergySumDatapoint(List, org.ogema.devicefinder.api.DatapointInfo.AggregationMode, org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtil, String, boolean, org.ogema.devicefinder.api.DatapointService)}
	 * - to register Virtual device sensors that are not identified via alias but by their full path call this directly
	 * @param dp
	 * @param absoluteTiming
	 * @return
	 */
	public static ViaHeartbeatSchedules registerDatapointForHeartbeatDp2Schedule(Datapoint dp, Integer absoluteTiming) {
		Set<String> als = dp.getAliases();
		return registerDatapointForHeartbeatDp2Schedule(dp, als.isEmpty()?null:als.iterator().next(), absoluteTiming);
	}
	public static ViaHeartbeatSchedules registerDatapointForHeartbeatDp2Schedule(Datapoint dp, String alias, Integer absoluteTiming) {
		ViaHeartbeatSchedules schedProv = new ViaHeartbeatSchedules(dp.getTimeSeries(), alias, absoluteTiming);
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
