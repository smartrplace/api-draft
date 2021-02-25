package org.smartrplace.heartbeat.gw.gui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.Datapoint;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeartOGEMAInstanceDpTransfer;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatInfoProvider;
import org.smartrplace.gui.filtering.GenericFilterFixedSingle;
import org.smartrplace.gui.filtering.GenericFilterOption;
import org.smartrplace.gui.filtering.SingleFiltering.OptionSavingMode;
import org.smartrplace.gui.filtering.SingleFilteringDirect;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.Button;

@SuppressWarnings("serial")
public abstract class DataTransmissionTableServer extends DataTransmissionTable {
	protected abstract ViaHeartbeartOGEMAInstanceDpTransfer getHbMan(String gwId);
	protected abstract Collection<String> getAllGateways();
	
	protected SingleFilteringDirect<String> gwSelection;
	
	public DataTransmissionTableServer(WidgetPage<?> page, ApplicationManager appMan) {
		super(page, appMan, null);
		//triggerPageBuild();
	}

	@Override
	protected ViaHeartbeartOGEMAInstanceDpTransfer getHbMan(OgemaHttpRequest req) {
		GenericFilterOption<String> selected = gwSelection.getSelectedItem(req);
		if(selected == gwSelection.NONE_OPTION)
			return null;
		String gw = ((GenericFilterFixedSingle<String>)selected).getValue();
		return getHbMan(gw);
	}
	
	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		gwSelection = new SingleFilteringDirect<String>(page, "gwSelection",
				OptionSavingMode.PER_USER, 10000, false) {

			@Override
			protected List<GenericFilterOption<String>> getOptionsDynamic(OgemaHttpRequest req) {
				List<GenericFilterOption<String>> result = new ArrayList<>();
				Collection<String> all = getAllGateways();
				for(String gw: all) {
					result.add(new GenericFilterFixedSingle<String>(gw, gw));
				}
				return result;
			}

			@Override
			protected long getFrameworkTime() {
				return appMan.getFrameworkTime();
			}
		};
		StaticTable topTable = new StaticTable(1, 4);
		Button logButton = getLogControlButton();
		topTable.setContent(0, 1, logButton);
		topTable.setContent(0, 0, gwSelection);
		page.append(topTable);
	}
	
	@Override
	protected String getHeader(OgemaLocale locale) {
		return "Datapoint Transmission Server";
	}
	
	@Override
	public Collection<DpTransData> getObjectsInTable(OgemaHttpRequest req) {
		ViaHeartbeartOGEMAInstanceDpTransfer hbManLoc = getHbMan(req);
		if(hbManLoc == null)
			return Collections.emptyList();
		
		List<DpTransData> result = new ArrayList<>();
		for(Entry<String, Datapoint> dpd: hbManLoc.getDatapointsToRecvM().entrySet()) {
			ViaHeartbeatInfoProvider infoProvider = hbManLoc.getInfoProvider(dpd.getValue());
			result.add(new DpTransData(dpd.getValue(), dpd.getKey(), false, infoProvider));
		}
		for(Entry<Datapoint, String> dpd: hbManLoc.getDatapointsToSendM().entrySet()) {
			ViaHeartbeatInfoProvider infoProvider = hbManLoc.getInfoProvider(dpd.getKey());
			result.add(new DpTransData(dpd.getKey(), dpd.getValue(), true, infoProvider));
		}
		return result;
	}

}
