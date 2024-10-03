package org.ogema.devicefinder.util;

import java.io.ObjectInputStream.GetField;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.SubcustomerUtil;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.api.DocumentationLinkProvider;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmDevice;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmInterfaceInfo;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmLogicInterface;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.devices.buildingtechnology.AirConditioner;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.devices.generators.PVPlant;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.devices.sensoractordevices.SensorDeviceLabelled;
import org.ogema.model.devices.sensoractordevices.SingleSwitchBox;
import org.ogema.model.devices.storage.ElectricityStorage;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.DoorWindowSensor;
import org.ogema.model.sensors.ElectricVoltageSensor;
import org.ogema.model.sensors.EnergyAccumulatedSensor;
import org.ogema.model.sensors.VolumeAccumulatedSensor;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.virtual.device.config.VirtualThermostatConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.external.accessadmin.config.SubCustomerData;
import org.smartrplace.tissue.util.resource.GatewaySyncResourceService;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
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
import de.iwes.widgets.html.form.label.LabelData;
import de.iwes.widgets.resource.widget.dropdown.ResourceDropdown;

public abstract class DeviceTableRaw<T, R extends Resource> extends ObjectGUITablePage<T,R>  {
	public static final long DEFAULT_POLL_RATE = 5000;
	public static final String BATTERY_VOLTAGE_HEADER = "Battery Voltage (V)";
	
	/** Unique ID for the table e.g. name of providing class*/
	protected abstract String id();
	
