package org.ogema.devicefinder.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.apps.roomsim.service.api.util.SingleRoomSimulationBase;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.html.form.label.Label;

public abstract class DeviceTableBase extends ObjectGUITablePage<InstallAppDevice,InstallAppDevice> {
	public static final long DEFAULT_POLL_RATE = 5000;
	
	public static interface InstalledAppsSelector {
		List<InstallAppDevice> getDevicesSelected();

		<T extends Resource> InstallAppDevice addDeviceIfNew(T model, DeviceHandlerProvider<T> tableProvider);
		<T extends Resource> InstallAppDevice removeDevice(T model);
		
		default  <T extends Resource> SingleRoomSimulationBase getRoomSimulation(T model) {
			return null;
		}
		
		ApplicationManager getAppManForSimulationStart();
	}
	
	protected abstract Class<? extends Resource> getResourceType();
	
	/** Unique ID for the table e.g. name of providing class*/
	protected abstract String id();
	
	/** Heading to be shown over the table*/
	protected abstract String getTableTitle();

	//protected abstract String getHeader(); // {return "Smartrplace Hardware InstallationApp";}
	protected final InstalledAppsSelector appSelector;
	
	public DeviceTableBase(WidgetPage<?> page, ApplicationManager appMan, Alert alert,
			InstalledAppsSelector appSelector) {
		super(page, appMan, null, null, InstallAppDevice.class, false, true, alert);
		this.appSelector = appSelector;
	}

	protected void addInstallationStatus(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		Map<String, String> valuesToSet = new HashMap<>();
		valuesToSet.put("0", "unknown");
		valuesToSet.put("1", "Device installed physically");
		valuesToSet.put("10", "Physical installation done including all on-site tests");
		valuesToSet.put("20", "All configuration finished, device is in full operation");
		valuesToSet.put("-10", "Error in physical installation and/or testing (explain in comment)");
		valuesToSet.put("-20", "Error in configuration, device cannot be used/requires action for real usage");
		vh.dropdown("Status", id, object.installationStatus(), row, valuesToSet );
	}
	
	protected void addComment(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		vh.stringEdit("Comment", id, object.installationComment(), row, alert);
	}
	
	protected void addHomematicCCUId(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		if(req != null) {
			String text = getHomematicCCUId(object.device().getLocation());
			vh.stringLabel("RT", id, text, row);
		} else
			vh.registerHeaderEntry("RT");
	}
	
	protected void addRoomWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		Map<Room, String> roomsToSet = new HashMap<>();
		List<Room> rooms = appMan.getResourceAccess().getResources(Room.class);
		for(Room room: rooms) {
			roomsToSet.put(room, ResourceUtils.getHumanReadableShortName(room));
		}
		vh.referenceDropdownFixedChoice("Room", id, deviceRoom, row, roomsToSet, 3);
	}
	
	protected void addSubLocation(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		vh.stringEdit("Location", id, object.installationLocation(), row, alert);
	}
	
	protected IntegerResource addRSSI(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		IntegerResource source = ResourceHelper.getSubResourceOfSibbling(object.device().getLocationResource(),
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiDevice", IntegerResource.class);
		vh.intLabel("RSSI", id, source, row, 0);
		return source;
	}
		
	protected Label addBattery(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom,
			FloatResource batteryReading) {
		return vh.floatLabel("Battery", id, batteryReading, row, "%.1f#min:0.1");
	}
	
	protected Label addLastContact(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom,
			FloatResource valueReadingResource) {
		Label lastContact = null;
		if(req != null) {
			lastContact = new LastContactLabel(valueReadingResource, appMan, mainTable, "lastContact"+id, req);
			row.addCell(WidgetHelper.getValidWidgetId("Last Contact"), lastContact);
			return lastContact;
		} else
			vh.registerHeaderEntry("Last Contact");
		return null;
	}

	public Resource addNameWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		final Resource device;
		if(req == null)
			device = ResourceHelper.getSampleResource(getResourceType());
		else
			device = object.device();
		final String name;
		if(device.getLocation().toLowerCase().contains("homematic")) {
			name = "WindowSens HM:"+ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
		} else
			name = ResourceUtils.getHumanReadableShortName(device);
		vh.stringLabel("Name", id, name, row);
		return device;
	}
	
	public Button addDeleteButton(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom,
			Resource resourceToDelete) {
		if(req != null) {
			Button result = new Button(mainTable, "delete"+id, "Delete", req) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					resourceToDelete.delete();
				}
			};
			row.addCell(WidgetHelper.getValidWidgetId("Delete"), result);
			return result;
		} else
			vh.registerHeaderEntry("Delete");
		return null;
	}

	@Override
	public List<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
		List<InstallAppDevice> all = appSelector.getDevicesSelected();
		List<InstallAppDevice> result = new ArrayList<InstallAppDevice>();
		for(InstallAppDevice dev: all) {
			if(getResourceType().isAssignableFrom(dev.device().getResourceType())) {
				result.add(dev);
			}
		}
		return result;
	}
	
	@Override
	public void addWidgetsAboveTable() {
		Header headerWinSens = new Header(page, WidgetHelper.getValidWidgetId("header_"+id()), getTableTitle());
		headerWinSens.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_CENTERED);
		page.append(headerWinSens);
	}

	@Override
	protected void addWidgetsBelowTable() {
	}

	@Override
	public InstallAppDevice getResource(InstallAppDevice object, OgemaHttpRequest req) {
		return object;
	}
	
	public static String getHomematicCCUId(String location) {
		String[] parts = location.split("/");
		String tail;
		if(parts[0].toLowerCase().startsWith("homematicip")) {
			tail = parts[0].substring("homematicip".length());
		} else {
			if(!parts[0].startsWith("homematic")) return "n/a";
			tail = parts[0].substring("homematic".length());			
		}
		if(tail.isEmpty()) return "102";
		else return tail;
	}

}
