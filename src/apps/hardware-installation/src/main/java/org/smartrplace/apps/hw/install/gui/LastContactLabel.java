package org.smartrplace.apps.hw.install.gui;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.SingleValueResource;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.label.Label;

public class LastContactLabel extends Label {
	private static final long serialVersionUID = 1L;

	protected final SingleValueResource resource;
	protected final ApplicationManager appMan;
	
	public LastContactLabel(SingleValueResource resource, ApplicationManager appMan,
			OgemaWidget parent, String id, OgemaHttpRequest req) {
		super(parent, id, req);
		this.appMan = appMan;
		this.resource = resource;
	}

	@Override
	public void onGET(OgemaHttpRequest req) {
		long ts = resource.getLastUpdateTime();
		String val = StringFormatHelper.getFormattedAgoValue(appMan, ts);
		setText(val, req);
	}

}
