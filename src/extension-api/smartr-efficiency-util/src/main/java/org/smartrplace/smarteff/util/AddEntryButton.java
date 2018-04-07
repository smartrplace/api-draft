package org.smartrplace.smarteff.util;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class AddEntryButton extends NaviOpenButton {
	private static final long serialVersionUID = 1L;

	public AddEntryButton(WidgetPage<?> page, String id, String pid, String text,
			Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage) {
		super(page, id, pid, text, type, exPage, PageType.EDIT_PAGE, true);
	}
	
	public AddEntryButton(OgemaWidget parent, String id, String pid, String text,
			Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			OgemaHttpRequest req) {
		super(parent, id, pid, text, type, exPage, PageType.EDIT_PAGE, true, req);
	}
}
