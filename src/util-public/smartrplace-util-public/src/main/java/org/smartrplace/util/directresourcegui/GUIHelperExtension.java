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

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.OGEMAResourceCopyHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.object.widget.popup.WidgetEntryData;

public class GUIHelperExtension {
	/** Add button to delete object in a ObjectResourceGUIHelper table
	 * 
	 * @param <T>
	 * @param objectList if non-null then deleting of the object will be prevented for the last object in the 
	 * 		ResourceList
	 * @param object
	 * @param mainTable
	 * @param id
	 * @param alert
	 * @param row
	 * @param vh
	 * @param req
	 * @return
	 */
	public static <T extends Resource> ButtonConfirm addDeleteButton(
			ResourceList<T> objectList, T object, OgemaWidget mainTable,
			String id, Alert alert, Row row, ObjectResourceGUIHelper<?, ?> vh, OgemaHttpRequest req) {
		return addDeleteButton(objectList, object, mainTable, id, alert, "delete", row, vh, req);
	}
	public static <T extends Resource> ButtonConfirm addDeleteButton(
			ResourceList<T> objectList, T object, OgemaWidget mainTable,
			String id, Alert alert, String columnName,
			Row row, ObjectResourceGUIHelper<?, ?> vh, OgemaHttpRequest req) {
		if(req != null) {
			ButtonConfirm deleteButton = new ButtonConfirm(mainTable, "deleteButton_"+id, req) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					if(objectList == null || (objectList.size() > 1))
						object.delete();
					else if(alert != null)
						alert.showAlert("Last element cannot be deleted", false, req);
						
				}
			};
			deleteButton.setText("delete", req);
			deleteButton.setConfirmBtnMsg("Delete", req);
			deleteButton.setCancelBtnMsg("Cancel", req);
			deleteButton.setConfirmPopupTitle("Delete element", req);
			deleteButton.setConfirmMsg("Really delete item "+object.getLocation()+" ?", req);
			if(row != null) row.addCell(WidgetHelper.getValidWidgetId(columnName), deleteButton);
			else vh.popTableData.add(new WidgetEntryData("delete", deleteButton));
			deleteButton.registerDependentWidget(mainTable);
			if(alert != null) deleteButton.registerDependentWidget(alert);
			return deleteButton;
		} else {
			vh.registerHeaderEntry(columnName);
			return null;
		}
	}
	
	//TODO: It would be better to provide a class with methods that can be overriden here
	public static <T extends Resource> Button addCopyButton(
			ResourceList<T> objectList, T object, OgemaWidget mainTable,
			String id, Alert alert, Row row, ObjectResourceGUIHelper<?, ?> vh, OgemaHttpRequest req,
			ApplicationManager appMan) {
		return addCopyButton(objectList, object, mainTable, id, alert, row, vh, req, appMan, null);
	}
	public static <T extends Resource> Button addCopyButton(
			ResourceList<T> objectList, T object, OgemaWidget mainTable,
			String id, Alert alert, Row row, ObjectResourceGUIHelper<?, ?> vh, OgemaHttpRequest req,
			ApplicationManager appMan,
			Class<? extends T> typeToCreate) {
		if(req != null) {
			Button copyButton = new Button(mainTable, "copyButton_"+id, "copy", req) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					if(object.isActive()) {
						final T newCat;
						if(typeToCreate != null) {
							String name = ResourceListHelper.getUniqueNameForNewElement(objectList);
							newCat = objectList.getSubResource(name, typeToCreate);
							OGEMAResourceCopyHelper.copySubResourceIntoDestination(newCat, object, appMan, false);
						} else
							newCat = OGEMAResourceCopyHelper.copySubResourceIntoResourceList(objectList, object, appMan,
									false);
						newCat.getSubResource("name", StringResource.class).setValue(
								"CopyOf_"+object.getSubResource("name", StringResource.class).getValue());
					} else {
						object.activate(true);
					}
				}
				@Override
				public void onGET(OgemaHttpRequest req) {
					if(object.isActive())
						setText("copy", req);
					else
						setText("activate", req);
				}
			};
			if(row != null) row.addCell("copy", copyButton);
			else vh.popTableData.add(new WidgetEntryData("copy", copyButton));
			copyButton.registerDependentWidget(mainTable);
			return copyButton;
		} else {
			vh.registerHeaderEntry("copy");
			return null;
		}
	}
}
