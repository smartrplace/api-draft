/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.apps.hw.install;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.tools.resource.util.LoggingUtils;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.apps.hw.install.gui.RoomSelectorDropdown;
import org.smartrplace.apps.hw.install.pattern.DoorWindowSensorPattern;
import org.smartrplace.apps.hw.install.pattern.ThermostatPattern;
import org.smartrplace.apps.hw.install.patternlistener.DoorWindowSensorListener;
import org.smartrplace.apps.hw.install.patternlistener.ThermostatListener;

import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;

// here the controller logic is implemented
public class HardwareInstallController {

	public final OgemaLogger log;
    public final ApplicationManager appMan;
    private final ResourcePatternAccess advAcc;
    public final DatapointService dpService;

	public HardwareInstallConfig appConfigData;
	public final HardwareInstallApp hwInstApp;
	
	public MainPage mainPage;
	WidgetApp widgetApp;

	public HardwareInstallController(ApplicationManager appMan, WidgetPage<?> page, HardwareInstallApp hardwareInstallApp,
			DatapointService dpService) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.advAcc = appMan.getResourcePatternAccess();
		this.hwInstApp = hardwareInstallApp;		
		this.dpService = dpService;
		
		initConfigurationResource();
		cleanupOnStart();
        initDemands();
		mainPage = getMainPage(page);
	}

	protected MainPage getMainPage(WidgetPage<?> page) {
		return new MainPage(page, this);
	}
	
	public ThermostatListener actionListener;
	public DoorWindowSensorListener doorWindowSensorListener;

    /*
     * This app uses a central configuration resource, which is accessed here
     */
    private void initConfigurationResource() {
		String name = HardwareInstallConfig.class.getSimpleName().substring(0, 1).toLowerCase()+HardwareInstallConfig.class.getSimpleName().substring(1);
		appConfigData = appMan.getResourceAccess().getResource(name);
		if (appConfigData != null) { // resource already exists (appears in case of non-clean start)
			appMan.getLogger().debug("{} started with previously-existing config resource", getClass().getName());
		}
		else {
			appConfigData = (HardwareInstallConfig) appMan.getResourceManagement().createResource(name, HardwareInstallConfig.class);
			appConfigData.isInstallationActive().create();
			appConfigData.knownDevices().create();
			appConfigData.room().create();
			appConfigData.room().setValue(RoomSelectorDropdown.ALL_DEVICES_ID);
			//appConfigData.installationStatusFilter().create();
			//appConfigData.installationStatusFilter().setValue(InstallationStatusFilterDropdown.FILTERS.ALL.name);
			appConfigData.activate(true);
			appMan.getLogger().debug("{} started with new config resource", getClass().getName());
		}
    }
    
    /*
     * register ResourcePatternDemands. The listeners will be informed about new and disappearing
     * patterns in the OGEMA resource tree
     */
    public void initDemands() {
		actionListener = new ThermostatListener(this);
		doorWindowSensorListener = new DoorWindowSensorListener(this);
		if(appConfigData.isInstallationActive().getValue()) {
			startDemands();
		}
    }
    public void checkDemands() {
    	if(appConfigData.isInstallationActive().getValue())
     		startDemands();
    	else
    		closeDemands();
    }
    
    public boolean demandsActivated = false;
    public void startDemands() {
		demandsActivated = true;
    	advAcc.addPatternDemand(ThermostatPattern.class, actionListener, AccessPriority.PRIO_LOWEST);
		advAcc.addPatternDemand(DoorWindowSensorPattern.class, doorWindowSensorListener, AccessPriority.PRIO_LOWEST);
		if(hwInstApp != null) for(DeviceHandlerProvider<?> devhand: hwInstApp.getTableProviders().values()) {
			devhand.addPatternDemand(mainPage);
		}
    }

	public void closeDemands() {
		if(!demandsActivated) return;
		demandsActivated = false;
		advAcc.removePatternDemand(ThermostatPattern.class, actionListener);
		advAcc.removePatternDemand(DoorWindowSensorPattern.class, doorWindowSensorListener);
		if(hwInstApp != null) for(DeviceHandlerProvider<?> devhand: hwInstApp.getTableProviders().values()) {
			devhand.removePatternDemand();
		}
    }
	public void close() {
		closeDemands();
	}
		
	/*
	 * if the app needs to consider dependencies between different pattern types,
	 * they can be processed here.
	 */
	public <T extends Resource> InstallAppDevice addDeviceIfNew(T device, DeviceHandlerProvider<T> tableProvider) {
		for(InstallAppDevice install: appConfigData.knownDevices().getAllElements()) {
			if(install.device().equalsLocation(device)) return install;
		}
		InstallAppDevice install = appConfigData.knownDevices().add();
		install.create();
		install.device().setAsReference(device);
		install.installationStatus().create();
		install.activate(true);
		if(tableProvider != null)
			startSimulation(tableProvider, device);
		return install;
	}
	public InstallAppDevice removeDevice(Resource device) {
		//TODO
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Resource> void startSimulations(DeviceHandlerProvider<T> tableProvider) {
		Class<?> tableType = tableProvider.getResourceType();
		for(InstallAppDevice install: appConfigData.knownDevices().getAllElements()) {
			if(install.device().getResourceType().equals(tableType)) {
				startSimulation(tableProvider, (T)install.device());
			}
		}
	}
	protected Map<String, Set<String>> simulationsStarted = new HashMap<>();
	@SuppressWarnings("unchecked")
	public <T extends Resource> void startSimulation(DeviceHandlerProvider<T> tableProvider, T device) {
		if(!Boolean.getBoolean("org.ogema.sim.simulateRemoteGateway"))
			return;
		Set<String> deviceSimsStarted = simulationsStarted.get(tableProvider.id());
		if(deviceSimsStarted == null) {
			deviceSimsStarted = new HashSet<>();
			simulationsStarted.put(tableProvider.id(), deviceSimsStarted);
		}
		if(deviceSimsStarted.contains(device.getLocation()))
			return;
		deviceSimsStarted.add(device.getLocation());
		tableProvider.startSimulationForDevice((T) device.getLocationResource(),
				mainPage.getRoomSimulation(device), dpService);
		if(Boolean.getBoolean("org.smartrplace.apps.hw.install.autologging")) {
			for(Datapoint dp: tableProvider.getDatapoints(device, dpService)) {
				if(!(dp instanceof SingleValueResource))
					continue;
				if(tableProvider.relevantForDefaultLogging(dp)) {
					//TODO: activate logging
					if(Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway")) {
						//TODO: Activate also log transfer
						//startTransmitLogData(dp.getDeviceResource());
						LoggingUtils.activateLogging((SingleValueResource) dp.getDeviceResource(), -2);
					} else
						LoggingUtils.activateLogging((SingleValueResource) dp.getDeviceResource(), -2);
				}
			}
		}
	}
	
	/*private void startTransmitLogData(SingleValueResource resource) {
		DataLogTransferInfo log = null;
		for(DataLogTransferInfo dl : dataLogs.getAllElements()) {
			if(dl.clientLocation().getValue().equals(resource.getPath()))
				log = dl;
		}

		if(log == null) log = dataLogs.add();
		
		StringResource clientLocation = log.clientLocation().create();
		clientLocation.setValue(resource.getPath());
	
		TimeIntervalLength tLength = log.transferInterval().timeIntervalLength().create();
		IntegerResource type = tLength.type().create();
		type.setValue(10);
		log.activate(true);
	}*/

	public void cleanupOnStart() {
		List<String> knownDevLocs = new ArrayList<>();
		for(InstallAppDevice install: appConfigData.knownDevices().getAllElements()) {
			if(knownDevLocs.contains(install.device().getLocation())) {
				install.delete();
			} else
				knownDevLocs.add(install.device().getLocation());
		}
		
	}
}
