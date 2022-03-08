package org.smartrplace.util.virtualdevice;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.smartrplace.util.virtualdevice.SetpointControlManager.RouterInstance;

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
	
	//Check feedback
	volatile Float valueFeedbackPending = null;
	volatile Object valueFeedbackPendingObject = null;
	
	//CCU finding
	final SetpointControlManager<?> ctrl;

	public SensorData(SetpointControlManager<?> ctrl2) {
		this.ctrl = ctrl2;
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
}