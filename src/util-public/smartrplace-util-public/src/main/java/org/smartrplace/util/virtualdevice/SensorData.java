package org.smartrplace.util.virtualdevice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.util.virtualdevice.HmSetpCtrlManager.WritePrioLevel;
import org.smartrplace.util.virtualdevice.SetpointControlManager.RouterInstance;

import de.iwes.util.resource.ValueResourceHelper;

public abstract class SensorData {
	abstract Resource sensor();
	abstract ValueResource setpoint();
	/** Not required currently*/
	abstract ValueResource feedback();
	abstract RouterInstance ccu();
	/** If not required may be null */
	abstract ResourceValueListener<?> setpointListener();
	abstract void stop();
	abstract void writeSetpoint(float value);
	/** Overwrite instead of writeSetpoint (or in addition to it) if required*/
	void writeSetpointData(Object setpointData) {
		throw new IllegalArgumentException("writeSetpointData for Object input not supported by default!");
	}
	
	//Resend data
	long lastSent = 1;
	//final SensorData sensor;
	Float valuePending;
	Object valuePendingObject;
	long valuePendingSince;
	
	/** Resend if no first sending was successful, but no fitting feedback is available*/
	long pendingTimeForMissingFeedback;
	
	//long valuePendingDueToOverloadSince = -1;
	float maxDC;
	
	//Check feedback
	volatile Float valueFeedbackPending = null;
	volatile Object valueFeedbackPendingObject = null;
	
	//CCU finding
	final SetpointControlManager<?> ctrl;

	public SensorData(SetpointControlManager<?> ctrl2) {
		this.ctrl = ctrl2;
		this.pendingTimeForMissingFeedback = ctrl2.pendingTimeForMissingFeedbackDefault;
		ctrl.appMan.getLogger().debug("  CREATING SensorData for: "+ctrl.getClass().getSimpleName());
	}
	
	/** Notify that setpoint control has changed*/
	protected void reportSetpoint(float value) {
		addKnownValue(value);
	}
	protected void reportFbConfirmed(float feedbackValue) {
		reportFbConfirmed(feedbackValue, true);
	}
	protected void reportFbConfirmed(float feedbackValue, boolean addToKnownValues) {
		lastSetpointFeedbackConfirmTime = ctrl.appMan.getFrameworkTime();
		if(addToKnownValues)
			addKnownValue(feedbackValue);
	}
	
	public static String getStatus(SensorData sd) {
		if(sd == null)
			return "--";
		if(sd.valuePending != null)
			return "Ct";
		if(sd.valueFeedbackPending != null)
			return "Fb";
		return "ok";
	}
	
	public static class KnownValue2 {
		
		public final long time;
		public final float value;
		
		public KnownValue2(float value, long time) {
			this.time = time;
			this.value = value;
		}
		
	}
	private volatile boolean receivedFirstFBValue = false;
	//public List<KnownValue2> knownValues = new ArrayList<>();
	public Map<Float, Long> knownValues = new HashMap<>();
	public long lastSetpointFeedbackConfirmTime = -1;
	public boolean isSetpointKnown(float fbReceived) {
		if(!(setpoint() instanceof FloatResource))
			return false;
		FloatResource temperatureSetpoint = (FloatResource) setpoint();
		FloatResource tempSetpointFeedbackValue = (FloatResource) feedback();
		synchronized (this) {
			cleanKnownValues();
			if(!receivedFirstFBValue) {
				addKnownValue(tempSetpointFeedbackValue);
				receivedFirstFBValue = true;
				ctrl.appMan.getLogger().warn("Ignored due to receivedFirstFBValue RemoteVal:"+(fbReceived-273.15)+" / "+(tempSetpointFeedbackValue.getValue()-273.15));
				return true;
			}
			// A setpoint requested should be provided by requestSetpointWrite
			// The setpoint may be rather old if not updated due to auto-curve etc., so we should not check for the setpoint value
			//if(ValueResourceHelper.isAlmostEqual(temperatureSetpoint.getValue(), fbReceived))
			//	return true;
			for(Float val: knownValues.keySet()) {
				if(ValueResourceHelper.isAlmostEqual(val, fbReceived)) {
					return true;
				}
			}			
		}
		return false;
	}
	
	public void cleanKnownValues() {
		long t0 = lastSetpointFeedbackConfirmTime-ctrl.knownSetpointValueOmitDuration(this); //ctrl.appMan.getFrameworkTime();
		final long agoMax = ctrl.appMan.getFrameworkTime() - TimeProcUtil.HOUR_MILLIS;
		if(t0 < agoMax)
			t0 = agoMax;
		//List<KnownValue2> remove = new ArrayList<>();
		List<Float> remove = new ArrayList<>();
		for (Entry<Float, Long> val: knownValues.entrySet()) {
			if (val.getValue() < t0) {
				remove.add(val.getKey());
			}
		}
		for(Float rem: remove)
			knownValues.remove(rem); //.removeAll(remove);
	}

	public void addKnownValue(FloatResource tres) {
		if(tres.isActive()) {
			addKnownValue(tres.getValue());
		}
	}
	public void addKnownValue(float value) {
		knownValues.put(value, ctrl.appMan.getFrameworkTime());
		//knownValues.add(new KnownValue2(value, ctrl.appMan.getFrameworkTime()));
	}

}