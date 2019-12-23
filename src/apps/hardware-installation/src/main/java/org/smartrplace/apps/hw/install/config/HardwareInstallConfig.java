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
package org.smartrplace.apps.hw.install.config;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Data;

/** 
 * The global configuration resource type for this app.
 */
public interface HardwareInstallConfig extends Data {

	ResourceList<InstallAppDevice> knownDevices();
	
	/** If true then listeners are active. If false then listeners are not active for performance reasons*/
	BooleanResource isInstallationActive();
	
	/** Indication of the room selected that shall be displayed or special String indicating that
	 * devices from all rooms shall be displayed. See {@link RoomSelectorDropdown} for details. Note that
	 * we save this information persistently for all users/sessions as this is most efficient during installation in
	 * many cases.
	 */
	StringResource room();
}
