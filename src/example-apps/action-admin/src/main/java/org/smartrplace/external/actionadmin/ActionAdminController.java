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
package org.smartrplace.external.actionadmin;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.smartrplace.external.actionadmin.config.ActionAdminConfig;
import org.smartrplace.external.actionadmin.gui.MainPage;
import org.smartrplace.external.actionadmin.pattern.ActionPattern;
import org.smartrplace.external.actionadmin.patternlistener.ActionListener;

import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;

// here the controller logic is implemented
public class ActionAdminController {

	public OgemaLogger log;
    public ApplicationManager appMan;
    private ResourcePatternAccess advAcc;

	public ActionAdminConfig appConfigData;
	
	public MainPage mainPage;
	WidgetApp widgetApp;

	public ActionAdminController(ApplicationManager appMan, WidgetPage<?> page) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.advAcc = appMan.getResourcePatternAccess();
		
		mainPage = new MainPage(page, appMan);

		initConfigurationResource();
        initDemands();
	}

	public ActionListener actionListener;

    /*
     * This app uses a central configuration resource, which is accessed here
     */
    private void initConfigurationResource() {
		//TODO provide Util?
		String configResourceDefaultName = ActionAdminConfig.class.getSimpleName().substring(0, 1).toLowerCase()+ActionAdminConfig.class.getSimpleName().substring(1);
		final String name = appMan.getResourceManagement().getUniqueResourceName(configResourceDefaultName);
		appConfigData = appMan.getResourceAccess().getResource(name);
		if (appConfigData != null) { // resource already exists (appears in case of non-clean start)
			appMan.getLogger().debug("{} started with previously-existing config resource", getClass().getName());
		}
		else {
			appConfigData = (ActionAdminConfig) appMan.getResourceManagement().createResource(name, ActionAdminConfig.class);
			appConfigData.name().create();
			//TODO provide different sample, provide documentation in code
			appConfigData.name().setValue("sampleName");
			appConfigData.activate(true);
			appMan.getLogger().debug("{} started with new config resource", getClass().getName());
		}
    }
    
    /*
     * register ResourcePatternDemands. The listeners will be informed about new and disappearing
     * patterns in the OGEMA resource tree
     */
    public void initDemands() {
		actionListener = new ActionListener(this);
		advAcc.addPatternDemand(ActionPattern.class, actionListener, AccessPriority.PRIO_LOWEST);
    }

	public void close() {
		advAcc.removePatternDemand(ActionPattern.class, actionListener);
    }

	/*
	 * if the app needs to consider dependencies between different pattern types,
	 * they can be processed here.
	 */
	public void processInterdependies() {
		// TODO Auto-generated method stub
		
	}
}
