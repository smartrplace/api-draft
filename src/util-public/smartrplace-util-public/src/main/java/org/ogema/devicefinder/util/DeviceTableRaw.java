package org.ogema.devicefinder.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
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

public abstract class DeviceTableRaw<T, R extends Resource> extends ObjectGUITablePage<T,R>  {
	public static final long DEFAULT_POLL_RATE = 5000;
	
	/** Unique ID for the table e.g. name of providing class*/
	protected abstract String id();
	
	/** Heading to be shown over the table*/
	protected abstract String getTableTitle();

	//protected abstract String getHeader(); // {return "Smartrplace Hardware InstallationApp";}
	//protected final InstalledAppsSelector appSelector;
	
	public DeviceTableRaw(WidgetPage<?> page, ApplicationManager appMan, Alert alert,
			T initSampleObject) {
		super(page, appMan, null, initSampleObject, null, false, true, alert);
	}

	public Button addDeleteButton(ObjectResourceGUIHelper<T, R> vh, String id,
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
	public void addWidgetsAboveTable() {
		Header headerWinSens = new Header(page, WidgetHelper.getValidWidgetId("header_"+id()), getTableTitle());
		headerWinSens.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_CENTERED);
		page.append(headerWinSens);
	}

	@Override
	protected void addWidgetsBelowTable() {
	}
	
	@Override
	public R getResource(T object, OgemaHttpRequest req) {
		return null;
	}
	
	protected void addInstallationStatus(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
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
	
	protected void addComment(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		vh.stringEdit("Comment", id, object.installationComment(), row, alert);
	}
	
	protected void addRoomWidget(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		Map<Room, String> roomsToSet = new HashMap<>();
		List<Room> rooms = appMan.getResourceAccess().getResources(Room.class);
		for(Room room: rooms) {
			roomsToSet.put(room, ResourceUtils.getHumanReadableShortName(room));
		}
		vh.referenceDropdownFixedChoice("Room", id, deviceRoom, row, roomsToSet, 3);
	}
	
	protected void addSubLocation(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		vh.stringEdit("Location", id, object.installationLocation(), row, alert);
	}
	
	protected IntegerResource addRSSI(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		IntegerResource source = ResourceHelper.getSubResourceOfSibbling(object.device().getLocationResource(),
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiDevice", IntegerResource.class);
		vh.intLabel("RSSI", id, source, row, 0);
		return source;
	}
		
	protected Label addBattery(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom,
			FloatResource batteryReading) {
		return vh.floatLabel("Battery", id, batteryReading, row, "%.1f#min:0.1");
	}
	
	protected Label addLastContact(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
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

	public String getName(InstallAppDevice object) {
		final Resource device;
		device = object.device();
		final String name;
		if(device.getLocation().toLowerCase().contains("homematic")) {
			name = "WindowSens HM:"+ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
		} else {
			// resolve reference here, otherwise we'd just get "device"
			int idx = device.getLocation().lastIndexOf('/');
			if(idx < 0)
				idx = 0;
			else
				idx++;
			name = device.getLocation().substring(idx);
			//name = device.getLocation().replaceAll(".*/([^/])", "");
		}
		return name;
	}
	public Resource addNameWidgetRaw(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		
		final String name;
		if(req == null)
			name = "initResName"; //ResourceHelper.getSampleResource(getResourceType());
		else
			name = getName(object);
		vh.stringLabel("Name", id, name, row);
		
		final Resource device;
		device = object.device();
		return device;
	}
	

}
