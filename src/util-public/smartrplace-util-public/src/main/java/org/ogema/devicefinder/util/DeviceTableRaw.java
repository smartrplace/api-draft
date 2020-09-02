package org.ogema.devicefinder.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DocumentationLinkProvider;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.devices.buildingtechnology.AirConditioner;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.devices.generators.PVPlant;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.devices.sensoractordevices.SingleSwitchBox;
import org.ogema.model.devices.storage.ElectricityStorage;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.DoorWindowSensor;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.html.form.label.Label;

public abstract class DeviceTableRaw<T, R extends Resource> extends ObjectGUITablePage<T,R>  {
	public static final long DEFAULT_POLL_RATE = 5000;
	
	/** Unique ID for the table e.g. name of providing class*/
	protected abstract String id();
	
	/** Heading to be shown over the table*/
	protected abstract String getTableTitle();
	
	protected DocumentationLinkProvider getDocLinkProvider() {
		return null;
	}

	protected Button getAddButton() {
		return null;
	}
	
	protected Header headerWinSens;
	protected final ApplicationManagerPlus appManPlus;
	
	//protected abstract String getHeader(); // {return "Smartrplace Hardware InstallationApp";}
	//protected final InstalledAppsSelector appSelector;
	
	public DeviceTableRaw(WidgetPage<?> page, ApplicationManagerPlus appMan, Alert alert,
			T initSampleObject) {
		super(page, appMan!=null?appMan.appMan():null, null, initSampleObject, null, false, true, alert);
		this.appManPlus = appMan;
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
		headerWinSens = new Header(page, WidgetHelper.getValidWidgetId("header_"+id()), getTableTitle());
		headerWinSens.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_LEFT);

		DocumentationLinkProvider docLinkProv = getDocLinkProvider();
		if(docLinkProv != null) {
			Button addButton = getAddButton();
			StaticTable topTable = new StaticTable(1, 4, new int[] {6, 2, 2, 2});
			RedirectButton docButtonPub = new RedirectButton(page, WidgetHelper.getValidWidgetId("docButtonPub_"+id()), "Driver Guide") {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					if(docLinkProv.getDriverDocumentationPageURL(true) != null)
						setWidgetVisibility(true, req);
					else
						setWidgetVisibility(false, req);
				}
				
				@Override
				public void onPrePOST(String data, OgemaHttpRequest req) {
					setUrl(docLinkProv.getDriverDocumentationPageURL(true), req);
				}
			};
			RedirectButton docButtonInternal = new RedirectButton(page, WidgetHelper.getValidWidgetId("docButtonInternal_"+id()), "Documentation Internal") {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					String user = GUIUtilHelper.getUserLoggedIn(req);
					//TODO: Check for Super-Admin level or similar
					if(user.equals("master") && docLinkProv.getDriverDocumentationPageURL(false) != null) {
						setWidgetVisibility(true, req);
					} else
						setWidgetVisibility(false, req);
				}
				
