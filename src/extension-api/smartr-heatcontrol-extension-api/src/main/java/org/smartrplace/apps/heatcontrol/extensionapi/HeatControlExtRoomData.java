package org.smartrplace.apps.heatcontrol.extensionapi;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.DoorWindowSensor;
import org.ogema.model.sensors.HumiditySensor;
import org.ogema.model.sensors.TemperatureSensor;

public abstract class HeatControlExtRoomData {
	/** Get information whether manual control of setpoint temperature
	 * is active and if yes for how long
	 * @return remaining manual control setting time or negative value if
	 * no manual control is active
	 */
	public abstract long getRemainingDirectThermostatManualDuration();
	/** Get current temperature setpoint in K*/
	public abstract float getCurrentTemperatureSetpoint();
	/**
	 * 
	 * @param setpoint manuel setpoint temperature in K
	 * @param duration duration in ms for which the setting shall be active
	 */
	public abstract void setManualTemperatureSetpoint(float setpoint, long duration);
	
	/** Motion sensor data is not directly processed by the heat control app, so we just
	 * pass the value that is used internally based on information of the Smartrplace
	 * presence detection app.
	 */
	public abstract boolean isUserPresent();
	
	/** The heating core application shall take care of additional configuration
	 * resources for each room for other applications in order to make the management
	 * of room resources more efficient
	 * 
	 * @param forceCreation if true the subresource will be created if not available yet
	 * if false the method will return false if the resource is not yet available
	 */
	public abstract <R extends Resource> R getRoomExtensionData(boolean forceCreation,
			Class<R> resourceType);

	final private Room room;
	final private List<Thermostat> thermostats;
	/** Only temperature sensors that are not part of thermostats*/
	final private List<TemperatureSensor> roomTemperatureSensors;
	final private List<HumiditySensor> roomHumiditySensors;
	final private List<DoorWindowSensor> windowSensors;
	public HeatControlExtRoomData(Room room, List<Thermostat> thermostats,
			List<TemperatureSensor> roomTemperatureSensors, List<HumiditySensor> roomHumiditySensors,
			List<DoorWindowSensor> windowSensors) {
		this.room = room;
		this.thermostats = thermostats;
		this.roomTemperatureSensors = roomTemperatureSensors;
		this.roomHumiditySensors = roomHumiditySensors;
		this.windowSensors = windowSensors;
	}
	
	public Room getRoom() {
		return room;
	}

	public List<Thermostat> getThermostats() {
		return thermostats;
	}

	public List<TemperatureSensor> getRoomTemperatureSensors() {
		return roomTemperatureSensors;
	}

	public List<HumiditySensor> getRoomHumiditySensors() {
		return roomHumiditySensors;
	}
	public List<DoorWindowSensor> getWindowSensors() {
		return windowSensors;
	}

}
