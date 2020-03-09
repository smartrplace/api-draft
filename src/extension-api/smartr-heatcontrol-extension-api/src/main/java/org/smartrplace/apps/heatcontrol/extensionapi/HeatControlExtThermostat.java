package org.smartrplace.apps.heatcontrol.extensionapi;

/** Currently this interface is not accessible for apps that have not direct dependency to the heat control logic. The interface
 * may be made accessible for real extension points in the future. Currently it is mainly used as internal interface of
 * the heat control logic.
 */
public interface HeatControlExtThermostat {
	public static class CheckManualResult {
		/** New manual setpoint detected*/
		public float newSetpoint;
		/** time until new manual setpoint shall be valid*/
		public long endTime = -1;
	}

	/** Check if a manual setting at the thermostat is detected
	 * 
	 * @param tp
	 * @return null if no manual setting is detected or the manual setpoint temperature
	 */
	CheckManualResult checkForManualSetting(final ThermostatPattern tp);
}
