package org.ogema.devicefinder.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.SubcustomerUtil;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.schedule.AbsoluteSchedule;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.core.resourcemanager.transaction.ResourceTransaction;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.api.DriverPropertySuccessHandler;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.devicefinder.api.PatternListenerExtended;
import org.ogema.devicefinder.api.PropType;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmDevice;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmInterfaceInfo;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmLogicInterface;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.devices.buildingtechnology.ThermostatProgram;
import org.ogema.model.devices.storage.ElectricityStorage;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.GenericBinarySensor;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.external.accessadmin.config.OnsiteVisitData;
import org.smartrplace.external.accessadmin.config.SubCustomerSuperiorData;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public abstract class DeviceHandlerBase<T extends PhysicalElement> implements DeviceHandlerProvider<T> {

	/** Request re-calculation of last decalc time after time given in this resource. As a default in 
	 * The time is requested at blockShiftingUntil, which by default is 60 minutes after The request.
	 * The default interval for the decalc timer is 120 minutes +/- random 10 minutes, so as a worst case calculation may take place
	 * almost 190 minutes after a "Decalc now" request. 
	 */
	public static final String REQUEST_LAST_DECALC_RESNAME = "requestLastDecalcCalculation";
	
	/** When clicking on "Decalc Now" or decalc via auto-issue action then also this resource is set so
	 * that decalc will be repeated with the decalc timer (approx. 120 minutes, see above) until decalc is
	 * confirmed. This is NOT VESonly, so it is sufficient the decalc time request is confirmed. Actions check
	 * for VESonly in addition.
	 */
	public static final String REQUEST_DECALC_AFTER_RESNAME = "requestDecalcAfter";
	public static final String LAST_DECALC_CALCULATED_RESNAME = "lastDecalc";
	public static final String LAST_DECALC_CALCULATED_VESONLY_RESNAME = "lastDecalcVESOnly";
	public static final String DECALC_FEEDBACK_RESNAME = "DECALC_FEEDBACK_EFFECTIVE";
	public static final String MAX_NORMAL_TEMPERATURE_RESNAME = "maxNormalTemperature";
	
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
			VoltageResource batteryVoltage = getSubResourceOfSibblingOrDirectChildMaintenance(dev.getLocationResource(),
					"battery/internalVoltage/reading", VoltageResource.class);
			return batteryVoltage;
		}		
	}
	
	public static IntegerResource getRSSIResource(PhysicalElement dev) {
		IntegerResource rssiDevice = getSubResourceOfSibblingOrDirectChildMaintenance(dev.getLocationResource(),
				"rssiDevice", IntegerResource.class);
		if(rssiDevice != null && rssiDevice.exists())
			return rssiDevice;
		return null;
	}
	public static IntegerResource getRSSIPeerResource(PhysicalElement dev) {
		IntegerResource rssiPeer = getSubResourceOfSibblingOrDirectChildMaintenance(dev.getLocationResource(),
				"rssiPeer", IntegerResource.class);
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
			BooleanResource batteryStatus = getSubResourceOfSibblingOrDirectChildMaintenance(dev,
					"batteryLow", BooleanResource.class);
			if(batteryStatus != null && batteryStatus.exists())
				addDatapoint(batteryStatus, result, dpService);
		}
		BooleanResource comDisturbed = getSubResourceOfSibblingOrDirectChildMaintenance(dev,
				"communicationStatus/communicationDisturbed", BooleanResource.class);
		if(comDisturbed != null && comDisturbed.exists())
			addDatapoint(comDisturbed, result, dpService);
		IntegerResource errorCode = getSubResourceOfSibblingOrDirectChildMaintenance(dev,
				"errorCode", IntegerResource.class);
		if(errorCode != null && errorCode.exists()) {
			addDatapoint(errorCode, result, dpService);			
		}
		BooleanResource configPending = getSubResourceOfSibblingOrDirectChildMaintenance(dev,
				"configPending", BooleanResource.class);
		if(configPending != null && configPending.exists()) {
			addDatapoint(configPending, result, dpService);			
		}
		IntegerResource rssiDevice = getSubResourceOfSibblingOrDirectChildMaintenance(dev,
				"rssiDevice", IntegerResource.class);
		if(rssiDevice != null && rssiDevice.exists())
			addDatapoint(rssiDevice, result, dpService);
		IntegerResource rssiPeer = getSubResourceOfSibblingOrDirectChildMaintenance(dev,
				"rssiPeer", IntegerResource.class);
		if(rssiPeer != null && rssiPeer.exists())
			addDatapoint(rssiPeer, result, dpService);
		GenericBinarySensor dutyCycle = getSubResourceOfSibblingOrDirectChildMaintenance(dev,
				"dutyCycle", GenericBinarySensor.class);
		if(dutyCycle != null && dutyCycle.reading().exists())
			addDatapoint(dutyCycle.reading(), result, dpService);
		IntegerResource cyclicMsgOnOff = (IntegerResource) PropType.getHmParam(dev, PropType.CYCLIC_MSG_ONOFF, false);
		if(cyclicMsgOnOff != null && cyclicMsgOnOff.exists())
			addDatapoint(cyclicMsgOnOff, result, "cyclicMsgOnOffCt", dpService);
		IntegerResource cyclicMsgOnOffFb = (IntegerResource) PropType.getHmParam(dev, PropType.CYCLIC_MSG_ONOFF, true);
		if(cyclicMsgOnOffFb != null && cyclicMsgOnOffFb.exists())
			addDatapoint(cyclicMsgOnOffFb, result, "cyclicMsgOnOffFb", dpService);
		IntegerResource cyclicMsgChanged = (IntegerResource) PropType.getHmParam(dev, PropType.CYCLIC_MSG_CHANGED, false);
		if(cyclicMsgChanged != null && cyclicMsgChanged.exists())
			addDatapoint(cyclicMsgChanged, result, "cyclicMsgChangedCt", dpService);
		IntegerResource cyclicMsgChangedFb = (IntegerResource) PropType.getHmParam(dev, PropType.CYCLIC_MSG_CHANGED, true);
		if(cyclicMsgChangedFb != null && cyclicMsgChangedFb.exists())
			addDatapoint(cyclicMsgChangedFb, result, "cyclicMsgChangedFb", dpService);
		IntegerResource cyclicMsgUnchanged = (IntegerResource) PropType.getHmParam(dev, PropType.CYCLIC_MSG_UNCHANGED, false);
		if(cyclicMsgChanged != null && cyclicMsgChanged.exists())
			addDatapoint(cyclicMsgUnchanged, result, "cyclicMsgUnchangedCt", dpService);
		IntegerResource cyclicMsgUnchangedFb = (IntegerResource) PropType.getHmParam(dev, PropType.CYCLIC_MSG_UNCHANGED, true);
		if(cyclicMsgChangedFb != null && cyclicMsgChangedFb.exists())
			addDatapoint(cyclicMsgUnchangedFb, result, "cyclicMsgUnchangedFb", dpService);
		return result;
	}
	
	public static <T extends Resource> T getSubResourceOfSibblingOrDirectChildMaintenance(Resource r, String subPath, Class<T> type) {
		T result = ResourceHelper.getSubResourceOfSibbling(r, "org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance",
				subPath, type);
		if(result != null && result.exists())
			return result;
		HmMaintenance subMaint = r.getSubResource("maintenance", HmMaintenance.class);
		if(!subMaint.exists())
			return null;
		result = ResourceHelper.getSubResource(subMaint, subPath, type);
		if(result != null && result.exists())
			return result;
		return null;
	}
	
	/** Check if Auto-mode is allowed based on configPending
	 * 
	 * @return true if AutoMode is allowed for the thermostat, which will only be used if no ecoMode is active etc.
	 */
	public static boolean isAutoModeAllowed(Thermostat dev, HardwareInstallConfig hwConfig) {
		BooleanResource configPending = getSubResourceOfSibblingOrDirectChildMaintenance(dev,
				"configPending", BooleanResource.class);
		return isAutoModeAllowed(dev, configPending, hwConfig.autoThermostatMode());
	}
	
	public static Long isAutoDecalcBlockedUntil(long now, ApplicationManager appMan) {
		final SubCustomerSuperiorData database = SubcustomerUtil.getEntireBuildingSubcustomerDatabase(appMan);
		if (database == null) {
			return null;
		}
		for(OnsiteVisitData visit : database.onSiteVisits().getAllElements()) {
			if(!visit.blockAutoDecalcForDays().isActive())
				continue;
			if(now < visit.date().getValue())
				continue;
			long endVal = visit.date().getValue() +
					(long)(TimeProcUtil.DAY_MILLIS * visit.blockAutoDecalcForDays().getValue());
			if(now < endVal)
				return endVal;
		}
		return null;
	}

	public static long blockShiftingUntil = -1;
	public static boolean performWeeklyPostpone(Thermostat dev,HardwareInstallConfig hwConfig, long now,
			ApplicationManager appMan) {
		if(hwConfig == null)
			return Boolean.getBoolean("org.smartrplace.homematic.devicetable.autostart.shiftdecalc");		
		int val = hwConfig.weeklyPostponeMode().getValue();
		if(val == 1)
			return false;
		if(val == 2)
			return true;
		if(val == 4) {
			if(now < hwConfig.noPostponeStart().getValue())
				return true;
			if(now >= hwConfig.noPostponeEnd().getValue())
				return true;
			return false;
		}
		if(val == 5) {
			TimeResource lastDecalc = dev.valve().getSubResource(LAST_DECALC_CALCULATED_RESNAME, TimeResource.class);
			if(lastDecalc == null)
				return true;
			if(now - lastDecalc.getValue() > 30*TimeProcUtil.DAY_MILLIS) {
				//request recalc
				TimeResource requestCalc = dev.valve().getSubResource(REQUEST_LAST_DECALC_RESNAME, TimeResource.class);
				Long blockedByOnsite = (appMan != null)?isAutoDecalcBlockedUntil(now, appMan):null;
				if(blockedByOnsite != null)
					return true;
				if(requestCalc.getValue() > now)
					return false;
				long nextScheduled = DeviceTableRaw.getNextDecalcTime(dev, now);
				ValueResourceHelper.setCreate(requestCalc, nextScheduled + 2*TimeProcUtil.HOUR_MILLIS);
				return false;
			}
			return true;
		}
		return Boolean.getBoolean("org.smartrplace.homematic.devicetable.autostart.shiftdecalc");		
	}
	
	public static boolean performDailyDecalc(Thermostat dev,HardwareInstallConfig hwConfig, long now) {
		if(hwConfig == null)
			return false;		
		int val = hwConfig.weeklyPostponeMode().getValue();
		if(val == 3)
			return true;
		if(val == 4) {
			if(now < hwConfig.noPostponeStart().getValue())
				return false;
			if(now >= hwConfig.noPostponeEnd().getValue())
				return false;
			return true;
		}
		if(val == 5) {
			return false;
		}
		return false;		
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
	public static IntegerResource getSendIntervalModeSingle(Thermostat th) {
		return th.getSubResource("sendIntervalModeSingle", IntegerResource.class);
	}
	public static BooleanResource getMaxSendSingle(Thermostat th) {
		return th.getSubResource("maxSendMode", BooleanResource.class);
	}
	public static BooleanResource getShortBatteryLifetimeIndicator(PhysicalElement device) {
		return device.getSubResource("shorBattery", BooleanResource.class);
	}
	public static ThermostatProgram getHmThermProgram(Thermostat th, boolean force) {
		ThermostatProgram hmThermProgram = th.getSubResource("program", ThermostatProgram.class);
		if(force)
			return hmThermProgram;
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
			String serialNr = getDeviceSerialNr(dev, appMan.dpService());
			if((serialNr != null) && serialNr.endsWith(endCode))
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
	
	public static int setOpenIntervalConfigs(IntegerResource sendIntervalModeGeneral,
			Collection<InstallAppDevice> all, DatapointService dpService, boolean resend, ResourceAccess resAcc) {
		if(all == null)
			all = dpService.managedDeviceResoures(Thermostat.class);
		int count = 0;
		for(InstallAppDevice dev: all) {
			if(dev.isTrash().getValue())
				continue;
			if(dev.device() instanceof Thermostat) {
				count += DeviceHandlerBase.setSendIntervalByMode((Thermostat) dev.device().getLocationResource(), sendIntervalModeGeneral, resend, resAcc);
			}
		}
		return count;
	}

	public static int setSendIntervalByMode(Thermostat dev,
			IntegerResource sendIntervalModeGeneral, boolean resend, ResourceAccess resAcc) {
		IntegerResource sendIntervalModeSingle = getSendIntervalModeSingle(dev);
		int singleState = sendIntervalModeSingle.getValue();
		int overallState = sendIntervalModeGeneral.getValue();
		int realState;
		if(overallState == 1 || overallState == 3 || overallState == 5 || singleState == 0)
			realState = overallState/2;
		else
			realState = singleState-1;
		if(singleState == 4)
			return 0;
		return setSendIntervalByMode(dev, realState, resend, resAcc);
		
	}
	
	/**
	 * 
	 * @param dev
	 * @param realState 0=standard mode, 1=summer mode(cyclic off), 2=reduced, 3=max
	 * @param resend
	 * @return
	 */
	public static int setSendIntervalByMode(Thermostat dev,
			int realState, boolean resend, ResourceAccess resAcc) {
		int count = 0;
		ResourceTransaction trans = resAcc.createResourceTransaction();
		switch(realState) {
		case 0:
			if(setSendIntervalConfig(0, 1, dev, resend, trans))
				count++;
			if(setSendIntervalConfig(1, 1, dev, resend, trans))
				count++;
			if(setSendIntervalConfig(2, 20, dev, resend, trans))
				count++;
			break;			
		case 1:
			if(setSendIntervalConfig(0, 0, dev, resend, trans))
				count++;
			//if(setSendIntervalConfig(1, 1, dev, resend))
			//	coun1t++;
			//if(setSendIntervalConfig(2, 20, dev, resend))
			//	count++;
			break;			
		case 2:
			if(setSendIntervalConfig(0, 1, dev, resend, trans))
				count++;
			if(setSendIntervalConfig(1, 9, dev, resend, trans))
				count++;
			if(setSendIntervalConfig(2, 10, dev, resend, trans))
				count++;
			break;			
		case 3:
			if(setSendIntervalConfig(0, 1, dev, resend, trans))
				count++;
			if(setSendIntervalConfig(1, 0, dev, resend, trans))
				count++;
			if(setSendIntervalConfig(2, 5, dev, resend, trans))
				count++;
			break;			
		default:
			//Should never occur
			break;
		}
		trans.commit();
		return count;
	}

	/**
	 * 
	 * @param type 0: ON_OFF (0/1), 1:CHANGED, 2:UNCHANGED
	 * @param value
	 * @param device
	 * @param resend
	 * @return
	 */
	public static boolean setSendIntervalConfig(int type, int value, Thermostat device, boolean resend) {
		return setSendIntervalConfig(type, value, device, resend, null);
	}
	public static boolean setSendIntervalConfig(int type, int value, Thermostat device, boolean resend, ResourceTransaction trans) {
		final IntegerResource setp;
		final IntegerResource fb;
		if(type == 0) {
			setp = (IntegerResource) PropType.getHmParam(device, PropType.CYCLIC_MSG_ONOFF, false);
			fb = (IntegerResource) PropType.getHmParam(device, PropType.CYCLIC_MSG_ONOFF, true);
		} else if(type == 1) {
			setp = (IntegerResource) PropType.getHmParam(device, PropType.CYCLIC_MSG_CHANGED, false);
			fb = (IntegerResource) PropType.getHmParam(device, PropType.CYCLIC_MSG_CHANGED, true);
		} else if(type == 2) {
			setp = (IntegerResource) PropType.getHmParam(device, PropType.CYCLIC_MSG_UNCHANGED, false);
			fb = (IntegerResource) PropType.getHmParam(device, PropType.CYCLIC_MSG_UNCHANGED, true);
		} else
			throw new IllegalStateException(" Unknown TYPE:"+type);
		if(setp == null)
			return false;
		if(!resend) {
			if(fb != null && (fb.getValue() == value && setp.getValue() == value))
				return false;
		}
		if(trans != null)
			trans.setInteger(setp, value);
		else
			setp.setValue(value);
		return true;
	}
}


