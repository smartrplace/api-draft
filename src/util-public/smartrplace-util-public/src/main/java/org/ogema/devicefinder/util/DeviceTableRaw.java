package org.ogema.devicefinder.util;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.model.locations.Room;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;

public abstract class DeviceTableRaw<T, R extends Resource> extends ObjectGUITablePage<T,R>  {
	public static final long DEFAULT_POLL_RATE = 5000;
	
	
	protected abstract Class<? extends Resource> getResourceType();
	
	/** Unique ID for the table e.g. name of providing class*/
	protected abstract String id();
	
	/** Heading to be shown over the table*/
	protected abstract String getTableTitle();

	//protected abstract String getHeader(); // {return "Smartrplace Hardware InstallationApp";}
	//protected final InstalledAppsSelector appSelector;
	
	public DeviceTableRaw(WidgetPage<?> page, ApplicationManager appMan, Alert alert,
			T initSampleObject) {
		super(page, appMan, null, initSampleObject, null, false, true, alert);
	}

	public Button addDeleteButton(ObjectResourceGUIHelper<T, R> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom,
			Resource resourceToDelete) {
		if(req != null) {
			Button result = new Button(mainTable, "delete"+id, "Delete", req) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					resourceToDelete.delete();
				}
			};
			row.addCell(WidgetHelper.getValidWidgetId("Delete"), result);
			return result;
		} else
			vh.registerHeaderEntry("Delete");
		return null;
	}
	
	@Override
	public void addWidgetsAboveTable() {
		Header headerWinSens = new Header(page, WidgetHelper.getValidWidgetId("header_"+id()), getTableTitle());
		headerWinSens.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_CENTERED);
		page.append(headerWinSens);
	}

	@Override
	protected void addWidgetsBelowTable() {
	}
}
