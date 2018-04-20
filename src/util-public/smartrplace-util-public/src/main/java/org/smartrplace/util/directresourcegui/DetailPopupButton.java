package org.smartrplace.util.directresourcegui;

import java.util.ArrayList;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.smartrplace.util.directobjectgui.ObjectDetailPopupButton;
import org.smartrplace.util.directobjectgui.ObjectGUIHelperBase;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.object.widget.popup.ClosingPopup;
import de.iwes.widgets.resource.widget.table.ResourceTable;

public class DetailPopupButton<T extends Resource> extends ObjectDetailPopupButton<T, T> {
	
	public DetailPopupButton(ResourceTable<T> mainTable, String id, String text, OgemaHttpRequest req,
			final ClosingPopup<T> popMore1, final T object, ApplicationManager appMan,
			final String tableId, final KnownWidgetHolder<T> knownWidgets,
			ResourceGUITableProvider<T> widgetProvider) {
		super(mainTable, id, text, req, popMore1, object, appMan, tableId, knownWidgets, widgetProvider);
	}
	private static final long serialVersionUID = 1L;
	
	protected ObjectGUIHelperBase<T> getObjectGUIHelperBase(KnownWidgetHolderData<T> clData, OgemaHttpRequest req) {
		ResourceGUIHelper<T> popvh = new ResourceGUIHelper<T>(popMore1.getPopupSnippet(), req, object, appMan, false);
		//popvh.pageSnippet = popMore1.getPopupSnippet();
		popvh.popTableData = new ArrayList<>();
		popvh.evaluteForDetailsPopup(ObjectGUIHelperBase.WidgetsToAdd.ALL);
		widgetProvider.addWidgets(object, popvh, tableId+"pup"+clData.counter++, req, null, appMan);
		return popvh;
	}
}
