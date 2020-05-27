package org.ogema.devicefinder.api;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public interface InstalledAppsSelector {
	List<InstallAppDevice> getDevicesSelected();

	<T extends Resource> InstallAppDevice addDeviceIfNew(T model, DeviceHandlerProvider<T> tableProvider);
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
	public <T extends Resource> void startSimulation(DeviceHandlerProvider<T> tableProvider, T device);
	//ApplicationManager getAppManForSimulationStart();
}
