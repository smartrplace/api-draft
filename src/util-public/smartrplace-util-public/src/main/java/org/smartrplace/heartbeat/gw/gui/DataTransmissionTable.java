package org.smartrplace.heartbeat.gw.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.Datapoint;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeartOGEMAInstanceDpTransfer;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatInfoProvider;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatInfoProvider.StringProvider;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.ButtonData;
import de.iwes.widgets.html.form.label.Label;

@SuppressWarnings("serial")
public class DataTransmissionTable extends ObjectGUITablePageNamed<DpTransData, Resource> {
	public static final long DEFAULT_POLL_RATE = 5000;

	protected final ViaHeartbeartOGEMAInstanceDpTransfer hbMan;
	protected final boolean isServer;
	protected ViaHeartbeartOGEMAInstanceDpTransfer getHbMan(OgemaHttpRequest req) {
		return hbMan;
	}
	
	public DataTransmissionTable(WidgetPage<?> page, ApplicationManager appMan,
			ViaHeartbeartOGEMAInstanceDpTransfer hbMan, boolean isServer) {
		super(page, appMan, new DpTransData(null, "init", false, null));
		this.hbMan = hbMan;
		this.isServer = isServer;
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return "Datapoint Transmission";
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		if(hbMan == null)
			return;
		StaticTable topTable = new StaticTable(1, 4);
		Button logButton = getLogControlButton();
		topTable.setContent(0, 1, logButton);
		page.append(topTable);
	}

	@Override
	public void addWidgets(DpTransData object, ObjectResourceGUIHelper<DpTransData, Resource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		vh.stringLabel("Key", id, object.key, row);
		if(req == null) {
			vh.registerHeaderEntry("Location");
			if(!isServer)
				vh.registerHeaderEntry("Alias");
			vh.registerHeaderEntry("isString");
			vh.registerHeaderEntry("type");
			vh.registerHeaderEntry("valueLastWrite");
			vh.registerHeaderEntry("lastSent");
			vh.registerHeaderEntry("lastRecv");
			vh.registerHeaderEntry("lastClean");
			return;
		}
		vh.stringLabel("Location", id, object.dp.id(), row);
		Set<String> als = object.dp.getAliases();
		if(!isServer) {
			if(als.isEmpty())
				vh.stringLabel("Alias", id, "--", row);
			else {
				String text = als.iterator().next()+" ("+als.size()+")";
				vh.stringLabel("Alias", id, text, row);
			}
		}
		Object prov = object.dp.getParameter(Datapoint.HEARTBEAT_STRING_PROVIDER_PARAM);
		if(prov == null) {
			vh.stringLabel("isString", id, "false", row);
			if(object.infoProvider != null) {
				Label label = vh.timeLabel("valueLastWrite", id, object.infoProvider.getLastValueWritten(), row, 2);
				//label.setPollingInterval(DEFAULT_POLL_RATE, req);
				label = vh.timeLabel("lastSent", id, object.infoProvider.getLastUpdateSent(), row, 2);
				//label.setPollingInterval(DEFAULT_POLL_RATE, req);
				label = vh.timeLabel("lastRecv", id, object.infoProvider.getLastRecvTime(), row, 2);
				//label.setPollingInterval(DEFAULT_POLL_RATE, req);				
			}
		} else if(prov instanceof StringProvider) {
			String text = prov.getClass().getSimpleName();
			vh.stringLabel("isString", id, text, row);
			StringProvider sprov = (StringProvider)prov;
			Label label = vh.timeLabel("valueLastWrite", id, sprov.getLastValueWritten(), row, 2);
			//label.setPollingInterval(DEFAULT_POLL_RATE, req);
			label = vh.timeLabel("lastSent", id, sprov.getLastUpdateSent(), row, 2);
			//label.setPollingInterval(DEFAULT_POLL_RATE, req);
			label = vh.timeLabel("lastRecv", id, sprov.getLastRecvTime(), row, 2);
			//label.setPollingInterval(DEFAULT_POLL_RATE, req);
			if(!object.isToSend)
				label = vh.timeLabel("lastClean", id, sprov.getLastClean(), row, 2);
			else {
				long lastclean = sprov.getLastClean();
				final String textc;
				if(lastclean <= 0)
					textc = "Clean";
				else
					textc = StringFormatHelper.getFormattedAgoValue(appMan, lastclean);
				Button cleanSchedBut = new Button(mainTable, "cleanSchedBut"+id, textc, req) {
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						sprov.resendAllOnNextOccasion();;
						alert.showAlert("Set schedule to send full clean data once.", true, req);
					}
				};
				cleanSchedBut.registerDependentWidget(alert, req);
				row.addCell("lastClean", cleanSchedBut);
			}
			//label.setPollingInterval(DEFAULT_POLL_RATE, req);
		} else
			vh.stringLabel("isString", id, "!!NO STRING_PROVIDER!!", row);
		
		vh.stringLabel("type", id, object.isToSend?"send":"recv", row);
	}

	protected Button getLogControlButton() {
		Button perm = new Button(page, "logControlBut") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				ViaHeartbeartOGEMAInstanceDpTransfer hbManLoc = getHbMan(req);
				if(hbManLoc == null) {
					disable(req);
					return;
				}
				enable(req);
				Boolean status = hbManLoc.forceConsoleLogging;
				setText(status ? "✓ console logging active": "✕  (activate logging on console) ", req);
				if (status) {
					setStyle(ButtonData.BOOTSTRAP_RED, req);
				} else {
					setStyle(ButtonData.BOOTSTRAP_GREEN, req);
				}
			}
			
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				ViaHeartbeartOGEMAInstanceDpTransfer hbManLoc = getHbMan(req);
				Boolean status = hbManLoc.forceConsoleLogging;
				if(status) {
					hbManLoc.forceConsoleLogging = false;
				} else
					hbManLoc.forceConsoleLogging = true;
			}
		};
		perm.registerDependentWidget(perm);
		return perm;
	}
	
	@Override
	protected String getTypeName(OgemaLocale locale) {
		return "Datapoint Location";
	}

	@Override
	protected String getLabel(DpTransData obj, OgemaHttpRequest req) {
		return obj.dp.id();
	}

	@Override
	public Collection<DpTransData> getObjectsInTable(OgemaHttpRequest req) {
		List<DpTransData> result = new ArrayList<>();
		for(Entry<String, Datapoint> dpd: hbMan.getDatapointsToRecvM().entrySet()) {
			ViaHeartbeatInfoProvider infoProvider = hbMan.getInfoProvider(dpd.getValue());
			result.add(new DpTransData(dpd.getValue(), dpd.getKey(), false, infoProvider));
		}
		for(Entry<Datapoint, String> dpd: hbMan.getDatapointsToSendM().entrySet()) {
			ViaHeartbeatInfoProvider infoProvider = hbMan.getInfoProvider(dpd.getKey());
			result.add(new DpTransData(dpd.getKey(), dpd.getValue(), true, infoProvider));
		}
		return result;
	}

	@Override
	public String getLineId(DpTransData object) {
		// TODO Auto-generated method stub
		return object.key+super.getLineId(object);
	}
}
