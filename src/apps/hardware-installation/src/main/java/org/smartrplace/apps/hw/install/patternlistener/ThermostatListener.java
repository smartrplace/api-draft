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
package org.smartrplace.apps.hw.install.patternlistener;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.resourcemanager.pattern.PatternListener;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.pattern.ThermostatPattern;

/**
 * A pattern listener for the TemplatePattern. It is informed by the framework 
 * about new pattern matches and patterns that no longer match.
 */
public class ThermostatListener implements PatternListener<ThermostatPattern> {
	
	private final HardwareInstallController app;
	public final List<ThermostatPattern> availablePatterns = new ArrayList<>();
	
 	public ThermostatListener(HardwareInstallController templateProcess) {
		this.app = templateProcess;
	}
	
	@Override
	public void patternAvailable(ThermostatPattern pattern) {
		availablePatterns.add(pattern);
		
		app.addDeviceIfNew(pattern.model, null);
	}
	@Override
	public void patternUnavailable(ThermostatPattern pattern) {
		app.removeDevice(pattern.model);
		availablePatterns.remove(pattern);
	}
	
	
}
