package org.smartrplace.apps.hw.install.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.config.RoomSelectorDropdown;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;

public abstract class DeviceTablePageFragment extends ObjectGUITablePage<InstallAppDevice,InstallAppDevice> {
	public static final long DEFAULT_POLL_RATE = 5000;
	
	protected abstract Class<? extends Resource> getResourceType();
	
	protected HardwareInstallController controller;
	private Header header;
	//private Alert alert;
	protected RoomSelectorDropdown roomsDrop;
	
	public DeviceTablePageFragment(WidgetPage<?> page, HardwareInstallController controller,
			RoomSelectorDropdown roomsDrop, Alert alert) {
		super(page, controller.appMan, null, null, InstallAppDevice.class, false, true, alert);
		this.controller = controller;
		this.roomsDrop = roomsDrop;
		//retardationOnGET = 2000;
		
		//triggerPageBuild();
	}

	protected void addWidgetsCommon(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		Map<Room, String> roomsToSet = new HashMap<>();
		List<Room> rooms = controller.appMan.getResourceAccess().getResources(Room.class);
		for(Room room: rooms) {
			roomsToSet.put(room, ResourceUtils.getHumanReadableShortName(room));
		}
		vh.referenceDropdownFixedChoice("Room", id, deviceRoom, row, roomsToSet );
		
		Map<String, String> valuesToSet = new HashMap<>();
		valuesToSet.put("0", "unknown");
		valuesToSet.put("1", "Device installed physically");
		valuesToSet.put("10", "Physical installation done including all on-site tests");
		valuesToSet.put("20", "All configuration finished, device is in full operation");
		valuesToSet.put("-10", "Error in physical installation and/or testing (explain in comment)");
		valuesToSet.put("-20", "Error in configuration, device cannot be used/requires action for real usage");
		vh.dropdown("Status", id, object.installationStatus(), row, valuesToSet );
		
		vh.stringEdit("Comment", id, object.installationComment(), row, alert);
	}
	
	protected void addWidgetsCommonExpert(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		vh.stringEdit("Location", id, object.installationLocation(), row, alert);
		
	}
	
	@Override
	public void addWidgetsAboveTable() {
		if(roomsDrop != null) return;
		header = new Header(page, "header", "Smartrplace Hardware InstallationApp");
		header.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_CENTERED);
		page.append(header).linebreak();
		
		StaticTable topTable = new StaticTable(1, 5, new int[] {1, 3, 3, 3, 2});
		BooleanResourceButton installMode = new BooleanResourceButton(page, "installMode", "Installation Mode",
				controller.appConfigData.isInstallationActive()) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onPrePOST(String data, OgemaHttpRequest req) {
				super.onPrePOST(data, req);
				controller.checkDemands();
			}
		};
		roomsDrop = new RoomSelectorDropdown(page, "roomsDrop", controller);
		
		topTable.setContent(0, 0, roomsDrop).setContent(0, 1, installMode);
		page.append(topTable);
	}
	
	@Override
	public List<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
		List<InstallAppDevice> all = roomsDrop.getDevicesSelected();
		List<InstallAppDevice> result = new ArrayList<InstallAppDevice>();
		for(InstallAppDevice dev: all) {
			if(getResourceType().isAssignableFrom(dev.device().getResourceType())) {
				result.add(dev);
			}
		}
		return result;
	}
	
	@Override
	protected void addWidgetsBelowTable() {
	}

	@Override
	public InstallAppDevice getResource(InstallAppDevice object, OgemaHttpRequest req) {
		return object;
	}

}
