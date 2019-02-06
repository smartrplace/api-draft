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
package de.smartrplace.app.heatcontrol.overview.config;

import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.model.prototypes.Data;

public interface HeatcontrolOverviewData extends Data {
	TemperatureResource comfortTemperature();
	TemperatureResource lowerTemperature();
	/** 0: Do not control manual mode of thermostat at all
	 *  1: Always set to manual mode
	 *  2: If non-manual mode detected switch back to manual and activate
	 *     comfort temperature for manual control duraration of thermostat control
	 *  3: like 2, but start boost when comfort temperature is activated
	 */
	IntegerResource controlManualMode(); 
}
