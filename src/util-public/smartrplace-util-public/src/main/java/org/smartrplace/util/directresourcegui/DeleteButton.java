package org.smartrplace.util.directresourcegui;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.buttonconfirm.ButtonConfirm;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.resource.widget.table.ResourceTable;

public class DeleteButton<T extends Resource> extends ButtonConfirm {
	private static final long serialVersionUID = 1L;

	private final ResourceList<T> objectList;
	private final T object;
	private final Alert alert;
	
	public DeleteButton(ResourceList<T> objectList, T object, ResourceTable<T> mainTable,
		String id, Alert alert, Row row, ResourceGUIHelper<T> vh, OgemaHttpRequest req) {
	
		super(mainTable, "deleteButton_"+id, req);
		
		this.objectList = objectList;
		this.object = object;
		this.alert = alert;
		
		setText("delete", req);
		setConfirmBtnMsg("Delete", req);
		setCancelBtnMsg("Cancel", req);
		setConfirmPopupTitle("Delete element", req);
		setConfirmMsg("Really delete item "+object.getLocation()+" ?", req);
		//if(row != null) row.addCell("delete", this);
		//else vh.popTableData.add(new WidgetEntryData("delete", this));
		//TODO
		registerDependentWidget(mainTable);
		if(alert != null) registerDependentWidget(alert);
	}
	
	@Override
	public void onPOSTComplete(String data, OgemaHttpRequest req) {
		if(objectList == null || (objectList.size() > 1))
			object.delete();
		else if(alert != null)
			alert.showAlert("Last element cannot be deleted", false, req);
			
	}

}
