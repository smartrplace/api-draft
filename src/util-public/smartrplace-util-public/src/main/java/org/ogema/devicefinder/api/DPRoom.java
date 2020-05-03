package org.ogema.devicefinder.api;

import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.locations.Room;

/** Information for a room. Here we use a single interface for the information regarding a
 * certain room and general room description information. If just generel information shall be given
 * then the fields not relevant shall just be null.
 * The room name can be accessed with getLabel(null)*/
public interface DPRoom extends GatewayResource {
	
	@Override
	Room getResource();
	
	Integer getRoomType();
	
	/**TODO: To be discussed if this would make sense, not implemented yet*/
	ElectricityConnectionBox getBuildingElectricityData();
}