				@Override
				public void onPrePOST(String data, OgemaHttpRequest req) {
					setUrl(docLinkProv.getDriverDocumentationPageURL(false), req);
				}
			};
			topTable.setContent(0, 0, headerWinSens).setContent(0, 2, "<br>").setContent(0, 2, docButtonPub).
					setContent(0, 3, "<br>").setContent(0, 3, docButtonInternal);
			if(addButton != null)
				topTable.setContent(0, 1, "<br>").setContent(0, 1, addButton);
			page.append(topTable);
		} else
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
	
	private static Map<Room, String> roomsToSet = new HashMap<>();
	private static long lastUpdate = -1;
	protected void addRoomWidget(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		addRoomWidgetStatic(object, vh, id, req, row, appMan, deviceRoom);
	}
	public static void addRoomWidgetStatic(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		long now = appMan.getFrameworkTime();
		if(now - lastUpdate > 10000) {
			List<Room> allRooms = KPIResourceAccess.getRealRooms(appMan.getResourceAccess()); //.getResources(Room.class);
			Map<Room, String> roomsToSetLoc = new HashMap<>();
			for(Room room: allRooms) {
				roomsToSetLoc.put(room, ResourceUtils.getHumanReadableShortName(room));
			}
			roomsToSet = roomsToSetLoc;
			lastUpdate = now;
		}
		vh.referenceDropdownFixedChoice("Room", id, deviceRoom, row, roomsToSet, 3);
	}
	
	protected void addSubLocation(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		addSubLocationStatic(object, vh, id, req, row, appMan, deviceRoom, alert);
	}
	public static void addSubLocationStatic(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom, Alert alert) {
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
		return addLastContact("Last Contact", vh, id, req, row, valueReadingResource);
	}
	protected Label addLastContact(String columnLabel, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, 
			SingleValueResource reading) {
		Label lastContact = null;
		if(req != null) {
			lastContact = new LastContactLabel(reading, appMan, mainTable, "lastContact"+id, req);
			row.addCell(WidgetHelper.getValidWidgetId(columnLabel), lastContact);
			return lastContact;
		} else
			vh.registerHeaderEntry(columnLabel);
		return null;
	}

	public static class AddBatteryVoltageResult {
		public AddBatteryVoltageResult(Label label, SingleValueResource reading) {
			super();
			this.label = label;
			this.reading = reading;
		}
		public Label label;
		public SingleValueResource reading;
	}
	protected AddBatteryVoltageResult addBatteryVoltage(ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row,
			PhysicalElement device2) {
		if(device2.getSubResource("battery") != null) {
			ElectricityStorage bat = device2.getSubResource("battery",  ElectricityStorage.class);
			if(bat != null && bat.internalVoltage().exists()) {
				return new AddBatteryVoltageResult(vh.floatLabel("Battery", id, bat.internalVoltage().reading(), row, "%.1f#min:0.1"),
						bat.internalVoltage().reading());
			}
		}
		VoltageResource batteryVoltage = ResourceHelper.getSubResourceOfSibbling(device2,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "battery/internalVoltage/reading", VoltageResource.class);
		if(batteryVoltage != null)
			return new AddBatteryVoltageResult(vh.floatLabel("Battery", id, batteryVoltage, row, "%.1f#min:0.1"),
					batteryVoltage);
		else if(req == null)
			vh.registerHeaderEntry("Battery");
		return null;
	}
	protected AddBatteryVoltageResult addBatteryStatus(ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row,
			PhysicalElement device2) {
		BooleanResource batteryStatus = ResourceHelper.getSubResourceOfSibbling(device2,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "batteryLow", BooleanResource.class);
		if(batteryStatus != null)
			return new AddBatteryVoltageResult(vh.booleanLabel("Bat.Low", id, batteryStatus, row, 0), batteryStatus);
		else if(req == null)
			vh.registerHeaderEntry("Bat.Low");
		return null;
	}
	
	public static String getName(InstallAppDevice object, ApplicationManagerPlus appManPlus) {
		final Resource device;
		device = object.device();
		
		DatapointGroup dpDev = appManPlus.dpService().getGroup(device.getLocation());
		if(dpDev.label(null) != null)
			return dpDev.label(null);
		
		String name;
		if(device.getLocation().toLowerCase().contains("homematic")) {
			name = getDeviceStdName(device) + ":" +
					ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
		} else {
			// resolve reference here, otherwise we'd just get "device"
			int idx = device.getLocation().lastIndexOf('/');
			if(idx < 0)
				idx = 0;
			else
				idx++;
			name = device.getLocation().substring(idx);
			if(name.equals("device"))
				name = getDeviceStdName(device) +
					ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
			//name = device.getLocation().replaceAll(".*/([^/])", "");
		}
		dpDev.setLabel(null, name);
		dpDev.setType("DEVICE");
		return name;
	}
	public Resource addNameWidgetRaw(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		
		final String name;
		if(req == null)
			name = "initResName"; //ResourceHelper.getSampleResource(getResourceType());
		else
			name = getName(object, appManPlus);
		vh.stringLabel("Name", id, name, row);
		
		final Resource device;
		device = object.device();
		return device;
	}
	
	
	public static String getDeviceStdName(Resource model) {
		//switch(typeClassName) {
		Class<? extends Resource> resType = model.getResourceType();
		if(DoorWindowSensor.class.isAssignableFrom(resType))
			return "WindowSens";
		if(Thermostat.class.isAssignableFrom(resType))
			return "Thermostat";
		if(SensorDevice.class.isAssignableFrom(resType))
			return getSensorDeviceStdName((SensorDevice) model);
		if(SingleSwitchBox.class.isAssignableFrom(resType))
			return "SwitchBox";
		if(ElectricityConnectionBox.class.isAssignableFrom(resType))
			return "ElectricMeter";
		if(AirConditioner.class.isAssignableFrom(resType))
			return "AirConditioning";
		if(PVPlant.class.isAssignableFrom(resType))
			return "PVPlant";
		return resType.getSimpleName();
	}
	
	public static String getSensorDeviceStdName(SensorDevice model) {
		// If more types of SensorDevices are supported in the future add detection here
		if(isTempHumSens(model))
			return "TempHumSens";
		return "SensorDevice";
	}
	public static boolean isTempHumSens(SensorDevice model) {
		if(model.getLocation().toLowerCase().startsWith("homematic"))
			return true;
		//for(Sensor sens: model.getSubResources(Sensor.class, false)) {
		//	if(sens instanceof TemperatureSensor || sens instanceof HumiditySensor)
		//		return true;
		//}
		return false;
	}

	/** 
	 * 
	 * @param object
	 * @param devHand may be null, in this case we just check if a non-empty value is given for the handler id
	 * @return
	 */
	public static boolean isTemplate(InstallAppDevice object, DeviceHandlerProvider<?> devHand) {
		if(devHand == null)
			return  object.isTemplate().isActive() && (!object.isTemplate().getValue().isEmpty());
		final boolean isTemplate = object.isTemplate().isActive() && object.isTemplate().getValue().equals(devHand.id());
		return isTemplate;
	}
}
