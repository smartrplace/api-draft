package org.smartrplace.apps.hw.install.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.config.RoomSelectorDropdown;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;
import org.smartrplace.util.directresourcegui.ResourceGUITablePage;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;

public class MainPage extends ResourceGUITablePage<InstallAppDevice> {
	private HardwareInstallController controller;
	private Header header;
	private Alert alert;
	private RoomSelectorDropdown roomsDrop;
	
	public MainPage(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller.appMan, null, InstallAppDevice.class, false, true);
		this.controller = controller;
		
		triggerPageBuild();
	}

	@Override
	public void addWidgets(InstallAppDevice object, ResourceGUIHelper<InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		if(!(object.device() instanceof Thermostat)) return;
		Thermostat device = (Thermostat) object.device();
		final String name;
		if(device.getLocation().toLowerCase().contains("homematic")) {
			name = "HM:"+ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
		} else
			name = ResourceUtils.getHumanReadableShortName(device);
		vh.stringLabel("Name", id, name, row);
		vh.floatEdit("Setpoint", id, device.temperatureSensor().settings().setpoint(), row, alert, 4.5f, 30f, "Allowed range: 4.5 to 30°C");
		vh.floatLabel("Measurement", id, device.temperatureSensor().reading(), row, "%.1f");
		vh.floatLabel("Battery", id, device.battery().chargeSensor().reading(), row, "%.1f");
		Map<Room, String> roomsToSet = new HashMap<>();
		List<Room> rooms = controller.appMan.getResourceAccess().getResources(Room.class);
		for(Room room: rooms) {
			roomsToSet.put(room, ResourceUtils.getHumanReadableShortName(room));
		}
		vh.referenceDropdownFixedChoice("Room", id, device.location().room(), row, roomsToSet );
		vh.stringEdit("Location", id, object.installationLocation(), row, alert);
		
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

	@Override
	public void addWidgetsAboveTable() {
		header = new Header(page, "header", "Smartrplace Hardware InstallationApp");
		header.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_CENTERED);
		page.append(header).linebreak();
		
		alert = new Alert(page, "alert", "");
		alert.setDefaultVisibility(false);
		page.append(alert).linebreak();
		
		StaticTable topTable = new StaticTable(1, 5, new int[] {1, 3, 3, 3, 2});
		BooleanResourceButton installMode = new BooleanResourceButton(page, "installMode", "Installation Mode",
				controller.appConfigData.isInstallationActive());
		roomsDrop = new RoomSelectorDropdown(page, "roomsDrop", controller);
		
		topTable.setContent(0, 0, roomsDrop).setContent(0, 1, installMode);
		page.append(topTable);
	}
	
	@Override
	public List<InstallAppDevice> getResourcesInTable(OgemaHttpRequest req) {
		return roomsDrop.getDevicesSelected();
	}
	
	@Override
	protected void addWidgetsBelowTable() {
	}

}
