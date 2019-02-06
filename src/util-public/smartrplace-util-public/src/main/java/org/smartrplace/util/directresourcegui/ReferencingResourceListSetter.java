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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;

import de.iwes.widgets.api.extended.mode.UpdateMode;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.resource.widget.multiselect.ResourceMultiselect;

/**Allows to set the elements of a resource list that only contains references via a Multiselect. Derived
 * from in StringArrayResourceSetter in appstore-gui-dev
 * TODO: Currently this works only for quasi-global tables like the ones typically generated via ResourceGUITable.
 * @author dnestle
 *
 */
public class ReferencingResourceListSetter<T extends Resource> {
	public final ResourceMultiselect<T> multiSelect;
	public final Button submit;
	//TODO: session specific options not yet supported
	private Collection<T> options;
	private Map<T, String> labels = null;
	private List<ResourceList<T>> selectionExclusiveWith = new ArrayList<>();

	private boolean changed = false;
	
	/**
	 * 
	 * @param page
	 * @param id
	 * @param resource
	 * @param defaultOptions may be null
	 */
	public ReferencingResourceListSetter(OgemaWidget parent, String id, ResourceList<T> resource,
			Collection<T> defaultOptions, OgemaHttpRequest req) {
		if(defaultOptions == null) {
			this.options = Collections.emptyList();
		} else
			this.options = defaultOptions;
		multiSelect = new ResourceMultiselect<T>(parent, id+"Multi", null, UpdateMode.MANUAL, null, req) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				if(resource.exists()) {
					Set<T> items = new HashSet<>();
					List<T> items2 = resource.getAllElements();
					items.addAll(items2);
					items.addAll(options);
					update(items, req);
					selectItems(items2, req);
				}
				else {
					update(options, req);
					selectItems(Collections.emptyList(), req);
				}
			}
		};
		/*multiSelect.setTemplate(new DefaultDisplayTemplate<T>() {
			@Override
			public String getLabel(T object, OgemaLocale locale) {
				if(labels == null)
					return super.getLabel(object, locale);
				String label = labels.get(object);
				if(label != null) return label;
				return super.getLabel(object, locale);
			}
		});*/
	
		submit = new Button(parent, id+"submit", "Submit/Add value:", req) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				Collection<T> out = multiSelect.getSelectedItems(req);
				//String[] result = out.toArray(new String[0]);
				if(!resource.exists()) {
					resource.create();
					updateReferencingResourceList(resource, out);
					resource.activate(false);
				} else
					updateReferencingResourceList(resource, out);
				for(T appId: out) {
				for(ResourceList<T> slist: selectionExclusiveWith) {
					if(!slist.isActive()) continue;
					List<T> vals = slist.getAllElements();
					for(int i=0; i<slist.size(); i++) {
						if(vals.get(i).equalsLocation(appId)) {
							vals.get(i).delete();
							break;
						}
					}
				}
				}
				changed = true;
			}
		};
		submit.addWidget(multiSelect);
		submit.registerDependentWidget(multiSelect);
	}
	
	public void setDefaultOptions(Collection<T> options) {
		this.options = options;
	}
	public Collection<T> getDefaultOptions() {
		return options;
	}
	public void setLabels(Map<T, String> labels) {
		this.labels = labels;
	}
	public Map<T, String> getLabels() {
		return labels;
	}
	public void addExclusiveWithList(ResourceList<T> exList) {
		selectionExclusiveWith.add(exList);
	}
	
	/**Get information that a value was submitted once. When called the next time no change will
	 * be reported anymore until a new value is submitted
	 */
	public boolean checkConfirmChanged() {
		if(!changed) return false;
		changed = false;
		return true;
	}
	
	/**In a ResourceList containing only references set the list of resources referenced to a new input list
	 * 
	 * @param resourceList ResourceList to be updated
	 * @param newResources list of resources that shall be contained in the ResourceList after the operation. All
	 * resources not yet in the ResourceList will be added, all resources not in newResources will be removed from
	 * the ResoureList.
	 * TODO: Move to util-extended.ResourceListHelper
	 */
	public static <S extends Resource> void updateReferencingResourceList(ResourceList<S> resourceList, Collection<S> newResources) {
		for(S res: newResources) {
			if(!resourceList.contains(res)) resourceList.add(res);
		}
		for(S exist: resourceList.getAllElements()) {
			boolean found = false;
			for(S res: newResources) {
				if(exist.equalsLocation(res)) {
					found = true;
					break;
				}
			}
			if(!found) exist.delete();
		}
	}
}
