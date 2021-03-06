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
package org.smartrplace.smarteff.access.api;

import de.iwes.widgets.template.LabelledItem;

public interface GenericPageConfigurationProvider extends LabelledItem {
	public final static String SMARTREFF_URL_BASEPATH = "/org/sp/smarteff/";

	/**The id of the configurationProvider must be provided as parameter
	 * configProvider with URL
	 */
	String id();
	
	/** Get configuration for specific session
	 * 
	 * @param configurationId paramter configurationId provided with URL. If the parameter is null
	 * 		a standard session configuration for the provider may be returned, but in this case
	 * 		also just null be returned, but no exception should be thrown.
	 * @return
	 */
	ConfigInfoExt getSessionConfiguration(String configurationId);
	
	/** Current selections/configurations sent to the provider. The provider can
	 * save these configurations fully or partially or just ignore this information. The
	 * message is generated by the ScheduleViewer when a "Save Configuration" button is pressed.
	 */
	void saveCurrentConfiguration(ConfigInfoExt currentConfiguration, String configurationId);

}
