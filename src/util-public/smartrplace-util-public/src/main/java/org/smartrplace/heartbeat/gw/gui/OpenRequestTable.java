package org.smartrplace.heartbeat.gw.gui;

import java.util.Collection;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeartOGEMAInstanceDpTransfer;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeartOGEMAInstanceDpTransfer.OpenDpRequest;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public class OpenRequestTable extends ObjectGUITablePageNamed<OpenDpRequest, Resource> {
	public static final long DEFAULT_POLL_RATE = 5000;

	protected final ViaHeartbeartOGEMAInstanceDpTransfer hbMan;
	protected final boolean isServer;
	protected ViaHeartbeartOGEMAInstanceDpTransfer getHbMan(OgemaHttpRequest req) {
		return hbMan;
	}
	
	public OpenRequestTable(WidgetPage<?> page, ApplicationManager appMan,
			ViaHeartbeartOGEMAInstanceDpTransfer hbMan, boolean isServer) {
		super(page, appMan, new OpenDpRequest("initkey", "init", false));
		this.hbMan = hbMan;
		this.isServer = isServer;
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "Open Datapoint Requests";
	}
	
	@Override
	public void addWidgets(OpenDpRequest object, ObjectResourceGUIHelper<OpenDpRequest, Resource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		vh.stringLabel("Key", id, object.key, row);
		if(req == null) {
			vh.registerHeaderEntry("Location");
			vh.registerHeaderEntry("type");
			return;
		}
		vh.stringLabel("Location", id, object.dpIdFromRemote, row);
		
		vh.stringLabel("type", id, object.isToSend?"send":"recv", row);
	}

	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Datapoint Location";
	}

	@Override
	protected String getLabel(OpenDpRequest obj, OgemaHttpRequest req) {
		return obj.dpIdFromRemote;
	}

	@Override
	public Collection<OpenDpRequest> getObjectsInTable(OgemaHttpRequest req) {
		return hbMan.openRequests().values();
	}

	@Override
	public String getLineId(OpenDpRequest object) {
		return object.key+super.getLineId(object);
	}
}
