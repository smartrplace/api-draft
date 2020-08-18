package org.ogema.devicefinder.util;

import java.util.HashMap;
import java.util.Map;

import org.ogema.devicefinder.api.DPRoom;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class DPRoomImpl implements DPRoom {
	protected String location;
	protected String gatewayId = null;
	protected Room resource = null;
	protected Map<OgemaLocale, String> labels = new HashMap<>();
	
	//The following values have setters
	protected Integer roomType = null;
	
	public DPRoomImpl(String location) {
		this.location = location;
		this.labels.put(OgemaLocale.ENGLISH, location);
	}
	public DPRoomImpl(String location, String label) {
		this.location = location;
		this.labels.put(OgemaLocale.ENGLISH, label);
	}
	public DPRoomImpl(Room room) {
		this.location = room.getLocation();
		this.labels.put(OgemaLocale.ENGLISH, ResourceUtils.getHumanReadableShortName(room));
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
		if(resource != null && (!resource.equalsLocation(room)))
			labels.clear();
		this.resource = room;		
		this.labels.put(OgemaLocale.ENGLISH, ResourceUtils.getHumanReadableShortName(room));
		this.location = room.getLocation();
		this.roomType = room.type().getValue();
	}
	
	@Override
	public String id() {
		return location;
	}

	@Override
	public String label(OgemaLocale locale) {
		if(locale == null)
			return labels.get(OgemaLocale.ENGLISH);
		String result = labels.get(locale);
		if(result != null || locale == OgemaLocale.ENGLISH)
			return result;
		return labels.get(OgemaLocale.ENGLISH);
	}

	@Override
	public Integer getRoomType() {
		return roomType;
	}

	@Override
	public ElectricityConnectionBox getBuildingElectricityData() {
		throw new UnsupportedOperationException("not implemented yet!");
	}
	@Override
	public void setLabel(String label, OgemaLocale locale) {
		labels.put(locale, label);
	}
}
