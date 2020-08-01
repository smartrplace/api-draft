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
package org.smartrplace.util.directresourcegui;

import java.util.Collection;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.resource.widget.dropdown.ResourceDropdown;
import de.iwes.widgets.resource.widget.init.ResourceInitSingleEmpty;
import de.iwes.widgets.template.DisplayTemplate;
/**
 * An HTML page, generated from the Java code.
 */
public class ResourceEditPage<T extends Resource> {
	
	public final long UPDATE_RATE = 5*1000;

	protected final ApplicationManager appMan;
	
	//private Label storeLabel = null;
	protected final WidgetPage<?> page;
	protected final ResourceInitSingleEmpty<T> init;
	protected final ResourceDropdown<T> drop;
	protected final T aggregatedData;
	
	/** Overwrite if necessary
	 * @param resourceType */
	protected Collection<? extends T> getItemsInDropdown(Class<T> resourceType) {
		return appMan.getResourceAccess().getResources(resourceType);		
	}
	
	/**Overwrite this to provide different url or set to null to avoid having the "Main Page" button*/
	protected String getOverviewPageUrl() {
		return "index.html";
	}
	
	public ResourceEditPage(final WidgetPage<?> page, final ApplicationManager appMan,
			T aggregatedData, final Class<T> resourceType, final LabelProvider<T> dropLabels) {
		this.appMan = appMan;
		this.page = page;
		this.aggregatedData = aggregatedData;
		init = new ResourceInitSingleEmpty<T>(page, "init", true, appMan) {
			private static final long serialVersionUID = 1L;
			@Override
			public T getSelectedItem(OgemaHttpRequest req) {
				T res = super.getSelectedItem(req);
				if(res!= null) return res;
				//res.remoteData().connectionCounter()
				res =  ResourceEditPage.this.aggregatedData;
				if(res!= null) return res;
				return drop.getSelectedItem(req);
			}
			@Override
			public void updateDependentWidgets(OgemaHttpRequest req) {
				T res = getSelectedItem(req);
				Collection<? extends T> items = getItemsInDropdown(resourceType);
				drop.update(items , req);
				drop.selectItem(res, req);
				/*for( T opt: drop.getItems(req)) {
					if(opt.equalsLocation(res)) {
						drop.selectItem(opt, req);
						break;
					}
				}*/
			}
		};
		
		drop = new ResourceDropdown<T>(page, "drop") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				init.selectItem(getSelectedItem(req), req);
			}
		};
		drop.setTemplate(new DisplayTemplate<T>() {
			@Override
			public String getLabel(T object, OgemaLocale locale) {
				return dropLabels.getLabel(object);
			}
			
			@Override
			public String getId(T object) {
				return object.getLocation();
			}
		});
		init.registerDependentWidget(drop);
		page.append(drop);
		
	}
	
	protected void finalize(StaticTable table) {
		if(table != null) registerDependentWidgets(drop, table);
		String mainUrl = getOverviewPageUrl();
		if(mainUrl != null) {
			RedirectButton mainPageBut = new RedirectButton(page, "mainPageBut", "Main page",
					mainUrl);
			page.append(mainPageBut);
		}
	}
	public static void registerDependentWidgets(OgemaWidget governor, StaticTable table) {
		for(OgemaWidget el: table.getSubWidgets()) {
			//Note: Synchronization issues with triggerAction
			//governor.triggerOnPOST(el);
			governor.registerDependentWidget(el);
		}
	}
}
