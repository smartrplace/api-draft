package org.ogema.devicefinder.util;

import org.ogema.devicefinder.api.DPRoom;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class DPRoomImpl implements DPRoom {
	protected String location;
	protected String gatewayId = null;
	protected Room resource = null;
	protected String label;
	
	//The following values have setters
	protected Integer roomType = null;
	
	public DPRoomImpl(String location) {
		this.location = location;
		this.label = location;
	}
	public DPRoomImpl(String location, String label) {
		this.location = location;
		this.label = label;
	}
	public DPRoomImpl(Room room) {
		this.location = room.getLocation();
		this.label = ResourceUtils.getHumanReadableShortName(room);
		this.resource = room;
	}
	public DPRoomImpl(String location, String label, String gatewayId) {
		this(location, label);
		this.gatewayId = gatewayId;
	}

	@Override
	public void setRoomType(Integer roomType) {
		this.roomType = roomType;
	}
	
	@Override
	public String getLocation() {
		return location;
	}

	@Override
	public String getGatewayId() {
		return gatewayId;
	}

	@Override
	public Room getResource() {
		return resource;
	}

	@Override
	public void setResource(Room room) {
		this.resource = room;		
		this.location = room.getLocation();
		this.label = ResourceUtils.getHumanReadableShortName(room);
	}
	
	@Override
	public String id() {
		return location;
	}

	@Override
	public String label(OgemaLocale locale) {
		return label;
	}

	@Override
	public Integer getRoomType() {
		return roomType;
	}

	@Override
	public ElectricityConnectionBox getBuildingElectricityData() {
		throw new UnsupportedOperationException("not implemented yet!");
	}
}
