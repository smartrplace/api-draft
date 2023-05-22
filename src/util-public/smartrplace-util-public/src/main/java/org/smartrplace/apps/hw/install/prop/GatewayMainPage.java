package org.smartrplace.apps.hw.install.prop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gui.tablepages.ObjectGUITablePageNamed;
import org.smartrplace.system.guiappstore.config.AppstoreConfig;
import org.smartrplace.system.guiappstore.config.GatewayData;
import org.smartrplace.system.guiappstore.config.GatewayGroupData;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.RedirectButton;

public class GatewayMainPage extends ObjectGUITablePageNamed<GatewayData, GatewayData> {
	public static Map<String, String> valuesToSetInstallGw = new HashMap<>(DeviceTableRaw.valuesToSetInstall);
	static  {
		valuesToSetInstallGw.put("1", "SerialNumberRecorded: Device production has fixed deviceID");
	}
	//protected final AppstoreGUIController controller;
	protected final AppstoreConfig appConfigData;
	protected StaticTable topTable;
	//protected final PopupSimple gatewayCreatePopup;
	
	protected final boolean isOperationStandardPage;
	
	protected final String serverGatewayLink;

	public GatewayMainPage(WidgetPage<?> page, ApplicationManager appMan, AppstoreConfig appConfigData,
			String serverGatewayLink) {
		this(page, appMan, appConfigData, serverGatewayLink, false);
	}
	public GatewayMainPage(WidgetPage<?> page, ApplicationManager appMan, AppstoreConfig appConfigData,
			String serverGatewayLink, boolean isOperationStandardPage) {
		super(page, appMan, ResourceHelper.getSampleResource(GatewayData.class));
		//this.controller = controller;
		this.appConfigData = appConfigData;
		this.serverGatewayLink = serverGatewayLink;
		this.isOperationStandardPage = isOperationStandardPage;
		triggerPageBuild();
	}

	@Override
	protected String getHeader(OgemaLocale locale) {
		return Boolean.getBoolean("org.smartrplace.system.guiappstore.gui.gatewayperbuildingunit")?"Building Unit Overview":"Gateways Overview";
	}
	
	@Override
	public void addWidgets(GatewayData object, ObjectResourceGUIHelper<GatewayData, GatewayData> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		addNameLabel(object, vh, id, row, req);
		if(req == null) {
			vh.registerHeaderEntry("Software Release Group");
			vh.registerHeaderEntry("Expected Online");
			vh.registerHeaderEntry("GUI");
			if(isOperationStandardPage) {
				vh.registerHeaderEntry("Op Link");
				vh.registerHeaderEntry("Controller");
				vh.registerHeaderEntry("Mode");
				vh.registerHeaderEntry("Operation Status");
			}
		} else {
			Map<GatewayGroupData, String> valuesToSetG = new HashMap<>();
			for(GatewayGroupData grGrp: ResourceListHelper.getAllElementsLocation(appConfigData.gatewayGroupData())) {
				valuesToSetG.put(grGrp, ResourceUtils.getHumanReadableShortName(grGrp));
			}
			vh.referenceDropdownFixedChoice("Software Release Group", id, object.installationLevelGroup(), row, valuesToSetG);
			vh.booleanEdit("Expected Online", id, object.expectedOnHeartbeat(), row);
			String gwUrl = ServerGatewayUtil.getGatewayBaseUrl(object);
			if(gwUrl != null) {
				vh.linkingButton("GUI", id, object, row, "To GW", gwUrl+"/ogema/index.html");
				if(isOperationStandardPage) {
					vh.linkingButton("Op Link", id, object, row, "Apps for Admin", gwUrl+"/org/smartrplace/hardwareinstall/superadmin/ccutDetails.hmtl.html");
					vh.linkingButton("Controller", id, object, row, "Controller", gwUrl+"/org/sp/app/drivermonapp/index.html");
					vh.linkingButton("Mode", id, object, row, "Mode", gwUrl+"/reactroomcontrolWE/index.html#/reactroomcontrolWE/settings");
					InstallAppDevice dev = DpGroupUtil.getInstallAppDevice(object, appMan.getResourceAccess());
					vh.stringEdit("Operation Status", id, dev.operationStatus(), row, alert);
				}
			}
			/*if(object.guiLink().isActive()) {
				RedirectButton guiButton = new RedirectButton(mainTable, id+"guiButton", "GUI",
						object.guiLink().getValue()+"/ogema/index.html", req);
				row.addCell("GUI", guiButton);
			}*/
		}
		vh.stringEdit("Customer", id, object.customer(), row, alert);
		if(req == null) {
			vh.registerHeaderEntry("Comment");
			vh.registerHeaderEntry("Status");
		} else {
			InstallAppDevice dev = DpGroupUtil.getInstallAppDevice(object, appMan.getResourceAccess());
			if(dev != null) {
				vh.dropdown("Status", id, dev.installationStatus(), row, valuesToSetInstallGw);
				vh.stringEdit("Comment", id, dev.installationComment(), row, alert);
			}
		}
		if(!isOperationStandardPage) {
			vh.stringEdit("GUI Link", id, object.guiLink(), row, alert);
			vh.stringEdit("SlotsGwId", id, object.remoteSlotsGatewayId(), row, alert);
		}
	}

	@Override
	public void addWidgetsAboveTable() {
		super.addWidgetsAboveTable();
		topTable = new StaticTable(1, 4);
		
		if(serverGatewayLink != null) {
			RedirectButton otherGwButton = new RedirectButton(page, "otherGwButton", "Server instances",
				serverGatewayLink);
			topTable.setContent(0, 2, otherGwButton);
		}
		page.append(topTable);
	}
	
	@Override
	protected String getTypeName(OgemaLocale locale) {
		return Boolean.getBoolean("org.smartrplace.system.guiappstore.gui.gatewayperbuildingunit")?"Building Unit":"Gateway";
	}

	@Override
	protected String getLabel(GatewayData obj, OgemaHttpRequest req) {
		return ResourceUtils.getHumanReadableShortName(obj);
	}

	@Override
	public Collection<GatewayData> getObjectsInTable(OgemaHttpRequest req) {
		if(isOperationStandardPage) {
			List<GatewayData> all = appConfigData.gatewayData().getAllElements();
			List<GatewayData> result = new ArrayList<>();
			for(GatewayData gw: all) {
				if(gw.expectedOnHeartbeat().getValue())
					result.add(gw);
			}
			return result;
		}
		return appConfigData.gatewayData().getAllElements();
	}


	@Override
	public String getLineId(GatewayData object) {
		String baseId = ViaHeartbeatUtil.getBaseGwId(ResourceUtils.getHumanReadableShortName(object));
		return baseId+super.getLineId(object);
	}
}
