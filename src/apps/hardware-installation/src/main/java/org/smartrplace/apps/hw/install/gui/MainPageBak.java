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
package org.smartrplace.apps.hw.install.gui;

import java.util.List;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.model.action.Action;
import org.ogema.tools.resource.util.ResourceUtils;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;

/**
 * An HTML page, generated from the Java code.
 */
public class MainPageBak {
	
	public final long UPDATE_RATE = 5*1000;
	
	public final DynamicTable<Action> gatewayTable;
	
	public MainPageBak(final WidgetPage<?> page, final ApplicationManager appMan) {

		Header header = new Header(page, "header", "Smartrplace Hardware InstallationApp");
		header.addDefaultStyle(HeaderData.CENTERED);
		page.append(header).linebreak();
		
		Alert alert = new Alert(page, "alert", "");
		alert.setDefaultVisibility(false);
		page.append(alert).linebreak();
		
		gatewayTable = new DynamicTable<Action>(page, "gatewayTable", true);
		List<Action> acl = appMan.getResourceAccess().getResources(Action.class);
		Action ac = null;
		if(!acl.isEmpty()) ac = acl.get(0);
		//ActionTemplate template = new ActionTemplate(page, alert, appMan, ac);
		//gatewayTable.setRowTemplate(template);
		
		page.append(gatewayTable);
	}
	
	public void addRowIfNotExisting(Action info) {
		String id = ResourceUtils.getValidResourceName(info.getLocation());		
		Set<String> rows = gatewayTable.getRows(null);
		if (!rows.contains(id)) {
			gatewayTable.addItem(info, null);
		}
	}		

}
