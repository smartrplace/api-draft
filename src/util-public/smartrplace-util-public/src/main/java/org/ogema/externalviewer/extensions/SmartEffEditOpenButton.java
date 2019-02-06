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
package org.ogema.externalviewer.extensions;

import java.util.Arrays;

import org.ogema.core.model.Resource;
import org.smartrplace.smarteff.access.api.ConfigContextExternal;
import org.smartrplace.smarteff.access.api.ConfigInfoExt;
import org.smartrplace.smarteff.access.api.DefaultGenericPageConfigurationProvider;
import org.smartrplace.smarteff.access.api.GenericPageConfigurationProvider;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;

public class SmartEffEditOpenButton extends RedirectButton {
	private static final long serialVersionUID = 1L;

	protected final String providerId;
	protected final String pageURL;
	//protected Map<String, SessionConfiguration> configs = new HashMap<>();
	//protected static int lastConfig = 0;
	protected final DefaultGenericPageConfigurationProvider schedConfigProv;


	public SmartEffEditOpenButton(WidgetPage<?> page, String id, String text,
			String pageURL,
			String providerId, DefaultGenericPageConfigurationProvider schedConfigProv) {
		this(page, id, text, pageURL, providerId, schedConfigProv, null);
	}
	public SmartEffEditOpenButton(WidgetPage<?> page, String id, String text,
			String pageURL,
			String providerId, DefaultGenericPageConfigurationProvider schedConfigProv,
			Resource fixedResource) {
		super(page, id, text);
		this.pageURL = pageURL;
		this.providerId = providerId;
		this.schedConfigProv = schedConfigProv;
		if(fixedResource != null) addConfig(fixedResource);
	}
	public SmartEffEditOpenButton(OgemaWidget parent, String id, String text,
			String pageURL,
			String providerId, DefaultGenericPageConfigurationProvider schedConfigProv,
			OgemaHttpRequest req) {
		this(parent, id, text, pageURL, providerId, schedConfigProv, null, req);
	}
	public SmartEffEditOpenButton(OgemaWidget parent, String id, String text,
			String pageURL,
			String providerId, DefaultGenericPageConfigurationProvider schedConfigProv,
			Resource fixedResource,
			OgemaHttpRequest req) {
		super(parent, id, text, null, req);
		this.pageURL = pageURL;
		this.providerId = providerId;
		this.schedConfigProv = schedConfigProv;
		if(fixedResource != null) addConfig(fixedResource);
	}
	
	public String addConfig(ConfigInfoExt sc) {
		String result = schedConfigProv.addConfig(sc);
		return result;
	}

	public String addConfig(Resource res) {
		ConfigInfoExt sc = new ConfigInfoExt(0, Arrays.asList(new Resource[]{res}));
		String result = schedConfigProv.addConfig(sc);
		return result;
	}
	public String addConfig(Resource res, String header) {
		ConfigInfoExt sc = new ConfigInfoExt(0, Arrays.asList(new Resource[]{res}));
		ConfigContextExternal cce = new ConfigContextExternal();
		cce.header = header;
		sc.context = cce;
		String result = schedConfigProv.addConfig(sc);
		return result;
	}
	
	protected void setConfigId(String configurationId, OgemaHttpRequest req) {
		setUrl(GenericPageConfigurationProvider.SMARTREFF_URL_BASEPATH+pageURL+"?providerId="+providerId+"&configId="+configurationId, req);
	}
}
