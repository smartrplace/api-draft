package org.ogema.devicefinder.util;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.label.Label;

public class LastContactLabel extends Label {
	public static final long MAX_SENSOR_WITHOUT_VALUE_DEFAULT = 30*TimeProcUtil.MINUTE_MILLIS;
	private static final long serialVersionUID = 1L;

	protected final SingleValueResource resource;
	protected final ApplicationManager appMan;
	
	public LastContactLabel(WidgetPage<?> page, String id, SingleValueResource resource, ApplicationManager appMan) {
		super(page, id);
		this.appMan = appMan;
		this.resource = resource;
	}

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

	public static boolean isOffline(SingleValueResource resource, ApplicationManager appMan) {
		return isOffline(resource, MAX_SENSOR_WITHOUT_VALUE_DEFAULT, appMan);
	}
	public static boolean isOffline(SingleValueResource resource, long maxOffline, ApplicationManager appMan) {
		long ts = resource.getLastUpdateTime();
		long now = appMan.getFrameworkTime();
		return (now-ts > maxOffline);
	}
}
