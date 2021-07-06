package org.smartrplace.apps.heatcontrol.extensionapi.heatandcool;

import org.ogema.core.model.units.TemperatureResource;
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
}
