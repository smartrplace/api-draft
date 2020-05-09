package org.ogema.devicefinder.api;

import org.ogema.apps.roomsim.service.api.SingleRoomSimulation;
import org.ogema.apps.roomsim.service.api.util.RoomInsideSimulationBase;
import org.ogema.apps.roomsim.service.api.util.SingleRoomSimulationBase;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DeviceTableBase.InstalledAppsSelector;
import org.ogema.model.devices.buildingtechnology.AirConditioner;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.template.LabelledItem;

public interface DeviceHandlerProvider<T extends Resource> extends LabelledItem {
	Class<T> getResourceType();
	
	/** TODO: Check if this makes sense*/
	default OGEMADriverPropertyService<?> getDriverPropertyService() {return null;}
	
	PatternListenerExtended<ResourcePattern<T>, T> addPatternDemand(ResourcePatternAccess advAcc, InstalledAppsSelector app);
	void removePatternDemand(ResourcePatternAccess advAcc);
	
	DeviceTableBase getDeviceTable(WidgetPage<?> page, ApplicationManager appMan, Alert alert,
			InstalledAppsSelector appSelector);
	
	/** Provide simulation for a device
	 * 
	 * @param resource device resource
	 * @param roomSimulation roomSimulation if applicable. Note that this is not implemented yet. Note that
	 * 		this also means that the step method in the object returned is not called
	 * @return null if no simulation is available for the device
	 */
	default RoomInsideSimulationBase startSimulationForDevice(T resource,
			SingleRoomSimulationBase roomSimulation, ApplicationManager appMan) {
		return null;
	}
}
