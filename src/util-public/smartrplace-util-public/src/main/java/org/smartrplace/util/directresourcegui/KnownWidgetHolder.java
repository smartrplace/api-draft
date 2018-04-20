package org.smartrplace.util.directresourcegui;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.emptywidget.EmptyWidget;

public class KnownWidgetHolder<T> extends EmptyWidget {

	private static final long serialVersionUID = 1L;

	public KnownWidgetHolder(WidgetPage<?> page, String id) {
		super(page, id);
	}
	
	@Override
	public KnownWidgetHolderData<T> createNewSession() {
		return new KnownWidgetHolderData<T>(this);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public KnownWidgetHolderData<T> getData(OgemaHttpRequest req) {
		return (KnownWidgetHolderData<T>) super.getData(req);
	}
	
}

