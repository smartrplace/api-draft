package org.smartrplace.apps.heatcontrol.extensionapi.heatandcool;

import org.ogema.core.model.units.TemperatureResource;
import org.ogema.model.devices.buildingtechnology.AirConditioner;
import org.ogema.model.devices.buildingtechnology.MechanicalFan;
import org.ogema.model.locations.Room;

/** This interface can be implemented by single devices and by room control units that control all heating and
 * cooling units within a room
 */
public interface TemperatureControlBase {

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
    public TemperatureResource  getTemperatureMeasurement();
    
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
	/** Set operation mode
	 * 
	 * @param state see {@link AirConditioner#operationMode()}
	 * @return
	 */
	default boolean setOperationMode(int state) {return false;}
	default Integer getOperationMode() {return null;}
	default Boolean getSwingMode() {return null;}
	default boolean setSwingMode(boolean newState) {return false;}
}
