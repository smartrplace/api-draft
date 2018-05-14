package org.smartrplace.util.directresourcegui;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.smartrplace.util.directobjectgui.ObjectGUITableProvider;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public interface ResourceGUITableProvider<T extends Resource> extends ObjectGUITableProvider<T, T> {

	void addWidgets(final T object, final ResourceGUIHelper<T> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan);
/*	void addWidgets(final T object,
			final ResourceGUIHelper<T> vh, final String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan);*/
}
