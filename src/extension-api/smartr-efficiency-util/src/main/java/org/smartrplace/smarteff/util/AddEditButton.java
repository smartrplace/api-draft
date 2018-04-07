package org.smartrplace.smarteff.util;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;

import de.iwes.widgets.api.widgets.WidgetPage;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class AddEditButton extends NaviOpenButton{
	private static final long serialVersionUID = 1L;

	public AddEditButton(WidgetPage<?> page, String id, String pid, String text,
			Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage) {
		super(page, id, pid, text, type, exPage, PageType.EDIT_PAGE, false);
	}
}
