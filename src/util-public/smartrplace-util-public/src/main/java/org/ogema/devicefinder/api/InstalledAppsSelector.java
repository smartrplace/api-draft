package org.ogema.devicefinder.api;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public interface InstalledAppsSelector {
	List<InstallAppDevice> getDevicesSelected(DeviceHandlerProvider<?> devHand, OgemaHttpRequest req);
	
	InstallAppDevice getInstallResource(Resource device);

	<T extends PhysicalElement> InstallAppDevice addDeviceIfNew(T model, DeviceHandlerProvider<T> tableProvider);
	<T extends Resource> InstallAppDevice removeDevice(T model);
	
	default  <T extends Resource> SingleRoomSimulationBase getRoomSimulation(T model) {
		return null;
	}
	
	/** Called whenever new device connections are detected, may be called even when the device is
	 * already known, so the implementation shall be make sure that simulations are only started once
	 * @param <T>
	 * @param tableProvider
	 * @param device
	 */
	public <T extends PhysicalElement> void startSimulation(DeviceHandlerProvider<T> tableProvider, T device);
	//ApplicationManager getAppManForSimulationStart();
	
	void addWidgetsExpert(DeviceHandlerProvider<?> tableProvider, InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan);
	
	default boolean sortByRoom() {
		return false;
	}
}
