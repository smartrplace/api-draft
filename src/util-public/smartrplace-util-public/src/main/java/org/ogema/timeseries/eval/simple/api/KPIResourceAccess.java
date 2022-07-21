package org.ogema.timeseries.eval.simple.api;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.DatapointInfo.UtilityType;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.gateway.EvalCollection;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;

import de.iwes.util.logconfig.EvalHelper;
import de.iwes.util.resource.ResourceHelper;

public class KPIResourceAccess {
	public static TemperatureResource getOpenWeatherMapTemperature(ResourceAccess resAcc) {
		Resource resIn = resAcc.getResource("OpenWeatherMapData/temperatureSensor/reading");
		if(resIn != null && (resIn instanceof TemperatureResource))
			return (TemperatureResource) resIn;
		return null;		
	}
	
	public static Room getOpenWeatherMapRoom(ResourceAccess resAcc) {
		Resource resIn = resAcc.getResource("OpenWeatherMapData");
		if(resIn != null && (resIn instanceof Room))
			return (Room) resIn;
		return null;		
	}

	public static SensorDevice getOrCreateOpenWeatherMapSensorDevice(ApplicationManager appMan) {
		ResourceAccess resAcc = appMan.getResourceAccess();
		Resource resIn = resAcc.getResource("WeatherData");
		if(resIn != null && (resIn instanceof SensorDevice))
			return (SensorDevice) resIn;
		Room openWRoom = getOpenWeatherMapRoom(resAcc);
		if(openWRoom == null)
			return null;
		SensorDevice openWSD = appMan.getResourceManagement().createResource("WeatherData", SensorDevice.class);
		openWSD.activate(true);
		return openWSD;
	}

	public static List<Room> getRealRooms(ResourceAccess resAcc) {
		if(Boolean.getBoolean("org.smartrplace.apps.heatcontrol.pattern.bacnet.roomcontrol")) {
			List<Room> result = new ArrayList<>();
			ResourceList<PhysicalElement> bacnetDevices = resAcc.getResource("BACnetDevices");
			for(PhysicalElement el: bacnetDevices.getAllElements()) {
				if(el.getName().startsWith("RoomControls_") && (el instanceof PhysicalElement)) {
					Room room = el.location().room();
					if(!room.exists())
						continue;
					result.add(room.getLocationResource());
				}
			}
			return result;
		}
		List<Room> result = resAcc.getResources(Room.class);
		List<Room> toRemove = new ArrayList<>();
		for(Room room: result) {
			if(room.getLocation().equals("OpenWeatherMapData"))
				toRemove.add(room);
		}
		result.removeAll(toRemove);
		return result;
	}
	
	public static Room getRealRoom(String humanReadableShortname, ResourceAccess resAcc) {
		List<Room> result = resAcc.getResources(Room.class);
		for(Room room: result) {
			if(ResourceUtils.getHumanReadableShortName(room).equals(humanReadableShortname))
				return room;
		}
		return null;
	}
	
	public static FloatResource getDefaultPriceResource(UtilityType type, ApplicationManager appMan) {
		switch(type) {
		case ELECTRICITY:
			FloatResource elPriceLoc;
			elPriceLoc = ResourceHelper.getSubResource(appMan.getResourceAccess().getResource("master"),
					"editableData/buildingData/E_0/electricityPrice", FloatResource.class);
			if(elPriceLoc == null) {
				EvalCollection evalCollection = EvalHelper.getEvalCollection(appMan);
				elPriceLoc = evalCollection.getSubResource("elPrice", FloatResource.class);
			}
			return elPriceLoc;
		case HEAT_ENERGY:
			FloatResource gasPriceLoc;
			gasPriceLoc = ResourceHelper.getSubResource(appMan.getResourceAccess().getResource("master"),
					"editableData/buildingData/E_0/gasPrice", FloatResource.class);
			if(gasPriceLoc == null) {
				EvalCollection evalCollection = EvalHelper.getEvalCollection(appMan);
				gasPriceLoc = evalCollection.getSubResource("gasPrice", FloatResource.class);
			}
			return gasPriceLoc;
		case ENERGY_MIXED: return null;
		case WATER:
			FloatResource waterPriceLoc;
			waterPriceLoc = ResourceHelper.getSubResource(appMan.getResourceAccess().getResource("master"),
					"editableData/buildingData/E_0/waterPrice", FloatResource.class);
			if(waterPriceLoc == null) {
				EvalCollection evalCollection = EvalHelper.getEvalCollection(appMan);
				waterPriceLoc = evalCollection.getSubResource("waterPrice", FloatResource.class);
			}
			return waterPriceLoc;
		case HEATING_DEGREE_DAYS: return null;
		case COOLING_DEGREE_DAYS: return null;
		case FOOD:
			FloatResource foodPriceLoc;
			foodPriceLoc = ResourceHelper.getSubResource(appMan.getResourceAccess().getResource("master"),
					"editableData/buildingData/E_0/foodPrice", FloatResource.class);
			if(foodPriceLoc == null) {
				EvalCollection evalCollection = EvalHelper.getEvalCollection(appMan);
				foodPriceLoc = evalCollection.getSubResource("foodPrice", FloatResource.class);
			}
			return foodPriceLoc;
		case UNKNOWN:
			return null;
		default: throw new IllegalStateException("Unknown type: "+type);
		}
	}

	public static FloatResource getDefaultEfficiencyResource(UtilityType type, ApplicationManager appMan) {
		switch(type) {
		case ELECTRICITY:
			return null;
		case HEAT_ENERGY:
			FloatResource gasEffLoc;
			gasEffLoc = ResourceHelper.getSubResource(appMan.getResourceAccess().getResource("master"),
					"editableData/buildingData/E_0/heatingEfficiency", FloatResource.class);
			if(gasEffLoc == null) {
				EvalCollection evalCollection = EvalHelper.getEvalCollection(appMan);
				gasEffLoc = evalCollection.getSubResource("gasEff", FloatResource.class);
			}
			return gasEffLoc;
		case ENERGY_MIXED: return null;
		case WATER:
			return null;
		case HEATING_DEGREE_DAYS: return null;
		case COOLING_DEGREE_DAYS: return null;
		case FOOD:
			return null;
		case UNKNOWN:
			return null;
		default: throw new IllegalStateException("Unknown type: "+type);
		}
	}

	public static FloatResource getDefaultYearlyConsumptionReferenceResource(UtilityType type, String roomId, ApplicationManager appMan) {
		Resource baseRes;
		if(roomId == null)
			baseRes = ResourceHelper.getSubResource(appMan.getResourceAccess().getResource("master"),
					"editableData/buildingData/E_0/buildingUnit/"+DPRoom.BUILDING_OVERALL_ROOM_LABEL);
		else {
			//TODO: In SmartrEfficiency the room resource name is E_0 etc. and the room has to be found via its name
			//For initial eval we generate the rooms like this
			baseRes = ResourceHelper.getSubResource(appMan.getResourceAccess().getResource("master"),
					"editableData/buildingData/E_0/buildingUnit/"+ResourceUtils.getValidResourceName(roomId)+"/");			
		}
		if(baseRes == null)
			return null;
		return ResourceHelper.getSubResource(baseRes, ResourceUtils.getValidResourceName(
				"yearlyConsumptionReference_"+type.name()), FloatResource.class);
	}
	
}
