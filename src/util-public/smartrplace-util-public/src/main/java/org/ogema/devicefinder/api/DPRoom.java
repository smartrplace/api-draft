package org.ogema.devicefinder.api;

import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.locations.Room;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Information for a room. Here we use a single interface for the information regarding a
 * certain room and general room description information. If just generel information shall be given
 * then the fields not relevant shall just be null.
 * The room name can be accessed with getLabel(null)*/
public interface DPRoom extends GatewayResource {
	public static final String BUILDING_OVERALL_ROOM_LABEL = "Building";
	
	@Override
	Room getResource();
	
	void setResource(Room room);
	
	Integer getRoomType();
	
	/**TODO: To be discussed if this would make sense, not implemented yet*/
	ElectricityConnectionBox getBuildingElectricityData();

	void setRoomType(Integer roomType);
	
	/** Should  usually only be used for rooms from remote gateways, local room names shall
	 * be determined from the resource
	 * @param label
	 * @param locale
	 */
	void setLabel(String label, OgemaLocale locale);
}
