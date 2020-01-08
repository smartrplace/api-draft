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

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.model.prototypes.PhysicalElement;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.config.RoomSelectorDropdown;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.apps.hw.install.pattern.ThermostatPattern;
import org.smartrplace.apps.hw.install.patternlistener.ThermostatListener;

import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import org.smartrplace.apps.hw.install.patternlistener.DoorWindowSensorListener;
import org.smartrplace.apps.hw.install.pattern.DoorWindowSensorPattern;

// here the controller logic is implemented
public class HardwareInstallController {

	public OgemaLogger log;
    public ApplicationManager appMan;
    private ResourcePatternAccess advAcc;

	public HardwareInstallConfig appConfigData;
	
	public MainPage mainPage;
	WidgetApp widgetApp;

	public HardwareInstallController(ApplicationManager appMan, WidgetPage<?> page) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.advAcc = appMan.getResourcePatternAccess();
		
		initConfigurationResource();
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
		//TODO provide Util?
		String configResourceDefaultName = HardwareInstallConfig.class.getSimpleName().substring(0, 1).toLowerCase()+HardwareInstallConfig.class.getSimpleName().substring(1);
		final String name = appMan.getResourceManagement().getUniqueResourceName(configResourceDefaultName);
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
    	advAcc.addPatternDemand(ThermostatPattern.class, actionListener, AccessPriority.PRIO_LOWEST);
		advAcc.addPatternDemand(DoorWindowSensorPattern.class, doorWindowSensorListener, AccessPriority.PRIO_LOWEST); 
    }

	public void closeDemands() {
		if(!demandsActivated) return;
		demandsActivated = false;
		advAcc.removePatternDemand(ThermostatPattern.class, actionListener);
		advAcc.removePatternDemand(DoorWindowSensorPattern.class, doorWindowSensorListener);
    }
	public void close() {
		closeDemands();
	}
		
	/*
	 * if the app needs to consider dependencies between different pattern types,
	 * they can be processed here.
	 */
	public InstallAppDevice addDeviceIfNew(PhysicalElement device) {
		for(InstallAppDevice install: appConfigData.knownDevices().getAllElements()) {
			if(install.equalsLocation(device)) return install;
		}
		InstallAppDevice install = appConfigData.knownDevices().add();
		install.create();
		install.device().setAsReference(device);
		install.installationStatus().create();
		install.activate(true);
		return install;
	}
	public InstallAppDevice removeDevice(PhysicalElement device) {
		//TODO
		return null;
	}
}
