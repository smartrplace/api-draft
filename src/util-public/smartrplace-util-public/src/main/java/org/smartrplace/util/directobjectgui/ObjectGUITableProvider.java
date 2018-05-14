package org.smartrplace.util.directobjectgui;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public interface ObjectGUITableProvider<T, R extends Resource> {
	void addWidgets(final T object,
			final ObjectResourceGUIHelper<T, R> vh, final String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan);

	R getResource(T object, OgemaHttpRequest req);
}
