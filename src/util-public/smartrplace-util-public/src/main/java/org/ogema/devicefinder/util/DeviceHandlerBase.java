package org.ogema.devicefinder.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.schedule.AbsoluteSchedule;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.api.DriverPropertySuccessHandler;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.devicefinder.api.PatternListenerExtended;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmDevice;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmInterfaceInfo;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmLogicInterface;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.devices.buildingtechnology.ThermostatProgram;
import org.ogema.model.devices.storage.ElectricityStorage;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.GenericBinarySensor;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public abstract class DeviceHandlerBase<T extends PhysicalElement> implements DeviceHandlerProvider<T> {

	/** You have to provide a resource pattern to find the devices that shall be processed by the
	 * {@link DeviceHandlerProvider}. If there are also other DeviceHandlerProviders working on the
	 * same device ResourceType then you have to make sure that the {@link ResourcePattern#accept()} method
	 * of the patterns make sure that each device of the type is assigned to exactly one DeviceHandlerProvider.
	 */
	protected abstract Class<? extends ResourcePattern<T>> getPatternClass();

	protected PatternListenerExtended<ResourcePattern<T>, T> listener = null;
	public List<ResourcePattern<T>> getAllPatterns() {
		if(listener == null)
			return Collections.emptyList();
		return listener.getAllPatterns();
	}
	public ResourcePattern<T> getPattern(T device) {
		for(ResourcePattern<T> pat: getAllPatterns()) {
			if(pat.model.equalsLocation(device))
				return pat;
		}
		return null;
	}
	
	protected abstract ResourcePatternAccess advAcc();
	
	@Override
	public String id() {
		return this.getClass().getName();
	}

	@Override
	public String label(OgemaLocale locale) {
		return this.getClass().getSimpleName();
	}

	@Override
	public PatternListenerExtended<ResourcePattern<T>, T> addPatternDemand(
			InstalledAppsSelector app) {
		if(listener == null) {
			listener = new PatternListenerExtendedImpl<ResourcePattern<T>, T>(app, this);
		}
		advAcc().addPatternDemand(getPatternClass(), listener, AccessPriority.PRIO_LOWEST);
		return listener;
	}

	@Override
	public void removePatternDemand() {
		if(listener == null)
			return;
		advAcc().removePatternDemand(getPatternClass(), listener);	
	}
	
	public List<ResourcePattern<T>> getAppPatterns() {
		if(listener == null)
			return null;
		return listener.getAllPatterns();
	}
	
	public static Datapoint addDatapoint(SingleValueResource res, List<Datapoint> result, DatapointService dpService) {
		if(res != null && res.isActive()) {
			Datapoint dp = dpService.getDataPointStandard(res);
			result.add(dp);
			return dp;
		}
		return null;
	}
	public static Datapoint addDatapoint(SingleValueResource res, List<Datapoint> result, DatapointService dpService,
			boolean addSubLocationByResourceName) {
		if(addSubLocationByResourceName) {
			String subLocation = res.getName();
			return addDatapoint(res, result, subLocation, dpService);
		}
		return addDatapoint(res, result, dpService);
	}
	protected static Datapoint addDatapoint(SingleValueResource res, List<Datapoint> result,
			String subLocation, DatapointService dpService) {
		Datapoint dp = addDatapoint(res, result, dpService);
		if(dp != null && subLocation != null) {
			dp.addToSubRoomLocationAtomic(null, null, subLocation, false);
			/*synchronized(dp) {
				String existing = dp.getSubRoomLocation(null, null);
				if(existing != null && (!existing.isEmpty()) && (!existing.contains(subLocation)))
					subLocation = existing+"-"+subLocation;
				dp.setSubRoomLocation(null, null, subLocation);
			}*/
		}
		return dp;
	}
	
	public Datapoint addDatapoint(AbsoluteSchedule res, List<Datapoint> result,
			DatapointService dpService) {
		if(res.isActive()) {
			Datapoint dp = dpService.getDataPointStandard(res);
			result.add(dp);
			return dp;
		}
		return null;
	}

	/*@Override
	public String getDeviceName(InstallAppDevice installDeviceRes) {
		return DeviceTableRaw.getName(installDeviceRes);
	}*/
	
	protected void setInstallationLocation(InstallAppDevice device, String subLoc, DatapointService dpService) {
		ValueResourceHelper.setCreate(device.installationLocation(), subLoc);
	}
	
	public Collection<Datapoint> addtStatusDatapointsHomematic(PhysicalElement dev, DatapointService dpService,
			List<Datapoint> result) {
		return addtStatusDatapointsHomematic(dev, dpService, result, true);
	}
	
	public static VoltageResource getBatteryVoltage(PhysicalElement dev) {
		VoltageResource batVolt = dev.getSubResource("battery", ElectricityStorage.class).internalVoltage().reading();
		if(batVolt != null && batVolt.isActive())
			return batVolt;
		else {
			VoltageResource batteryVoltage = ResourceHelper.getSubResourceOfSibbling(dev.getLocationResource(),
					"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "battery/internalVoltage/reading", VoltageResource.class);
			return batteryVoltage;
		}		
	}
	
	public static IntegerResource getRSSIResource(PhysicalElement dev) {
		IntegerResource rssiDevice = ResourceHelper.getSubResourceOfSibbling(dev.getLocationResource(),
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiDevice", IntegerResource.class);
		if(rssiDevice != null && rssiDevice.exists())
			return rssiDevice;
		return null;
	}
	public static IntegerResource getRSSIPeerResource(PhysicalElement dev) {
		IntegerResource rssiPeer = ResourceHelper.getSubResourceOfSibbling(dev.getLocationResource(),
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiPeer", IntegerResource.class);
		if(rssiPeer != null && rssiPeer.exists())
			return rssiPeer;
		return null;
	}
	
	public Collection<Datapoint> addtStatusDatapointsHomematic(PhysicalElement dev, DatapointService dpService,
			List<Datapoint> result, boolean hasBattery) {
		dev = dev.getLocationResource();
		if(hasBattery) {
			VoltageResource batteryVoltage = getBatteryVoltage(dev);
			if(batteryVoltage != null)
				addDatapoint(batteryVoltage, result, dpService);
			/*VoltageResource batVolt = dev.getSubResource("battery", ElectricityStorage.class).internalVoltage().reading();
			if(batVolt.isActive())
				result.add(dpService.getDataPointStandard(batVolt));
			else {
				VoltageResource batteryVoltage = ResourceHelper.getSubResourceOfSibbling(dev,
						"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "battery/internalVoltage/reading", VoltageResource.class);
				if(batteryVoltage != null)
					addDatapoint(batteryVoltage, result, dpService);
			}*/
			BooleanResource batteryStatus = ResourceHelper.getSubResourceOfSibbling(dev,
					"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "batteryLow", BooleanResource.class);
			if(batteryStatus != null && batteryStatus.exists())
				addDatapoint(batteryStatus, result, dpService);
		}
		BooleanResource comDisturbed = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "communicationStatus/communicationDisturbed", BooleanResource.class);
		if(comDisturbed != null && comDisturbed.exists())
			addDatapoint(comDisturbed, result, dpService);
		IntegerResource errorCode = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "errorCode", IntegerResource.class);
		if(errorCode != null && errorCode.exists()) {
			addDatapoint(errorCode, result, dpService);			
		}
		BooleanResource configPending = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "configPending", BooleanResource.class);
		if(configPending != null && configPending.exists()) {
			addDatapoint(configPending, result, dpService);			
		}
		IntegerResource rssiDevice = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiDevice", IntegerResource.class);
		if(rssiDevice != null && rssiDevice.exists())
			addDatapoint(rssiDevice, result, dpService);
		IntegerResource rssiPeer = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiPeer", IntegerResource.class);
		if(rssiPeer != null && rssiPeer.exists())
			addDatapoint(rssiPeer, result, dpService);
		GenericBinarySensor dutyCycle = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "dutyCycle", GenericBinarySensor.class);
		if(dutyCycle != null && dutyCycle.reading().exists())
			addDatapoint(dutyCycle.reading(), result, dpService);
		return result;
	}
	
	/** Check if Auto-mode is allowed based on configPending
	 * 
	 * @return true if AutoMode is allowed for the thermostat, which will only be used if no ecoMode is active etc.
	 */
	public static boolean isAutoModeAllowed(Thermostat dev, HardwareInstallConfig hwConfig) {
		BooleanResource configPending = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "configPending", BooleanResource.class);
		return isAutoModeAllowed(dev, configPending, hwConfig.autoThermostatMode());
	}
	
	public static boolean performWeeklyPostpone(Thermostat dev,HardwareInstallConfig hwConfig) {
		if(hwConfig == null)
			return Boolean.getBoolean("org.smartrplace.homematic.devicetable.autostart.shiftdecalc");		
		int val = hwConfig.weeklyPostponeMode().getValue();
		if(val == 1)
			return false;
		if(val == 2)
			return true;
		return Boolean.getBoolean("org.smartrplace.homematic.devicetable.autostart.shiftdecalc");		
	}
	
	public static boolean isAutoModeAllowed(Thermostat dev,
			BooleanResource configPending, IntegerResource autoThermostatMode) {
		IntegerResource autoThermostatModeSingle = getAutoThermostatModeSingle(dev);
		int overallState = autoThermostatMode.getValue();
		if(overallState == 3)
			return false;
		int state = autoThermostatModeSingle.getValue();
		switch(state) {
		case 0:
			switch(overallState) {
			case 0:
				//evaluate configPending
				break;			
			case 1:
				return true;
			case 2:
				return false;
			}
			//evaluate configPending
			break;
		case 1:
			return true;
		case 2:
			return false;
		case 3:
			//evaluate configPending
			break;
		default:
			//Should never occur
			break;
		}
		if(configPending != null && configPending.exists() && configPending.getValue()) {
			return false;			
		}					
		return true;
	}
	public static IntegerResource getAutoThermostatModeSingle(Thermostat th) {
		return th.getSubResource("autoThermostatModeSingle", IntegerResource.class);
	}
	public static ThermostatProgram getHmThermProgram(Thermostat th) {
		ThermostatProgram hmThermProgram = th.getSubResource("program", ThermostatProgram.class);
		if ((hmThermProgram == null) || (!hmThermProgram.isActive()))
			return null;
		return hmThermProgram;
	}

	/** Get anchor resource for homematic property access
	 * 
	 * @param device
	 * @param channelName the resource name must start with this String
	 * @return
	 */
	public static Resource getAnchorResource(PhysicalElement device, String channelName) {
		Resource hmDevice = ResourceHelper.getFirstParentOfType(device, "org.ogema.drivers.homematic.xmlrpc.hl.types.HmDevice");
		if(hmDevice == null)
			return null;
		ResourceList<?> channels = hmDevice.getSubResource("channels", ResourceList.class);
		if(!channels.exists())
			return null;
		for(Resource res: channels.getAllElements()) {
			if(res.getName().startsWith(channelName))
				return res;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static void writePropertyHm(String propertyId, Resource propDev, String value,
			DriverPropertySuccessHandler<?> successHandler,
			OGEMADriverPropertyService<Resource> hmPropService,
			OgemaLogger logger) {
		if(propDev == null)
			return;
		//String propertyId = getPropId(propType);
		if(propertyId == null)
			return;
		hmPropService.writeProperty(propDev, propertyId , logger, value,
				(DriverPropertySuccessHandler<Resource>)successHandler);
		
	}

	public static class PropAccessDataHm {
		public Resource anchorRes;
		public String propId;
		
		public PropAccessDataHm(Resource anchorRes, String propId) {
			this.anchorRes = anchorRes;
			this.propId = propId;
		}
	}
	
	public static void setBuildingAsRoom(Datapoint dp, DatapointService dpService) {
		DPRoom room = dpService.getRoom(DPRoom.BUILDING_OVERALL_ROOM_LABEL);
		if(room != null)
			dp.setRoom(room);
		
	}
	
	/** From InstallationServiceHM */
	public static InstallAppDevice getOtherCCU(HmLogicInterface ccu, boolean isThisCCUCC, DatapointService dpService) {
		if(ccu == null)
			return null;
		String otherName = ccu.getName().substring(0, ccu.getName().length()-2)+(isThisCCUCC?"ip":"cc");
		Collection<InstallAppDevice> all = dpService.managedDeviceResoures("HomematicCCUHandler", true);
		for(InstallAppDevice iad: all) {
			Resource parent = iad.device().getLocationResource().getParent();
			if(parent != null && parent.getName().equals(otherName)) {
				return iad;
			}
		}
		return null;
	}
	
	/** This is the device name which is used to check a preKnown endCode to. If the DeviceName
	 * ends with the preKnown Code then the endCode is considered fitting. Usually the last 4 or 5
	 * digits of the deviceName are considered sufficient for a unique identification.
	 * @param device
	 * @return
	 */
	public static String getDeviceSerialNr(Resource device, DatapointService dpService) {
		String hmName;
		if(device instanceof HmInterfaceInfo) {
			HmInterfaceInfo hinfo = null;
			boolean isCC = DeviceTableBase.getHomematicType(device.getLocation()) == 2;
			if(isCC && dpService != null) {
				InstallAppDevice iad = getOtherCCU((HmLogicInterface) device.getLocationResource().getParent(), true, dpService);
				if(iad != null)
					hinfo = (HmInterfaceInfo) iad.device();
			} else
				hinfo = (HmInterfaceInfo)device;
			if(hinfo == null)
				return null;
			String addr = hinfo.address().getValue();
			if(addr == null || addr.isEmpty())
				return null;
			hmName = addr;
		} else {
			HmDevice hmDevice = (HmDevice) ResourceHelper.getFirstParentOfType(device, "HmDevice");
			if(hmDevice == null)
				return device.getName();
			hmName = hmDevice.getName();
		}
		return hmName;
	}
	public static String getDeviceEndCode(Resource device, DatapointService dpService) {
		String deviceName = getDeviceSerialNr(device, dpService);
		if(deviceName == null)
			return "NONE::"+device.getLocation();
		if(deviceName.length() > 5)
			return deviceName.substring(deviceName.length()-5);
		return deviceName;
	}
	
	public static <T extends Resource> T getDeviceByEndcode(String endCode, Class<T> deviceType, ApplicationManagerPlus appMan) {
		List<T> devices = appMan.getResourceAccess().getResources(deviceType);
		for(T dev: devices) {
			if(getDeviceSerialNr(dev, appMan.dpService()).endsWith(endCode))
				return dev;
		}
		return null;
	}
	
	private static Resource getDeviceByEndcodeInternal(String endCode, Class<? extends Resource> deviceType, ApplicationManagerPlus appMan) {
		return getDeviceByEndcode(endCode, deviceType, appMan);
	}

	public static class DeviceByEndcodeResult<T extends PhysicalElement> {
		public T device;
		public DeviceHandlerBase<T> devHand;
		
		@SuppressWarnings("unchecked")
		public DeviceByEndcodeResult(PhysicalElement device, DeviceHandlerBase<T> devHand) {
			this.device = (T) device;
			this.devHand = devHand;
		}
	}
	@SuppressWarnings({"unchecked", "rawtypes" })
	public static DeviceByEndcodeResult<? extends PhysicalElement> getDeviceByEndcode(String endCode, String devHandShortId, ApplicationManagerPlus appMan) {
		for(DeviceHandlerProviderDP<?> devHand: appMan.dpService().getDeviceHandlerProviders()) {
			if(!(devHand instanceof DeviceHandlerBase))
				continue;
			DeviceHandlerBase<?> devHandBase = (DeviceHandlerBase<?>) devHand;
			if(!devHand.getDeviceTypeShortId(appMan.dpService()).equals(devHandShortId))
				continue;
			Resource dev = getDeviceByEndcodeInternal(endCode, devHand.getResourceType(), appMan);
			if(dev == null || (!devHand.getResourceType().isAssignableFrom(dev.getResourceType())))
				return null;
			Class<? extends ResourcePattern> patternClass = devHandBase.getPatternClass();
			Constructor<?> constructor;
			try {
				try {
					constructor = patternClass.getConstructor(devHand.getResourceType());
				} catch (NoSuchMethodException e) {
					constructor = patternClass.getConstructor(Resource.class);
				}
				Object patternInstance = constructor.newInstance(dev);
				if(isSatisfiedInternal((ResourcePattern)patternInstance, patternClass, appMan.appMan())) {
					DeviceByEndcodeResult<? extends PhysicalElement> result = new DeviceByEndcodeResult((PhysicalElement) dev, devHandBase);
					return result;
				}
			} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	@SuppressWarnings({"unchecked", "rawtypes" })
	public static DeviceByEndcodeResult<? extends PhysicalElement> getDeviceHandler(PhysicalElement dev, ApplicationManagerPlus appMan) {
		if(dev == null)
			return null;
		for(DeviceHandlerProviderDP<?> devHand: appMan.dpService().getDeviceHandlerProviders()) {
			if(!(devHand instanceof DeviceHandlerBase))
				continue;
			DeviceHandlerBase<?> devHandBase = (DeviceHandlerBase<?>) devHand;
			if(!devHand.getResourceType().isAssignableFrom(dev.getResourceType()))
				continue;
			Class<? extends ResourcePattern> patternClass = devHandBase.getPatternClass();
			Constructor<?> constructor;
			try {
				try {
					constructor = patternClass.getConstructor(devHand.getResourceType());
				} catch (NoSuchMethodException e) {
					constructor = patternClass.getConstructor(Resource.class);
				}
				Object patternInstance = constructor.newInstance(dev);
				if(isSatisfiedInternal((ResourcePattern)patternInstance, patternClass, appMan.appMan())) {
					DeviceByEndcodeResult<? extends PhysicalElement> result = new DeviceByEndcodeResult((PhysicalElement) dev, devHandBase);
					return result;
				}
			} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	private static <T extends ResourcePattern> boolean isSatisfiedInternal(ResourcePattern patternInstanceIn,
			Class<T> patternClass, //Class<? extends ResourcePattern> patternClassIn,
			ApplicationManager appMan) {
		@SuppressWarnings("unchecked")
		T patternInstance = (T) patternInstanceIn;
		//Class<T> patternClass = (Class<T>) patternClassIn;
		return appMan.getResourcePatternAccess().isSatisfied(patternInstance, patternClass);
	}
}


