package org.smartrplace.apps.heatcontrol.extensionapi.heatandcool;

import java.util.Collection;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.schedule.AbsoluteSchedule;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.model.devices.buildingtechnology.AirConditioner;
import org.ogema.model.devices.buildingtechnology.MechanicalFan;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.TemperatureSensor;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** This interface can be implemented by single devices and by room control units that control all heating and
 * cooling units within a room
 */
public interface TemperatureControlBase extends RoomDeviceProvider {

    /** Get temperature control setpoint resource ("setpoint set")
     * 
     * @return null if no suitable device is available in the room
     */
    public TemperatureResource getTemperatureSetpoint();
    
    public void setTemperatureSetpoint(float t);
    /** Write to setpoint even if this may cause a network traffic overflow. This can be used e.g. to switch thermostats
     * to summer mode. In this case settings may be sent to devices later, but need to be visible right away*/
    public void setTemperatureSetpointForced(float t);

    /** Get temperature setpoint feedback resource
     * 
     * @return null if no suitable device is available in the room
     */
    public TemperatureResource getTempSetpointFeedback();

    /** 
     * 
     * @return null if no suitable device is available in the room
     */
    public Float getTempSetpointFeedbackValue();

    /** 
     * 
     * @return null if no suitable device is available in the room
     */
    public Float getTempSetpointSetValue();

    /** 
     * 
     * @return null if no suitable device is available in the room
     */
    public TemperatureSensor getTemperatureMeasurement();
    
    /** 
     * 
     * @return null if no suitable device is available in the room
     */
    public Float getTemperatureMeasurementValue();
    
    public void setState(boolean on);
    public boolean isOn();

    public Room getRoom();
    //public ResourcePattern getPattern();
    
    /** Get number off speed states the device supports. According to
     * resource {@link AirConditioner#fan()} and {@link MechanicalFan#setting()} this can usually be null, 2 or 3.<br>
     * 4 states may be given if auto shall be supported (currently not implemented)
     *  
     * @return null if no fan speed is supported
     */
	default Integer fanSpeedStateNum() {return null;}
	
	/** Set fan speed. See {@link MechanicalFan#setting()} for details.
	 * 
	 * @param state TODO: Specifiy indexing for device => can driver do this?
	 * @return true if setting the fan speed was succesful
	 */
	default boolean setFanSpeed(int state) {return false;}
	default Integer getFanSpeed() {return null;}
	
	default String[] fanSpeedLabels(OgemaLocale locale) { return null;}
	/*default String[] fanSpeedLabels(OgemaLocale locale) {
		if(locale == OgemaLocale.GERMAN)
			return new String[]{"Auto", "Low", "Medium", "High"};
		else
			return new String[]{"Auto", "Niedrig", "Medium", "Hoch"};
	}*/
	
	/** Set operation mode
	 * 
	 * @param state see {@link AirConditioner#operationMode()}
	 * @return
	 */
	default boolean setOperationMode(int state) {return false;}
	default Integer getOperationMode() {return null;}
	default Boolean getSwingMode() {return null;}
	default boolean setSwingMode(boolean newState) {return false;}
	default public void setAutoSetpoints(boolean useAutoMode, boolean force) {};
	
	/** This mode must be provided for heating and cooling*/
	public enum HeatCoolFanMode {
		WITHOUT_FAN,
		FULL_WITH_FAN,
		UNSUPPORTED,
		/** The respective season (heating/cooling) is not supported, but fan control
		 * is still available during the season.
		 */
		FAN_ONLY,
		/** Not supported, but configuration shall be offered*/
		CONFIGURARATION_ONLY
	}
	default HeatCoolFanMode isHeatingSupported() {
		return HeatCoolFanMode.UNSUPPORTED;
	}
	default HeatCoolFanMode isCoolingSupported() {
		return HeatCoolFanMode.UNSUPPORTED;
	}
	
	/** Use the application specific methods. They may be overwritten with more
	 * specific implmentations
	 * @param isHeating
	 * @return
	 */
	default boolean isFanSupported(boolean isHeating) {
		HeatCoolFanMode mode;
		if(isHeating)
			mode = isHeatingSupported();
		else
			mode = isCoolingSupported();
		return mode == HeatCoolFanMode.FULL_WITH_FAN ||
				mode == HeatCoolFanMode.FAN_ONLY;
	}

	default boolean isTemperatureControlSupported(boolean isHeating) {
		HeatCoolFanMode mode;
		if(isHeating)
			mode = isHeatingSupported();
		else
			mode = isCoolingSupported();
		return mode == HeatCoolFanMode.FULL_WITH_FAN ||
				mode == HeatCoolFanMode.WITHOUT_FAN;
	}

	default boolean isConfigurationSupported(boolean isHeating) {
		HeatCoolFanMode mode;
		if(isHeating)
			mode = isHeatingSupported();
		else
			mode = isCoolingSupported();
		return mode != HeatCoolFanMode.UNSUPPORTED;
	}
	
	default void setBackupSetpointSchedule(Collection<SampledValue> vals) {
		TemperatureResource setp = getTemperatureSetpoint();
		AbsoluteSchedule program = setp.program();
		if(!program.exists()) {
			program.create();
			program.replaceValues(0, Long.MAX_VALUE, vals);
			program.activate(false);
		} else
			program.replaceValues(0, Long.MAX_VALUE, vals);
	}
	
	/** Provide Virtual Switches by devices*/
	public static class SmartPlugData {
		public boolean state;
		public boolean online;
		public String label;
		public int stateNum = 2;
		public int multiState;
		public float power;
		public String powerTsId = "";
		
		/** Override this if Controller shall use this*/
		public void setState(int newState) {};
	}
}
