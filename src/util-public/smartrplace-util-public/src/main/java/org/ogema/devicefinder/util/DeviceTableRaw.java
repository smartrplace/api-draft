package org.ogema.devicefinder.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DocumentationLinkProvider;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.devices.buildingtechnology.AirConditioner;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.devices.generators.PVPlant;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.devices.sensoractordevices.SingleSwitchBox;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.DoorWindowSensor;
import org.ogema.model.sensors.EnergyAccumulatedSensor;
import org.ogema.model.sensors.VolumeAccumulatedSensor;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.virtual.device.config.VirtualThermostatConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
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
	private static long lastUpdate = -1;
	protected void addRoomWidget(ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		addRoomWidgetStatic(vh, id, req, row, appMan, deviceRoom);
	}
	public static void addRoomWidgetStatic(ObjectResourceGUIHelper<?,?> vh, String id,
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
			OgemaHttpRequest req, Row row) {
		addSubLocationStatic(object, vh, id, req, row, alert);
	}
	public static void addSubLocationStatic(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, Alert alert) {
		vh.stringEdit("Location", id, object.installationLocation(), row, alert);
	}
	
	/*protected IntegerResource addRSSI(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row) {
		IntegerResource source = ResourceHelper.getSubResourceOfSibbling(object.device().getLocationResource(),
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiDevice", IntegerResource.class);
		vh.intLabel("RSSI", id, source, row, 0);
		return source;
	}*/
		
	protected Label addBattery(InstallAppDevice object, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row,
			FloatResource batteryReading) {
		return vh.floatLabel("Battery", id, batteryReading, row, "%.1f#min:0.1");
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
		/*if(device2.getSubResource("battery") != null) {
			ElectricityStorage bat = device2.getSubResource("battery",  ElectricityStorage.class);
			if(bat != null && bat.internalVoltage().exists()) {
				return new AddBatteryVoltageResult(vh.floatLabel("Battery", id, bat.internalVoltage().reading(), row, "%.1f#min:0.1"),
						bat.internalVoltage().reading());
			}
		}*/
		VoltageResource batteryVoltage = DeviceHandlerBase.getBatteryVoltage(device2); //ResourceHelper.getSubResourceOfSibbling(device2,
		//		"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "battery/internalVoltage/reading", VoltageResource.class);
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
		//final Resource device;
		//device = object.device();
		return DatapointImpl.getDeviceLabel(object, null, appManPlus.dpService(), null);
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
		public SubResourceInfo(String resourceName, String resType) {
			this.resourceName = resourceName;
			this.resType = resType;
		}
	}
	public static List<SubResourceInfo> getSubResInfo(Resource parent) {
		return getSubResInfo(parent.getSubResources(false));
	}
	public static List<SubResourceInfo> getSubResInfo(Collection<Resource> ress) {
		List<SubResourceInfo> result = new ArrayList<>();
		for(Resource res: ress) {
			result.add(new SubResourceInfo(res.getName(), res.getResourceType().getName()));
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
		if(isWaterMeterDevice(resourceLocation, subResources))
			return "WaterMeter";
		return "SensorDevice";
	}
	
	public static boolean isTempHumSens(String resourceLocation) {
		if(isWeatherStation(resourceLocation))
			return false;
		if(resourceLocation.contains("maintenanceChannelReadings"))
			return false;
		if(resourceLocation.toLowerCase().startsWith("homematic"))
			return true;
		return false;
	}
	public static boolean isWeatherStation(String resourceLocation) {
		if(resourceLocation.toLowerCase().startsWith("homematic") && resourceLocation.contains("HM_HM_WDS100_"))
			return true;
		return false;
	}
	
	public static boolean isDimmerSensorDevice(String resourceLocation) {
		if(resourceLocation.toLowerCase().startsWith("vekin"))
			return true;
		return false;
	}
	public static boolean isSmartProtectDevice(String resourceLocation) {
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
		if(subResources == null)
			return false;
		if(!(resourceLocation.toLowerCase().startsWith("jmbus")||resourceLocation.startsWith("serverMirror")))
			return false;
		if(isHeatMeterDevice(resourceLocation, subResources))
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
		if(subResources == null)
			return false;
		//do not accept if subResource size fits GasEnergyCam
		if((!resourceLocation.toLowerCase().startsWith("jmbus")||resourceLocation.startsWith("serverMirror")))
			return false;
		if(isHeatMeterDevice(resourceLocation, subResources))
			return false;
		int unsup = getUnsupportedOfType(subResources, TimeResource.class.getName());
		if(unsup == 1)
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
	public static boolean isHeatMeterDevice(String resourceLocation, Collection<SubResourceInfo> subResources) {
		if(subResources == null)
			return false;
		if(!(resourceLocation.toLowerCase().startsWith("jmbus")||resourceLocation.startsWith("serverMirror")))
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
	
	//TODO: Add device handler for this type
	public static boolean isCO2wMBUSDevice(String resourceLocation, Collection<SubResourceInfo> subResources) {
		if(subResources == null)
			return false;
		//do not accept if subResource size fits GasEnergyCam
		if((!resourceLocation.toLowerCase().startsWith("jmbus")||resourceLocation.startsWith("serverMirror")))
			return false;
		if(isHeatMeterDevice(resourceLocation, subResources))
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
		return resourceLocation.startsWith("BigBlueRoom") || resourceLocation.startsWith("WeatherData");
	}
	
	public static boolean isHAPDevice(String resourceLocation, Collection<SubResourceInfo> subResources) {
		return resourceLocation.contains("maintenanceChannelReadings");
	}

	/** For {@link ElectricityConnectionBox} devices*/
	public static boolean isEnergyServerDevice(String resourceLocation) {
		if(resourceLocation.startsWith("EnergyServerReadings_ESE/ESE_"))
			return true;
		return false;
	}
	
	public static boolean isVirtualThermostat(Resource model) {
		return !(model.getSubResources(VirtualThermostatConfig.class, false).isEmpty());
	}
	
	public static boolean isWallThermostat(String resourceLocation) {
		if(resourceLocation.contains("HM_HmIP_WTH_2"))
			return true;
		if(resourceLocation.contains("HM_HM_TC_IT_WM_W_EU"))
			return true;
		return false;
	}
	public static boolean isCO2SensorHm(String resourceLocation) {
		if(resourceLocation.contains("HM_HmIP_SCTH230"))
			return true;
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
	
	public static void setTemplateStatus(InstallAppDevice object, DeviceHandlerProvider<?> devHand, boolean newStatus) {
		if(newStatus) {
			ValueResourceHelper.setCreate(object.isTemplate(), devHand.id());
			if(!object.isTemplate().isActive())
				object.isTemplate().activate(false);
		} else if(object.isTemplate().exists()) {
			object.isTemplate().deactivate(false);
			object.isTemplate().setValue("");
		}
	}
	
	public static InstallAppDevice getTemplateForType(List<InstallAppDevice> devsOfhandler,
			DeviceHandlerProvider<?> devHand) {
		for(InstallAppDevice dev: devsOfhandler) {
			if(isTemplate(dev, devHand))
				return dev;
		}
		return null;
	}
}
