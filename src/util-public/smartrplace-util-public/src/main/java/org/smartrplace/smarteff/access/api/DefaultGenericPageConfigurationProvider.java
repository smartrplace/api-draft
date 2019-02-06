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

import java.util.Map;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public abstract class DefaultGenericPageConfigurationProvider implements GenericPageConfigurationProvider {

	public static final int MAX_ID = 100;

	protected abstract Map<String, ConfigInfoExt> configs();
	protected abstract String getNextId();

	/** Add this to implementation class*/
	/*public static DefaultGenericPageConfigurationProvider getInstance() {
		if(instance == null) instance = MyGenericPageConfigurationProvider();
		return instance;
	}*/
	
	public String addConfig(ConfigInfoExt sc) {
		String id = getNextId();
		configs().put(id, sc);
		return id;
	}

	//protected static volatile DefaultGenericPageConfigurationProvider instance = null;
	protected abstract DefaultGenericPageConfigurationProvider getInstanceObj();
	protected abstract void setInstance(DefaultGenericPageConfigurationProvider instance);
	
	/** Note that OSGi creates new instance some times when old instances are still
	 * on the system. So we make configs static, the static instance just reduces the number
	 * of objects around, but there will be several instances
	 */
	public DefaultGenericPageConfigurationProvider() {
		super();
		if(getInstanceObj() == null)
			setInstance(this);
	}
	
	@Override
	public ConfigInfoExt getSessionConfiguration(String configurationId) {
		if(configurationId == null) {
			if(configs().isEmpty()) return null;
			return configs().values().iterator().next();
		}
		return configs().get(configurationId);
	}
	
	protected static int getNextId(int currentId, int maxId) {
		currentId++;
		if(currentId > maxId) currentId = 1;
		return currentId;
	}

	@Override
	public String id() {
		return "DefaultPageConfiguration";
	}

	@Override
	public String label(OgemaLocale locale) {
		return id();
	}
	
	@Override
	public void saveCurrentConfiguration(ConfigInfoExt currentConfiguration, String configurationId) {
		//do nothing for now
	}

}
