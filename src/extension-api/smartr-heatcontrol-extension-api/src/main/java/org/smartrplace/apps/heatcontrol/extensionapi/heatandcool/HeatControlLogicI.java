package org.smartrplace.apps.heatcontrol.extensionapi.heatandcool;

public interface HeatControlLogicI {
	public float getNextNoPresenceIntervalSetpoint();
	public long getNextNoPresenceIntervalStart();
	public boolean isValueFittingRemoteAuto(float t, TemperatureControlDev tp);
}