	/** Heading to be shown over the table*/
	protected abstract String getTableTitleRaw();
	
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
		this(page, appMan, alert, initSampleObject, false);
	}
	
	public DeviceTableRaw(WidgetPage<?> page, ApplicationManagerPlus appMan, Alert alert,
			T initSampleObject, boolean hideIfEmpty) {
		super(page, appMan!=null?appMan.appMan():null, null, initSampleObject, null, false, true, alert, hideIfEmpty);
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
		headerWinSens = new Header(page, WidgetHelper.getValidWidgetId("header_"+id()), getTableTitleRaw());
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
	
	public static Map<String, String> valuesToSetInstall = new HashMap<>();
	static  {
		valuesToSetInstall.put("0", "unknown");
		valuesToSetInstall.put("1", "SerialNumberRecorded: Teach-in process alsofinished");
		valuesToSetInstall.put("3", "PackedForShipping");
		valuesToSetInstall.put("5", "Shipping: Parcel handed over to delivery service");
		valuesToSetInstall.put("7", "AtCustomerSite: Delivery confirmed");
		valuesToSetInstall.put("10", "Physical installation done");
		valuesToSetInstall.put("20", "Physical testing done: Installation including all on-site tests");
		valuesToSetInstall.put("30", "All configuration finished, device is in full operation");
		valuesToSetInstall.put("-10", "Error in physical installation and/or testing (explain in comment)");
		valuesToSetInstall.put("-20", "Error in configuration, device cannot be used/requires action for real usage");		
	}
	protected void addInstallationStatus(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row) {
		vh.dropdown("Status", id, object.installationStatus(), row, valuesToSetInstall);
	}
	
	protected void addComment(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row) {
		vh.stringEdit("Comment", id, object.installationComment(), row, alert);
	}
	
	public static Map<Room, String> roomsToSet = new HashMap<>();
	public static Map<SubCustomerData, String> tenantsToSet = new HashMap<>();
	private static long lastUpdate = -1;
	private static long lastUpdateTenant = -1;
	protected void addRoomWidget(ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		addRoomWidgetStatic(vh, id, req, row, appMan, deviceRoom);
	}
	public static void addRoomWidgetStatic(ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		addRoomWidgetStatic(vh, id, req, row, appMan, deviceRoom, "Room");
	}
	public static void addRoomWidgetStatic(ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom, String columnName) {
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
		final ResourceDropdown<Room> roomSelector = vh.referenceDropdownFixedChoice(columnName, id, deviceRoom, row, roomsToSet, 3);
		if (roomSelector != null)
			roomSelector.setDefaultMinWidth("7em");
	}
	
	public static void addTenantWidgetStatic(ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			PhysicalElement device) {
		SubCustomerData subc = device.location().getSubResource("tenant", SubCustomerData.class);
		addTenantWidgetStatic(vh, id, req, row, appMan, subc, "Tenant");
	}
	public static void addTenantWidgetStatic(ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			SubCustomerData deviceTenant) {
		addTenantWidgetStatic(vh, id, req, row, appMan, deviceTenant, "Tenant");
	}
	public static void addTenantWidgetStatic(ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			SubCustomerData deviceTenant, String columnName) {
		long now = appMan.getFrameworkTime();
		if(now - lastUpdateTenant > 10000) {
			List<SubCustomerData> allTenants = SubcustomerUtil.getSubcustomers(appMan);
			Map<SubCustomerData, String> tenantsToSetLoc = new HashMap<>();
			for(SubCustomerData tenant: allTenants) {
				tenantsToSetLoc.put(tenant, ResourceUtils.getHumanReadableShortName(tenant));
			}
			tenantsToSet = tenantsToSetLoc;
			lastUpdateTenant = now;
		}
		vh.referenceDropdownFixedChoice(columnName, id, deviceTenant, row, tenantsToSet, 3);
	}

	protected void addSubLocation(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row) {
		addSubLocationStatic(object, vh, id, req, row, alert);
	}
	public static void addSubLocationStatic(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, Alert alert) {
		vh.stringEdit("Location", id, object.installationLocation(), row, alert);
	}
	
	protected Label addBattery(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row,
			FloatResource batteryReading) {
		return vh.floatLabel(BATTERY_VOLTAGE_HEADER, id, batteryReading, row, "%.1f#min:0.1");
	}
	
	protected Label addLastContact(ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row,
			SingleValueResource valueReadingResource) {
		return addLastContact(null, vh, id, req, row, valueReadingResource);
	}
	protected Label addLastContact(String columnLabel, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, 
			SingleValueResource reading) {
		if(columnLabel == null)
			columnLabel = "Last Contact";
		Label lastContact = null;
		if(req != null) {
			lastContact = new LastContactLabel(reading, appMan, mainTable, WidgetHelper.getValidWidgetId(columnLabel)+id, req);
			row.addCell(WidgetHelper.getValidWidgetId(columnLabel), lastContact);
			return lastContact;
		} else
			vh.registerHeaderEntry(columnLabel);
		return null;
	}

	public static class AddBatteryVoltageResult {
		public AddBatteryVoltageResult(Label label, SingleValueResource reading) {
			this(label, reading, false);
		}
		public AddBatteryVoltageResult(Label label, SingleValueResource reading, boolean isSOC) {
			this.label = label;
			this.reading = reading;
			this.isSOC = isSOC;
		}
		public Label label;
		public SingleValueResource reading;
		public boolean isSOC;
	}
	protected AddBatteryVoltageResult addBatteryVoltage(ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row,
			PhysicalElement device2) {
		return addBatteryVoltage(vh, id, req, row, device2, BATTERY_VOLTAGE_HEADER);
	}
	public static AddBatteryVoltageResult addBatteryVoltage(ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row,
			PhysicalElement device2, String colHeader) {
		VoltageResource batteryVoltage = DeviceHandlerBase.getBatteryVoltage(device2);
		if(batteryVoltage != null) {
			AddBatteryVoltageResult result = new AddBatteryVoltageResult(vh.floatLabel(colHeader, id, batteryVoltage, row, "%.1f#min:0.1"),
					batteryVoltage);
			float val = batteryVoltage.getValue();
			BatteryEvalBase.addBatteryStyle(result.label, val, true, device2.getLocation(), req, false);
			return result;
		} else if(!device2.getLocation().contains("_cc")){
			FloatResource batSOC = device2.getSubResource("battery", ElectricityStorage.class).chargeSensor().reading();
			if(batSOC != null && batSOC.isActive()) {
				AddBatteryVoltageResult result = new AddBatteryVoltageResult(vh.floatLabel(colHeader, id, batSOC, row, "%.0f%%#fac:100"),
						batSOC, true);
				float val = batSOC.getValue();
				BatteryEvalBase.addBatteryStyle(result.label, val, true, device2.getLocation(), req, true);
				return result;
			}
		}
		if(req == null)
			vh.registerHeaderEntry(colHeader);
		return null;
	}
		
	protected AddBatteryVoltageResult addBatteryStatus(ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row,
			PhysicalElement device2) {
		BooleanResource batteryStatus = DeviceHandlerBase.getSubResourceOfSibblingOrDirectChildMaintenance(device2.getLocationResource(),
				"batteryLow", BooleanResource.class);
		if(batteryStatus != null) {
			AddBatteryVoltageResult result = new AddBatteryVoltageResult(vh.booleanLabel("Bat.Low", id, batteryStatus, row, 0), batteryStatus);
			boolean val = batteryStatus.getValue();
			if(val)
				result.label.addStyle(LabelData.BOOTSTRAP_RED, req);
			return result;
		} else if(req == null)
			vh.registerHeaderEntry("Bat.Low");
		return null;
	}
	
	public static String getName(InstallAppDevice object, ApplicationManagerPlus appManPlus) {
		String result = DatapointImpl.getDeviceLabel(object, null, appManPlus.dpService(), null);
		String devLoc = object.device().getLocation();
		if(DeviceTableBase.isHomematic(devLoc)) {
			String id = ScheduleViewerOpenButtonEval.getDeviceShortId(devLoc);
			if(!id.startsWith("?"))
				result += "-"+id;
		}
		return result;
		//return getNameForDevice(device, appManPlus.dpService());
	}
	@Deprecated //use DatapointImpl.getDeviceLabelPlus instead
	public static String getNameForDevice(Resource device, DatapointService dpService) {
		DatapointGroup dpDev = dpService.getGroup(device.getLocation());
		if(dpDev.label(null) != null)
			return dpDev.label(null);
		
		String subName = getSubNameForDevice(device, dpService);
		String name = getDeviceStdName(device) + subName;
		dpDev.setLabel(null, name);
		dpDev.setType("DEVICE");
		return name;		
	}
	public static String getSubNameForDevice(Resource device, DatapointService dpService) {
		if(device.getLocation().toLowerCase().contains("homematic")) {
			return	ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
		} else {
			// resolve reference here, otherwise we'd just get "device"
			int idx = device.getLocation().lastIndexOf('/');
			if(idx < 0)
				idx = 0;
			else
				idx++;
			String name = device.getLocation().substring(idx);
			if(name.equals("device"))
				return ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
			//name = device.getLocation().replaceAll(".*/([^/])", "");
			return name;
		}
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
		return getDeviceStdName(resType.getName(), model.getLocation(), getSubResInfo(model.getSubResources(false)));
		/*if(DoorWindowSensor.class.isAssignableFrom(resType))
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
		return resType.getSimpleName();*/
	}
	public static class SubResourceInfo {
		String resourceName;
		String resType;
		Resource res;
		public SubResourceInfo(String resourceName, String resType, Resource res) {
			this.resourceName = resourceName;
			this.resType = resType;
			this.res = res;
		}
	}
	public static List<SubResourceInfo> getSubResInfo(Resource parent) {
		return getSubResInfo(parent.getSubResources(false));
	}
	public static List<SubResourceInfo> getSubResInfo(Collection<Resource> ress) {
		List<SubResourceInfo> result = new ArrayList<>();
		for(Resource res: ress) {
			result.add(new SubResourceInfo(res.getName(), res.getResourceType().getName(), res));
		}
		return result ;
	}
	/** Get device standard name
	 * 
	 * @param resType
	 * @param resourceLocation
	 * @param subResources may be null, but then device may not be identified correctly. Information on
	 * 		direct sub resources of device (non-recursive)
	 * @return
	 */
	public static String getDeviceStdName(String resType, String resourceLocation, List<SubResourceInfo> subResources) {
		if(DoorWindowSensor.class.getName().equals(resType))
			return "WindowSens";
		if(Thermostat.class.getName().equals(resType))
			return "Thermostat";
		if(SensorDevice.class.getName().equals(resType))
			return getSensorDeviceStdName(resourceLocation, subResources);
		if(SingleSwitchBox.class.getName().equals(resType))
			return "SwitchBox";
		if(ElectricityConnectionBox.class.getName().equals(resType))
			return "ElectricMeter";
		if(AirConditioner.class.getName().equals(resType))
			return "AirConditioning";
		if(PVPlant.class.getName().equals(resType))
			return "PVPlant";
		String[] els = resType.split("\\.");
		if(els.length == 0)
			return resType;
		return els[els.length-1];
	}

	/** Get additional String that shall be added to subRoomLocation of datapoint
	 * Usually this is just the device installationLocation, but if the sensor type is not unique
	 * within the device (e.g. because a sensor type appears on each phase) then a sensor information
	 * has to be added to the device sublocation
	 * @param dpLocation
	 * @return
	 */
	public static String getDatapointSubname(String dpLocation) {
		dpLocation = DeviceTableBase.makeDeviceToplevel(dpLocation);
		if(dpLocation.toLowerCase().contains("vekin") && dpLocation.contains("/CH")) {
			String[] els = dpLocation.split("/CH", 2);
			if(els.length == 2) {
				int idx = els[1].indexOf("/");
				String nums;
				if(idx >= 0) {
					nums = els[1].substring(0, idx);
				} else
					nums = els[1];
				try {
					int num = Integer.parseInt(nums);
					return ""+num;
				} catch(NumberFormatException e) {}
			}
		}
		if((isEnergyServerDevice(dpLocation) || dpLocation.toLowerCase().startsWith("iota") || dpLocation.toLowerCase().startsWith("elmeterspm2x"))
				&& dpLocation.contains("/L")) {
			String[] els = dpLocation.split("/L", 2);
			if(els.length == 2) {
				int idx = els[1].indexOf("/");
				String nums;
				if(idx >= 0) {
					nums = els[1].substring(0, idx);
				} else
					nums = els[1];
				try {
					int num = Integer.parseInt(nums);
					return "L"+num;
				} catch(NumberFormatException e) {}
			}
			
		}
		if(dpLocation.toLowerCase().startsWith("elmeterspm2x")
				&& dpLocation.contains("/connection/voltageSensor")) {
			String[] els = dpLocation.split("/connection/voltageSensor", 2);
			if(els.length == 2) {
				int idx = els[1].indexOf("/");
				String nums;
				if(idx >= 0) {
					nums = els[1].substring(0, idx);
				} else
					nums = els[1];
				if(nums.length()>0 && nums.length()<4)
					return "L_"+nums;
			}
			
		}
		return null;
	}
	
	/////////////////////////////////////////////
	// SensorDevice Handling
	/////////////////////////////////////////////
	
	public static String getSensorDeviceStdName(String resourceLocation, List<SubResourceInfo> subResources) {
		// If more types of SensorDevices are supported in the future add detection here
		if(isWeatherStation(resourceLocation))
			return "WeatherStation";
		if(isTempHumSens(resourceLocation))
			return "TempHumSens";
		if(isFALorFALMOT(resourceLocation))
			return "FAL230";
		if(isOpenWeatherMapSensorDevice(resourceLocation, subResources))
			return "OpenWeatherMapSensor";
		if(isDimmerSensorDevice(resourceLocation))
			return "smartDimmer";
		if(isSmartProtectDevice(resourceLocation))
			return "smartProtectionDevice";
		if(isGasEnergyCamDevice(resourceLocation, subResources))
			return "EnergyCam";
		if(isHeatMeterDevice(resourceLocation, subResources))
			return "HeatMeter";
		if(isHeatCostAllocatorDevice(resourceLocation, subResources))
			return "HeatCostAllocator";
		if(isSmokeDetectorDevice(resourceLocation, subResources))
			return "SmokeDetector";
		if(isWiredMBusMasterDevice(resourceLocation, subResources))
			return "WiredMBusMaster";
		if(isWaterMeterDevice(resourceLocation, subResources))
			return "WaterMeter";
		return "SensorDevice";
	}
	
	public static boolean isTempHumSens(String resourceLocation) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		if(isWeatherStation(resourceLocation))
			return false;
		if(isFALorFALMOT(resourceLocation))
			return false;
		if(isPeoplePassingDetector(resourceLocation))
			return false;
		if(resourceLocation.contains("maintenanceChannelReadings"))
			return false;
		if(resourceLocation.toLowerCase().startsWith("homematic"))
			return true;
		return false;
	}
	
	/** In a relativeSetpointSystem thermostats exists that take setpoit values relative to
	 * a predefined setpoint, e.g. -3 to +3. Still it may be possible to also control the
	 * absolute setpoint, which currently is determined by the property org.smartrplace.apps.heatcontrol.relativesetpoints .
	 * The main thermostat is the one that is active based on the property. For non-relativeSetpointSystems the function
	 * always return false
	 * @param thermostat
	 * @return
	 */
	public static boolean isMainThermostatInRelativeSetpointSystem(PhysicalElement thermostat) {
		//TODO: Differentiate whether these setpoints are available and whether they are used or
		// absolute setpoints are used by roomcontrol
		if(!isRelativeSetpointSystem())
			return false;
		if(thermostat.getLocation().contains("_Leitwarte_")) {
			if(thermostat.getLocation().contains("knx_devices/"))
				return true;
			return false;
		}
		if(Boolean.getBoolean("org.smartrplace.apps.heatcontrol.relativesetpoints") &&
				thermostat.getLocation().contains("/RoomControls_") && thermostat.getName().equals("thermostat"))
			return true;
		if(Boolean.getBoolean("org.smartrplace.apps.heatcontrol.relativesetpoints") &&
				thermostat.getLocation().contains("/ISPRoomControls_")) {
			return true;
		}
		if((!Boolean.getBoolean("org.smartrplace.apps.heatcontrol.relativesetpoints")) &&
				(!(thermostat.getLocation().contains("/RoomControls_") && thermostat.getName().equals("thermostat"))))
			return true;
		return false;
	}
	public static boolean isRelativeSetpointSystem() {
		return Boolean.getBoolean("org.smartrplace.apps.heatcontrol.relativesetpointsystem");
	}
	
	/** Check if resource is inside a relative setpoint thermostat
	 * 
	 * @param thermostat path of resource to check, may be sub resource of thermostat
	 * @return
	 */
	public static boolean hasThermostatRelativeSetpoint(Resource thermostat) {
		//Simple variant
		//return Boolean.getBoolean("org.smartrplace.apps.heatcontrol.relativesetpoints");
		
		if(!Boolean.getBoolean("org.smartrplace.apps.heatcontrol.relativesetpoints"))
			return false;
		if(thermostat.getSubResource("hasAbsoluteSetpoint") != null) {
			BooleanResource isAbs = thermostat.getSubResource("hasAbsoluteSetpoint", BooleanResource.class);
			if(isAbs.isActive())
				return !isAbs.getValue();
			return true;
		}
		//if(thermostat.getLocation().contains("Leitwarte"))
		//	return false; //true;
		if(thermostat.getLocation().contains("/ISPRoomControls_"))
			return false;
		return true;
	}
	
	public static boolean isWeatherStation(String resourceLocation) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		if(resourceLocation.toLowerCase().startsWith("homematic") && resourceLocation.contains("HM_HM_WDS100_"))
			return true;
		return false;
	}
	
	public static boolean isFALorFALMOT(String resourceLocation) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		if(resourceLocation.toLowerCase().startsWith("homematic") && (
				resourceLocation.contains("FAL230_") || resourceLocation.contains("FAL24_") ||resourceLocation.contains("FALMOT_")))
			return true;
		return false;
	}

	public static boolean isPeoplePassingDetector(String resourceLocation) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		if(resourceLocation.toLowerCase().startsWith("homematic") && (
				resourceLocation.contains("/PASSAGE_DETECTOR_DIRECTION_TRANSMITTER_")))
			return true;
		return false;
	}

	public static boolean isDimmerSensorDevice(String resourceLocation) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		if(resourceLocation.toLowerCase().startsWith("vekin"))
			return true;
		return false;
	}
	public static boolean isSmartProtectDevice(String resourceLocation) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		if(resourceLocation.toLowerCase().startsWith("livy"))
			return true;
		return false;
	}
	public static int getUnsupportedOfType(Collection<SubResourceInfo> subResources, String type) {
		int count = 0;
		for(SubResourceInfo srinfo: subResources) {
			if(type != null && (!type.equals(srinfo.resType))) {
				continue;
			}
			if(srinfo.resourceName.startsWith("unsupported_"))
				count++;
		}
		return count;
	}
	public static boolean isGasEnergyCamDevice(String resourceLocation, Collection<SubResourceInfo> subResources) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		if(subResources == null)
			return false;
		if(!(resourceLocation.toLowerCase().startsWith("jmbus") || resourceLocation.toLowerCase().startsWith("JMBUS_BASE")
				|| resourceLocation.startsWith("serverMirror")))
			return false;
		if(isHeatMeterDevice(resourceLocation, subResources))
			return false;
		if(isHeatCostAllocatorDevice(resourceLocation, subResources))
			return false;
		if(DeviceTableRaw.isSmokeDetectorDevice(resourceLocation, subResources))
			return false;
		if(isWiredMBusMasterDevice(resourceLocation, subResources))
			return false;
		int unsup = getUnsupportedOfType(subResources, TimeResource.class.getName());
		if(unsup != 1)
			return false;
		boolean foundVolume = false;
		for(SubResourceInfo srinfo: subResources) {
			if(VolumeAccumulatedSensor.class.getName().equals(srinfo.resType)) {
				foundVolume = true;
				break;
			}
		}
		return foundVolume;
	}
	public static boolean isWaterMeterDevice(String resourceLocation, Collection<SubResourceInfo> subResources) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		if((resourceLocation.toLowerCase().startsWith("jmbus") || resourceLocation.startsWith("MBusReadings"))
				&& (resourceLocation.contains("/WATER_METER_")
						|| resourceLocation.contains("/WARM_WATER_METER_")))
			return true;
		if(subResources == null)
			return false;
		//do not accept if subResource size fits GasEnergyCam
		boolean isOther = false;
		if(resourceLocation.startsWith("MBusReadings")
				&& (resourceLocation.contains("/OTHER_"))) {
			isOther = true;
		} else if(!(resourceLocation.toLowerCase().startsWith("jmbus") || resourceLocation.toLowerCase().startsWith("JMBUS_BASE")
				|| resourceLocation.startsWith("serverMirror")))
			return false;
		for(SubResourceInfo srinfo: subResources) {
			if(srinfo.resourceName.equals("mBusType") && StringResource.class.getName().equals(srinfo.resType)) {
				if(srinfo.res != null && ((StringResource)srinfo.res).getValue().equals("WATER_METER"))
					return true;
				else
					break;
			}
		}
		if(isHeatMeterDevice(resourceLocation, subResources))
			return false;
		if(!isOther) {
			int unsup = getUnsupportedOfType(subResources, TimeResource.class.getName());
				if(unsup == 1)
					return false;
		}
		boolean foundVolume = false;
		for(SubResourceInfo srinfo: subResources) {
			if(VolumeAccumulatedSensor.class.getName().equals(srinfo.resType)) {
				foundVolume = true;
				break;
			}
		}
		return foundVolume;
	}

	public static boolean isHeatMeterDevice(String resourceLocation, Collection<SubResourceInfo> subResources) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		if(subResources == null)
			return false;
		if(!(resourceLocation.toLowerCase().startsWith("jmbus")
				|| resourceLocation.startsWith("MBusReadings")
				||resourceLocation.startsWith("serverMirror")))
			return false;
		if(isHeatCostAllocatorDevice(resourceLocation, subResources))
			return false;
		boolean foundEnergy = false;
		for(SubResourceInfo srinfo: subResources) {
			if(EnergyAccumulatedSensor.class.getName().equals(srinfo.resType)) {
				foundEnergy = true;
				break;
			}
		}
		return foundEnergy;
	}
	
	public static boolean isHeatCostAllocatorDevice(String resourceLocation, Collection<SubResourceInfo> subResources) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		if(!(resourceLocation.toLowerCase().startsWith("jmbus") || resourceLocation.startsWith("MBusReadings")))
			return false;
		if(resourceLocation.contains("/HEAT_COST_ALLOCATOR_"))
			return true;
		for(SubResourceInfo srinfo: subResources) {
			if(srinfo.resourceName.equals("hcaEnergy")) {
				return true;
			}
		}
		return false;
	}
	
	public static boolean isSmokeDetectorDevice(String resourceLocation, Collection<SubResourceInfo> subResources) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		if(resourceLocation.startsWith("MBusReadings") && resourceLocation.contains("/SMOKE_DETECTOR_"))
			return true;
		return false;
	}
	
	public static boolean isWiredMBusMasterDevice(String resourceLocation, Collection<SubResourceInfo> subResources) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		if(resourceLocation.startsWith("MBusReadings") && resourceLocation.contains("/BUS_SYSTEM_COMPONENT_"))
			return true;
		return false;
	}
	
	public static boolean isJMbusRepeater(String resourceLocation, Collection<SubResourceInfo> subResources) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		if(!(resourceLocation.toLowerCase().startsWith("jmbus") || resourceLocation.startsWith("MBusReadings")))
			return false;
		if(subResources == null)
			return false;
		for(SubResourceInfo srinfo: subResources) {
			if(srinfo.resourceName.equals("mBusType") && StringResource.class.getName().equals(srinfo.resType)) {
				if(srinfo.res != null && ((StringResource)srinfo.res).getValue().equals("UNIDIRECTION_REPEATER"))
					return true;
				else
					break;
			}
		}
		if(isHeatMeterDevice(resourceLocation, subResources))
			return false;
		if(isWaterMeterDevice(resourceLocation, subResources))
			return false;
		if(isCO2wMBUSDevice(resourceLocation, subResources))
			return false;
		if(isWiredMBusMasterDevice(resourceLocation, subResources))
			return false;
		if(isHeatCostAllocatorDevice(resourceLocation, subResources))
			return false;
		if(isGasEnergyCamDevice(resourceLocation, subResources))
			return false;
		boolean foundVoltage = false;
		for(SubResourceInfo srinfo: subResources) {
			if(ElectricVoltageSensor.class.getName().equals(srinfo.resType)) {
				foundVoltage = true;
				break;
			}
		}
		return foundVoltage;
	}

	public static boolean isFaultMessageDevice(String resourceLocation, Collection<SubResourceInfo> subResources) {
		if((resourceLocation.toLowerCase().contains("/errors_")||resourceLocation.contains("/faults_")))
			return true;
		return false;
	}
	
	//TODO: Add device handler for this type
	public static boolean isCO2wMBUSDevice(String resourceLocation, Collection<SubResourceInfo> subResources) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		if(subResources == null)
			return false;
		//do not accept if subResource size fits GasEnergyCam
		if((!resourceLocation.toLowerCase().startsWith("jmbus") || resourceLocation.toLowerCase().startsWith("JMBUS_BASE")
				||resourceLocation.startsWith("serverMirror")))
			return false;
		if(isHeatMeterDevice(resourceLocation, subResources))
			return false;
		if(isHeatCostAllocatorDevice(resourceLocation, subResources))
			return false;
		if(DeviceTableRaw.isSmokeDetectorDevice(resourceLocation, subResources))
			return false;
		if(isWiredMBusMasterDevice(resourceLocation, subResources))
			return false;
		int unsup = getUnsupportedOfType(subResources, TimeResource.class.getName());
		if(unsup  > 0)
			return false;
		boolean foundUserDefined = false;
		for(SubResourceInfo srinfo: subResources) {
			if(srinfo.resourceName.equals("USER_DEFINED_0_0")) {
				foundUserDefined = true;
				break;
			}
		}
		return foundUserDefined;
	}
	
	public static boolean isOpenWeatherMapSensorDevice(String resourceLocation, Collection<SubResourceInfo> subResources) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		return resourceLocation.startsWith("BigBlueRoom") || resourceLocation.startsWith("WeatherData");
	}
	
	public static boolean isHAPDevice(String resourceLocation, Collection<SubResourceInfo> subResources) {
		return resourceLocation.contains("maintenanceChannelReadings");
	}

	/** For {@link ElectricityConnectionBox} devices*/
	public static boolean isEnergyServerDevice(String resourceLocation) {
		resourceLocation = DeviceTableBase.makeDeviceToplevel(resourceLocation);
		if(resourceLocation.startsWith("EnergyServerReadings_ESE/ESE_"))
			return true;
		return false;
	}
	public static boolean isHMIECDevice(String resourceLocation) {
		if(resourceLocation.contains("HM_HM_ES_TX_WM"))
			return true;
		return false;
	}
	
	public static boolean isElectricityMeterESI(SensorDeviceLabelled model) {
		if(model.getLocation().contains("HM_HmIP_ESI_") && model.mainSensorTitle().getValue().equals("Energy")
				&& model.getLocation().endsWith("_2"))
			return true;
		return false;
	}
	public static boolean isGasMeterESI(SensorDeviceLabelled model) {
		/* We only accept the volume sensor here. The sensor "Gas Flow" is added in the DeviceHandler*/
		if(model.getLocation().contains("HM_HmIP_ESI_") && model.mainSensorTitle().getValue().equals("Gas Volume"))
			return true;
		return false;
	}
	public static boolean isSubMeterESINonOwnDevice(SensorDeviceLabelled model) {
		if(model.getLocation().contains("HM_HmIP_ESI_")
				&& model.mainSensorTitle().getValue().equals("Gas Flow"))
			return true;
		if(model.getLocation().contains("HM_HmIP_ESI_") && (!model.mainSensorTitle().getValue().contains("Gas"))
				&& !model.getLocation().endsWith("_2"))
			return true;
		return false;
	}	
	public static boolean isLoraGw(SensorDeviceLabelled model) {
		if(model.getLocation().contains("chirpstack") && model.mainSensorTitle().getValue().equals("Last event"))
			return true;
		return false;
	}
	public static boolean isLoraDevice(PhysicalElement device) {
		return device.getLocation().contains("chirpstack");
	}

	//public enum ControlMode { COOLING, HEATING, NONE; }
    public static final int CTRLMODE_NONE = 0;
    public static final int CTRLMODE_COOLING = 1;
    public static final int CTRLMODE_HEATING = 2;
    public static final int CTRLMODE_COOLING_FORCED = 3; //means FORCED_OROFF
    public static final int CTRLMODE_HEATING_FORCED = 4; //means FORCED_OROFF
    public static final int CTRLMODE_OFF = 5;
    public static final int CTRLMODE_ECO = 6;
    public static final int CTRLMODE_COOLING_ORECO = 7; //means FORCED_ORECO
    public static final int CTRLMODE_HEATING_ORECO = 8; //means FORCED_ORECO
	public static final long BLOCKSHIFTUNTIL_DURATION = Long.getLong("org.ogema.devicefinder.util.blockshiftuntil.duration", 60*TimeProcUtil.MINUTE_MILLIS);

    public static boolean isVirtualThermostat(Thermostat model) {
		return !(model.getSubResources(VirtualThermostatConfig.class, false).isEmpty());
	}
	public static VirtualThermostatConfig getVirtualThermostatConfig(Thermostat model) {
		List<VirtualThermostatConfig> resultPlus = model.getSubResources(VirtualThermostatConfig.class, false);
		if(resultPlus.isEmpty())
			return null;
		if(resultPlus.size() > 1)
			throw new IllegalStateException("More than one VirtualThermostatConfig for "+model.getLocation());
		return resultPlus.get(0);
	}
	
	public static boolean isWallThermostat(String resourceLocation) {
		if(resourceLocation.contains("HM_HmIP_WTH_"))
			return true;
		if(resourceLocation.contains("HM_HmIP_BWTH_"))
			return true;
		if(resourceLocation.contains("HM_HM_TC_IT_WM_W_EU"))
			return true;
		return false;
	}
	public static boolean isCLThermostat(String resourceLocation) {
		if(resourceLocation.contains("HmIP_eTRV_CL_"))
			return true;
		return false;
	}

	public static boolean isCO2SensorHm(String resourceLocation) {
		if(resourceLocation.contains("HM_HmIP_SCTH230"))
			return true;
		return false;
	}

	public static boolean isKnownOnOffWithEventPushButtonDevice(Resource parent) {
		if(parent != null && (parent.getName().contains("HM_HmIP_BS2_") ||
				parent.getName().contains("HmIP_PSM_2_") ||
				parent.getName().contains("HM_HmIP_BSM_") ||
				parent.getName().contains("HM_HmIP_BSL_")))
			return true;
		return false;
	}

	/** Thermostat location -> next check time*/
	@Deprecated
	public static final Map<String, Long> nextDecalcShiftCheck = new HashMap<>();
	public static final Map<String, Timer> deCalcTimer = new HashMap<>();
	public static String setDecalcTimeForwardMax(Thermostat device, long now, GatewaySyncResourceService gwSync) {
		//long destTime = now+6*TimeProcUtil.DAY_MILLIS+6*TimeProcUtil.HOUR_MILLIS;
		long startOfDay = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.DAY);
		int decalcMinOfDay = Integer.getInteger("org.ogema.devicefinder.util.decalcMinutesOfDay", 360);
		long destTime = startOfDay + decalcMinOfDay*TimeProcUtil.MINUTE_MILLIS;
		if(destTime > now)
			destTime += 6*TimeProcUtil.DAY_MILLIS;
		return setDecalcTime(device, destTime, gwSync);												
	}
	
	public static String setDecalcNow(Thermostat device, long now, GatewaySyncResourceService gwSync) {
		String result = DeviceTableRaw.setDecalcTime(device, now+5*TimeProcUtil.MINUTE_MILLIS, gwSync);	
		DeviceHandlerBase.blockShiftingUntil = now + BLOCKSHIFTUNTIL_DURATION;
		TimeResource requestCalc = device.valve().getSubResource(DeviceHandlerBase.REQUEST_LAST_DECALC_RESNAME, TimeResource.class);
		ValueResourceHelper.setCreate(requestCalc, DeviceHandlerBase.blockShiftingUntil-1000);
		return result;
	}
	
	public static String setDecalcTimeForNextDay(Thermostat device, long now, GatewaySyncResourceService gwSync) {
		//long destTime = now+6*TimeProcUtil.DAY_MILLIS+6*TimeProcUtil.HOUR_MILLIS;
		long startOfDay = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.DAY);
		long destTime = startOfDay + 26*TimeProcUtil.HOUR_MILLIS;
		return setDecalcTime(device, destTime, gwSync);												
	}
	
	public static String setDecalcTime(Thermostat device, long destTime, GatewaySyncResourceService gwSync) {
		StringResource res = device.valve().getSubResource("DECALCIFICATION", StringResource.class);
		boolean isMonthly = isLoraDevice(device);
		String val = getDecalcString(destTime, isMonthly);
		if(val != null) {
			setCreate(res, val, gwSync);
			System.out.println("Setting Decalc Time with gwSync set:"+(gwSync != null));
			//ValueResourceHelper.setCreate(res, val);
		}
		return val;
	}
	
	public static String[] dayOfWeekStr = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
	public static String[] weekOfMonthStr = {"FIRST_WEEK", "SECOND_WEEK", "THIRD_WEEK", "FOURTH_WEEK", "LAST_WEEK"};
	public static String getDecalcString(long destTime, boolean isMonthly) {
		String base = getDecalcString(destTime);
		if(!isMonthly)
			return base;
		long curMonthStart = AbsoluteTimeHelper.getIntervalStart(destTime, AbsoluteTiming.MONTH);
		long timeInMonth = destTime - curMonthStart;
		long weekOfMonthIdx = timeInMonth / (7*TimeProcUtil.DAY_MILLIS);
		if(weekOfMonthIdx > 4)
			weekOfMonthIdx = 4;
		String result = weekOfMonthStr[(int) weekOfMonthIdx];
		return base + " "+result;
	}
	public static String getDecalcString(long destTime) {
		long curWeekStart = AbsoluteTimeHelper.getIntervalStart(destTime, AbsoluteTiming.WEEK);
		long timeInWeek = destTime - curWeekStart;
		long dayOfWeekIdx = timeInWeek / TimeProcUtil.DAY_MILLIS;
		if(dayOfWeekIdx > 6)
			dayOfWeekIdx = 6;
		String result = dayOfWeekStr[(int) dayOfWeekIdx];
		
		long inDayTime = timeInWeek % TimeProcUtil.DAY_MILLIS;
		StringFormatHelper.getTimeOfDayInLocalTimeZone(destTime);
		long hourOfDay = inDayTime / TimeProcUtil.HOUR_MILLIS;
		if(hourOfDay > 23)
			hourOfDay = 23;
		long inHourTime = inDayTime - hourOfDay*TimeProcUtil.HOUR_MILLIS;
		if(inHourTime == 0)
			result += " "+String.format("%02d", hourOfDay)+":00";
		else if(inHourTime <= 30*TimeProcUtil.MINUTE_MILLIS)
			result += " "+String.format("%02d", hourOfDay)+":30";
		else if(hourOfDay == 23) {
			dayOfWeekIdx++;
			if(dayOfWeekIdx > 6)
				dayOfWeekIdx = 0;
			result = dayOfWeekStr[(int) dayOfWeekIdx]+" "+String.format("%02d", hourOfDay)+":30";
		} else {
			result += " "+String.format("%02d", hourOfDay+1)+":00";
		} return result;
	}
	
	public static long getNextDecalcTime(Thermostat device, long now) {
		StringResource res = device.valve().getSubResource("DECALCIFICATION_FEEDBACK", StringResource.class);
		if(!res.isActive())
			return 0;
		return getNextDecalcTime(res.getValue(), now);
	}
	
	public static long getNextDecalcTime(String deCalcString, long now) {
		long timeInWeek = getDecalcTimeInWeek(deCalcString);
		return getNextDecalcTime(timeInWeek, now);
		/*if(timeInWeek < 0)
			return 0;
		long nowStartOfWeek = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.WEEK);
		long result = nowStartOfWeek + timeInWeek;
		if(result <= now)
			return result + 7*TimeProcUtil.DAY_MILLIS;
		else
			return result;
		
		String[] daySplit = deCalcString.split("\\s+");
		if(daySplit.length != 2)
			return 0;
		int dayOfWeekIdx = 0;
		boolean found = false;
		for(String dayStr: dayOfWeekStr) {
			if(dayStr.equals(daySplit[0])) {
				found = true;
				break;
			}
			dayOfWeekIdx++;
		}
		if(!found)
			return 0;
		String[] hourSplit = daySplit[1].split(":");
		if(hourSplit.length != 2)
			return 0;
		try {
			int hours = Integer.parseInt(hourSplit[0]);
			int minutes = Integer.parseInt(hourSplit[1]);
			long timeInWeek = dayOfWeekIdx*TimeProcUtil.DAY_MILLIS + hours*TimeProcUtil.HOUR_MILLIS + minutes*TimeProcUtil.MINUTE_MILLIS;
			
			long nowStartOfWeek = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.WEEK);
			long result = nowStartOfWeek + timeInWeek;
			if(result <= now)
				return result + 7*TimeProcUtil.DAY_MILLIS;
			else
				return result;
		} catch(NumberFormatException e) {
			return 0;
		}*/
	}
	
	public static long getNextDecalcTime(long timeInWeek, long now) {
		if(timeInWeek < 0)
			return 0;
		long nowStartOfWeek = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.WEEK);
		long result = nowStartOfWeek + timeInWeek;
		if(result <= now)
			return result + 7*TimeProcUtil.DAY_MILLIS;
		else
			return result;
	}
	
	public static long getDecalcTimeInWeek(String deCalcString) {
		String[] daySplit = deCalcString.split("\\s+");
		if(daySplit.length != 2)
			return -1;
		int dayOfWeekIdx = 0;
		boolean found = false;
		for(String dayStr: dayOfWeekStr) {
			if(dayStr.equals(daySplit[0])) {
				found = true;
				break;
			}
			dayOfWeekIdx++;
		}
		if(!found)
			return -1;
		String[] hourSplit = daySplit[1].split(":");
		if(hourSplit.length != 2)
			return -1;
		try {
			int hours = Integer.parseInt(hourSplit[0]);
			int minutes = Integer.parseInt(hourSplit[1]);
			return dayOfWeekIdx*TimeProcUtil.DAY_MILLIS + hours*TimeProcUtil.HOUR_MILLIS + minutes*TimeProcUtil.MINUTE_MILLIS;
		} catch(NumberFormatException e) {
			return -1;
		}		
	}

	/** 
	 * 
	 * @param object
	 * @param devHand may be null, in this case we just check if a non-empty value is given for the handler id
	 * @return
	 */
	public static boolean isTemplate(InstallAppDevice object, DeviceHandlerProviderDP<?> devHand) {
		if(devHand == null)
			return  object.isTemplate().isActive() && (!object.isTemplate().getValue().isEmpty());
		final boolean isTemplate = object.isTemplate().isActive() && object.isTemplate().getValue().equals(devHand.id());
		return isTemplate;
	}
	
	public static void setTemplateStatus(InstallAppDevice object, boolean newStatus) {
		if(newStatus) {
			ValueResourceHelper.setCreate(object.isTemplate(), object.devHandlerInfo().getValue());
			if(!object.isTemplate().isActive())
				object.isTemplate().activate(false);
		} else if(object.isTemplate().exists()) {
			object.isTemplate().deactivate(false);
			object.isTemplate().setValue("");
		}
	}
	
	public static InstallAppDevice getTemplateForType(Collection<InstallAppDevice> collection,
			DeviceHandlerProvider<?> devHand) {
		for(InstallAppDevice dev: collection) {
			if(isTemplate(dev, devHand))
				return dev;
		}
		return null;
	}
	
	public static String getSubNameAfterSeparator(Resource valve, int ch_separator) {
		int idx = valve.getName().lastIndexOf(ch_separator);
		if((idx < 0) || (idx >= (valve.getName().length()-1)))
			return valve.getName();
		else
			return valve.getName().substring(idx+1);		
	}
	
	public static void disableHardwareForDevice(InstallAppDevice object, GatewaySyncResourceService gwSync, boolean alsoDeleteFromHw) {
		int destVal = alsoDeleteFromHw?3:2;
		Resource device = object.device().getLocationResource();
		IntegerResource disableStatus = device.getSubResource("disableStatus", IntegerResource.class);
		setCreate(disableStatus, destVal, gwSync);
	}
	
	public static void deleteDevice(InstallAppDevice object, GatewaySyncResourceService gwSync) {
		Resource device = object.device().getLocationResource();
		deleteDeviceBase(device, gwSync);
		object.delete();	
	}
	
	public static void deleteDeviceBase(Resource device, GatewaySyncResourceService gwSync) {
		if(DeviceTableBase.isHomematic(device.getLocation())) {
			Resource parent = device.getParent();
			if(parent != null && ((parent instanceof HmDevice) ||
					((device instanceof HmInterfaceInfo) && (parent instanceof HmLogicInterface))))
				deleteResource(parent, gwSync);
			else
				deleteResource(device, gwSync);
		} else
			deleteResource(device, gwSync);
	}
	
	public static void deleteResource(Resource device, GatewaySyncResourceService gwSync) {
		if(gwSync != null) {
			gwSync.delete(device.getLocationResource());
			System.out.println("Delete VIA SYNC: "+device.getLocation());
		} else {
			device.delete();		
		}
	}
	
	public static boolean setCreate(BooleanResource fres, boolean value, GatewaySyncResourceService gwSync) {
		if(!fres.exists()) {
			ValueResourceHelper.setCreate(fres,  value);
			if(gwSync != null)
				gwSync.create(fres);
			return true;
		} //else if(gwSync != null)
		//	gwSync.create(fres);
		fres.setValue(value);
		return false;
	}
	
	public static boolean setCreate(IntegerResource fres, int value, GatewaySyncResourceService gwSync) {
		if(!fres.exists()) {
			ValueResourceHelper.setCreate(fres,  value);
			if(gwSync != null)
				gwSync.create(fres);
			return true;
		} //else if(gwSync != null)
		//	gwSync.create(fres);
		fres.setValue(value);
		return false;
	}

	public static boolean setCreate(StringResource fres, String value, GatewaySyncResourceService gwSync) {
		if(!fres.exists()) {
			ValueResourceHelper.setCreate(fres,  value);
			if(gwSync != null) {
				gwSync.create(fres);
			}
			return true;
		} //else if(gwSync != null)
		//	gwSync.create(fres);
		fres.setValue(value);
		return false;
	}
}
