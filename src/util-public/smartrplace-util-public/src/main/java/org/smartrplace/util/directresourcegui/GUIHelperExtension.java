package org.smartrplace.util.directresourcegui;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;

import de.iwes.util.resource.OGEMAResourceCopyHelper;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.object.widget.popup.WidgetEntryData;
import de.iwes.widgets.resource.widget.table.ResourceTable;

public class GUIHelperExtension {
	public static <T extends Resource> ButtonConfirm addDeleteButton(
			ResourceList<T> objectList, T object, ResourceTable<T> mainTable,
			String id, Alert alert, Row row, ResourceGUIHelper<T> vh, OgemaHttpRequest req) {
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
			if(row != null) row.addCell("delete", deleteButton);
			else vh.popTableData.add(new WidgetEntryData("delete", deleteButton));
			deleteButton.registerDependentWidget(mainTable);
			if(alert != null) deleteButton.registerDependentWidget(alert);
			return deleteButton;
		} else {
			vh.registerHeaderEntry("delete");
			return null;
		}
	}
	
	//TODO: It would be better to provide a class with methods that can be overriden here
	public static <T extends Resource> Button addCopyButton(
			ResourceList<T> objectList, T object, ResourceTable<T> mainTable,
			String id, Alert alert, Row row, ResourceGUIHelper<T> vh, OgemaHttpRequest req,
			ApplicationManager appMan) {
		if(req != null) {
			Button copyButton = new Button(mainTable, "copyButton_"+id, "copy", req) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					if(object.isActive()) {
						T newCat = OGEMAResourceCopyHelper.copySubResourceIntoResourceList(objectList, object, appMan,
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
