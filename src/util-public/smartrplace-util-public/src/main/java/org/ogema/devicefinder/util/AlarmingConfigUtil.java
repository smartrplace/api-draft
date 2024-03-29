package org.ogema.devicefinder.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.core.resourcemanager.ResourceNotFoundException;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.AlarmingService;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.model.devices.sensoractordevices.SingleSwitchBox;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon.TimeSeriesServlet;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.config.InstallAppDeviceBase;

import de.iwes.util.resource.OGEMAResourceCopyHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.messaging.MessagePriority;

public class AlarmingConfigUtil {
	public static final double QUALITY_TIME_SHARE_LIMIT = 0.95f;
	public static final double QUALITY_SHORT_MAX_MINUTES = 4*1440*(1.0 - QUALITY_TIME_SHARE_LIMIT);
	public static final double QUALITY_LONG_MAX_MINUTES = 28*1440*(1.0 - QUALITY_TIME_SHARE_LIMIT);
	public static final double QUALITY_DAY_MAX_MINUTES = 1*1440*(1.0 - QUALITY_TIME_SHARE_LIMIT);

	private final static Logger logger = LoggerFactory.getLogger(AlarmingConfigUtil.class);
	
	public static final int MAIN_ASSIGNEMENT_ROLE_NUM = 8; //including unassigned (0)
	public static final Map<String, String> ASSIGNEMENT_ROLES = new LinkedHashMap<>();
	public static final int ASSIGNMENT_OTHER = 1000;
	public static final int ASSIGNMENT_OPERATION = 2000;
	public static final int ASSIGNMENT_BATTERYLOW = 2100;
	public static final int ASSIGNMENT_DEVICE_NOT_REACHEABLE = 2150;
	public static final int ASSIGNMENT_SIGNALSTRENGTH = 2200;
	public static final int ASSIGNMENT_OPERATRION_EXTERNAL = 2500;
	public static final int ASSIGNMENT_DEVELOPMENT = 3000;
	public static final int ASSIGNMENT_VALVE_ERROR = 3500;
	public static final int ASSIGNMENT_CUSTOMER = 4000;
	public static final int ASSIGNMENT_BACKLOG = 5000;
	public static final int ASSIGNMENT_DEPDENDENT = 6000;
	public static final int ASSIGNMENT_SPECIALSETS = 7000;
	static {
		ASSIGNEMENT_ROLES.put("0", "None");
		//ASSIGNEMENT_ROLES.put("1", "requires more analysis");
		ASSIGNEMENT_ROLES.put(""+ASSIGNMENT_OTHER, "Other");	
		ASSIGNEMENT_ROLES.put(""+ASSIGNMENT_OPERATION, "Operation");
		ASSIGNEMENT_ROLES.put(""+ASSIGNMENT_BATTERYLOW, "Op Battery");
		ASSIGNEMENT_ROLES.put(""+ASSIGNMENT_DEVICE_NOT_REACHEABLE, "Op Device not Reacheable");
		ASSIGNEMENT_ROLES.put(""+ASSIGNMENT_SIGNALSTRENGTH, "Op Signal strength");
		ASSIGNEMENT_ROLES.put(""+ASSIGNMENT_OPERATRION_EXTERNAL, "Op External");
		ASSIGNEMENT_ROLES.put(""+ASSIGNMENT_DEVELOPMENT, "Development");
		//ASSIGNEMENT_ROLES.put("2100", "Development Logic");
		//ASSIGNEMENT_ROLES.put("2200", "Development HW Driver");
		ASSIGNEMENT_ROLES.put(""+ASSIGNMENT_VALVE_ERROR, "Op Valve Error"); //"Dev External");
		ASSIGNEMENT_ROLES.put(""+ASSIGNMENT_CUSTOMER, "Customer");
		ASSIGNEMENT_ROLES.put(""+ASSIGNMENT_BACKLOG, "Backlog");
		ASSIGNEMENT_ROLES.put(""+ASSIGNMENT_DEPDENDENT, "Dependent");
		ASSIGNEMENT_ROLES.put(""+ASSIGNMENT_SPECIALSETS, "Special Settings (Non Blocking)");
	}
	public static String assignedText(int value) {
		String result = ASSIGNEMENT_ROLES.get(""+value);
		if(result != null)
			return result;
		return "unknown:"+value;
	}
	
	public static IntegerResource getAlarmStatus(ValueResource reading) {
		return getAlarmStatus(reading, true);
	}
	public static IntegerResource getAlarmStatus(ValueResource reading, boolean mustBeActive) {
		//Resource parent = reading.getParent();
		//IntegerResource alarmStatus = parent.getSubResource(AlarmingService.ALARMSTATUS_RES_NAME,
		//		IntegerResource.class);
		try {
			IntegerResource alarmStatus = reading.getSubResource(AlarmingService.ALARMSTATUS_RES_NAME,
					IntegerResource.class);
			if(!mustBeActive)
				return alarmStatus;
			return alarmStatus.isActive()?alarmStatus:null;
		} catch(ResourceNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static void disAbleAllOfTemplateType(String devTypeId, ApplicationManagerPlus appManPlus) {
		List<InstallAppDevice> allDev = new ArrayList<>();
		InstallAppDevice template = null;
		for(InstallAppDevice dev: appManPlus.getResourceAccess().getResources(InstallAppDevice.class)) {
			DatapointGroup devTypeGrp = DpGroupUtil.getDeviceTypeGroup(dev, appManPlus);
			//GenericFilterFixedSingle<String> selected = (GenericFilterFixedSingle<String>) deviceDrop.getSelectedItem(req);
			if(devTypeGrp == null || (!devTypeGrp.id().equals(devTypeId)))
				continue;
			if(DeviceTableRaw.isTemplate(dev, null)) {
				template = dev;
				continue;
			}
			allDev.add(dev);
		}
		if(template == null)
			return;
		for(InstallAppDevice dev: allDev) {
			AlarmingConfigUtil.deactivateAlarms(dev);
		}		
	}

	public static void applyTemplate(String devTypeId, ApplicationManagerPlus appManPlus) {
		List<InstallAppDevice> allDev = new ArrayList<>();
		InstallAppDevice template = null;
		for(InstallAppDevice dev: appManPlus.getResourceAccess().getResources(InstallAppDevice.class)) {
			DatapointGroup devTypeGrp = DpGroupUtil.getDeviceTypeGroup(dev, appManPlus);
			//GenericFilterFixedSingle<String> selected = (GenericFilterFixedSingle<String>) deviceDrop.getSelectedItem(req);
			if(devTypeGrp == null || (!devTypeGrp.id().equals(devTypeId)))
				continue;
			if(DeviceTableRaw.isTemplate(dev, null)) {
				template = dev;
				continue;
			}
			allDev.add(dev);
		}
		if(template == null)
			return;
		for(InstallAppDevice dev: allDev) {
			AlarmingConfigUtil.copySettings(template, dev, appManPlus.appMan());
		}		
	}

	public static InstallAppDevice getTemplate(InstallAppDevice destination,
			ApplicationManagerPlus appManPlus) {
		String devTypeId = destination.devHandlerInfo().getValue();
		return getTemplate(devTypeId, appManPlus);
	}
	public static InstallAppDevice getTemplate(String devTypeId,
			ApplicationManagerPlus appManPlus) {
		for(InstallAppDevice dev: appManPlus.getResourceAccess().getResources(InstallAppDevice.class)) {
			//DatapointGroup devTypeGrp = DpGroupUtil.getDeviceTypeGroup(dev, appManPlus);
			//if(devTypeGrp == null || (!devTypeGrp.id().equals(devTypeId)))
			//	continue;
			//if(DeviceTableRaw.isTemplate(dev, null)) {
			//	return dev;
			//}
			if(dev.isTemplate().isActive() && dev.isTemplate().getValue().equals(devTypeId))
				return dev;
		}
		return null;
	}
	
	public static void copySettings(InstallAppDevice source, InstallAppDevice destination, ApplicationManager appMan) {
		copySettings(source, destination, appMan, false);
	}
	
	public static class CopyAlarmsSettings {
		public CopyAlarmsSettings(AlarmConfiguration templateConfig, AlarmConfiguration deviceConfig) {
			this.templateConfig = templateConfig;
			this.deviceConfig = deviceConfig;
		}
		public AlarmConfiguration templateConfig;
		public AlarmConfiguration deviceConfig;
	}
	
	public static Map<String, CopyAlarmsSettings> getTemplateAlarmSettings(InstallAppDeviceBase template, InstallAppDevice device) {
		Map<String, CopyAlarmsSettings> result = new HashMap<>();
		for(AlarmConfiguration alarmSource: template.alarms().getAllElements()) {
			//TODO: Make this more general
			SingleValueResource destSens = null;
			if((template.device() instanceof SingleSwitchBox) && (DeviceTableBase.makeDeviceToplevel(template.device().getLocation()).startsWith("virtSwitchBoxes"))) {
				String sourceLoc = alarmSource.sensorVal().getLocation();
				if(sourceLoc.endsWith("stateControl"))
					destSens = ((SingleSwitchBox)device.device()).onOffSwitch().stateControl();
				else if(sourceLoc.endsWith("stateFeedback"))
					destSens = ((SingleSwitchBox)device.device()).onOffSwitch().stateFeedback();
			}
			if(destSens == null)
				destSens = ResourceHelper.getRelativeResource(template.device(),alarmSource.sensorVal(), device.device());
			if(destSens == null || (!destSens.exists())) {
				continue;
			}
			String destPath = destSens.getLocation();
			for(AlarmConfiguration alarmDest: device.alarms().getAllElements()) {
				if(alarmDest.sensorVal().getLocation().equals(destPath)) {
					result.put(destPath, new CopyAlarmsSettings(alarmSource, alarmDest));
					//copySettings(alarmSource, alarmDest, setSendAlarms);
					break;
				}
			}
		}
		return result ;
	}
	
	public static void copySettings(InstallAppDevice source, InstallAppDevice destination, ApplicationManager appMan,
			boolean setSendAlarms) {
		Map<String, CopyAlarmsSettings> data = getTemplateAlarmSettings(source, destination);
		for(CopyAlarmsSettings copy: data.values()) {
			copySettings(copy.templateConfig, copy.deviceConfig, setSendAlarms);
		}
	}
	
	public static void copySettings(AlarmConfiguration source, AlarmConfiguration destination,
			boolean setSendAlarms) {
		if(setSendAlarms)
			ValueResourceHelper.setCreate(destination.sendAlarm(), true);
		else
			copyValue(source.sendAlarm(), destination.sendAlarm());
		copyValue(source.lowerLimit(), destination.lowerLimit());
		copyValue(source.upperLimit(), destination.upperLimit());
		copyValue(source.alarmLevel(), destination.alarmLevel());
		copyValue(source.maxIntervalBetweenNewValues(), destination.maxIntervalBetweenNewValues());
		copyValue(source.maxViolationTimeWithoutAlarm(), destination.maxViolationTimeWithoutAlarm());
		copyValue(source.alarmRepetitionTime(), destination.alarmRepetitionTime());
		copyValue(source.performAdditinalOperations(), destination.performAdditinalOperations());
		copyValue(source.alarmingExtensions(), destination.alarmingExtensions());
	}
	
	public static void copyValue(FloatResource source, FloatResource destination) {
		if(source.isActive()) {
			if(destination.isActive())
				destination.setValue(source.getValue());
			else {
				destination.create();
				destination.setValue(source.getValue());
				destination.activate(false);
			}
		}
	}
	public static void copyValue(BooleanResource source, BooleanResource destination) {
		if(source.isActive()) {
			if(destination.isActive())
				destination.setValue(source.getValue());
			else {
				destination.create();
				destination.setValue(source.getValue());
				destination.activate(false);
			}
		}
	}
	public static void copyValue(IntegerResource source, IntegerResource destination) {
		if(source.isActive()) {
			if(destination.isActive())
				destination.setValue(source.getValue());
			else {
				destination.create();
				destination.setValue(source.getValue());
				destination.activate(false);
			}
		}
	}
	public static void copyValue(TimeResource source, TimeResource destination) {
		if(source.isActive()) {
			if(destination.isActive())
				destination.setValue(source.getValue());
			else {
				destination.create();
				destination.setValue(source.getValue());
				destination.activate(false);
			}
		}
	}
	public static void copyValue(StringResource source, StringResource destination) {
		if(source.isActive()) {
			if(destination.isActive())
				destination.setValue(source.getValue());
			else {
				destination.create();
				destination.setValue(source.getValue());
				destination.activate(false);
			}
		}
	}
	public static void copyValue(StringArrayResource source, StringArrayResource destination) {
		if(source.isActive()) {
			if(destination.isActive())
				destination.setValues(source.getValues());
			else {
				destination.create();
				destination.setValues(source.getValues());
				destination.activate(false);
			}
		}
	}
	public static void deactivateAlarms(InstallAppDevice object) {
		for(AlarmConfiguration alarmDest: object.alarms().getAllElements()) {
			alarmDest.sendAlarm().setValue(false);
		}
	}
	
	/** Get alarming summary status
	 * 
	 * @param template
	 * @param object
	 * @param appMan
	 * @return true if matches template device, false if inactive and null if active differently from
	 * 		template device
	 */
	public static Boolean getAlarmingStatus(InstallAppDevice template, InstallAppDevice destination) {
		boolean result = false;
		for(AlarmConfiguration alarmSource: template.alarms().getAllElements()) {
			SingleValueResource destSens = ResourceHelper.getRelativeResource(template.device(),alarmSource.sensorVal(), destination.device());
			if(destSens == null || (!destSens.exists())) {
				//appMan.getLogger().warn("Alarming "+alarmSource.sensorVal().getLocation()+" not found as relative path for: "+destination.device().getLocation());
				//throw new IllegalStateException("Alarming "+alarmSource.sensorVal().getLocation()+" not found as relative path for: "+destination.device().getLocation());
				continue;
			}
			String destPath = destSens.getLocation();
			for(AlarmConfiguration alarmDest: destination.alarms().getAllElements()) {
				if(alarmDest.sensorVal().getLocation().equals(destPath)) {
					Boolean resLoc = getAlarmingStatus(alarmSource, alarmDest);
					if(resLoc == null)
						return null;
					if(resLoc)
						result = true;
					break;
				}
			}
		}
		if(!result) for(AlarmConfiguration alarmDest: destination.alarms().getAllElements()) {
			if(alarmDest.sendAlarm().getValue())
				return true;
		}
		return result;
	}

	public static Boolean getAlarmingStatus(AlarmConfiguration template, AlarmConfiguration destination) {
		if(!template.sendAlarm().getValue())
			return destination.sendAlarm().getValue()?null:false;
		if(!destination.sendAlarm().getValue())
			return false;
		Boolean result = false;
		result = compareValue(template.lowerLimit(), destination.lowerLimit(), result);
		if(result == null) return null;
		result = compareValue(template.upperLimit(), destination.upperLimit(), result);
		if(result == null) return null;
		result = compareValue(template.alarmLevel(), destination.alarmLevel(), result);
		if(result == null) return null;
		result = compareValue(template.maxIntervalBetweenNewValues(), destination.maxIntervalBetweenNewValues(), result);
		if(result == null) return null;
		result = compareValue(template.maxViolationTimeWithoutAlarm(), destination.maxViolationTimeWithoutAlarm(), result);
		if(result == null) return null;
		result = compareValue(template.alarmRepetitionTime(), destination.alarmRepetitionTime(), result);
		if(result == null) return null;
		result = compareValue(template.performAdditinalOperations(), destination.performAdditinalOperations(), result);
		if(result == null) return null;
		result = compareValue(template.alarmingExtensions(), destination.alarmingExtensions(), result);
		return result;
	}

	public static <T extends ValueResource> Boolean compareValue(T template, T destination,
			boolean stateBefore) {
		if(template.isActive()) {
			if(destination.isActive())
				return ValueResourceUtils.isEqual(destination, template)?true:null;
			else {
				return null;
			}
		} else {
			if(destination.isActive())
				return null;
			else {
				return stateBefore;
			}			
		}
	}

	public static String getDeviceId(AlarmConfiguration ac) {
		InstallAppDevice device = ResourceHelper.getFirstParentOfType(ac, InstallAppDevice.class);
		if(device == null)
			return "NoDev";
		else
			return device.deviceId().getValue();
	}
	
	public static String getDatapointLabel(AlarmConfiguration ac, DatapointService dpService) {
		Datapoint dp = getDatapointAsIs(ac, dpService);
		if(dp == null)
			return getDeviceId(ac)+"::"+ac.getName();
		else
			return dp.label(null);
	}
	public static Datapoint getDatapointAsIs(AlarmConfiguration ac, DatapointService dpService) {
		SingleValueResource sensor = ac.sensorVal().getLocationResource();
		Datapoint dp = dpService.getDataPointAsIs(sensor);
		return dp;
	}
	
	/** 0: number of Knis not assigned, [1] assigned to other, [2]: assigned to operation, [3]: development, [4]: customer,
	 * [8]: operation external, [9]: development external, [10]: battery, [11]: device not reachable, [12]: signal strength
	 * more indeces may be defined by AlarmGroupData.USER_ROLES*/
	public static int[] getKnownIssues(ResourceAccess resAcc) {
		HardwareInstallConfig hwInstall = ResourceHelper.getTopLevelResource(HardwareInstallConfig.class, resAcc);
		int[] result = new int[MAIN_ASSIGNEMENT_ROLE_NUM+5];
		for(InstallAppDevice dev: hwInstall.knownDevices().getAllElements()) {
			if(dev.isTrash().getValue())
				continue;
			AlarmGroupData kni = dev.knownFault();
			if(!kni.exists())
				continue;
			if(!kni.assigned().exists()) {
				result[0] ++;
				continue;
			}
			int role = kni.assigned().getValue();
			if(role == 0) {
				result[0] ++;
				continue;
			}
			if(role == ASSIGNMENT_BATTERYLOW) {
				result[MAIN_ASSIGNEMENT_ROLE_NUM+2] ++;
				continue;								
			}
			if(role == ASSIGNMENT_DEVICE_NOT_REACHEABLE) {
				result[MAIN_ASSIGNEMENT_ROLE_NUM+3] ++;
				continue;								
			}
			if(role == ASSIGNMENT_SIGNALSTRENGTH) {
				result[MAIN_ASSIGNEMENT_ROLE_NUM+4] ++;
				continue;								
			}
			if(role >= 2500 && role < 3000) {
				result[MAIN_ASSIGNEMENT_ROLE_NUM] ++;
				continue;				
			}
			if(role >= 3500 && role < 4000) {
				result[MAIN_ASSIGNEMENT_ROLE_NUM+1] ++;
				continue;				
			}

			int mainRole = role/1000;
			if(mainRole < 0) {
				result[0] ++;
				continue;
			}
			if(mainRole >= MAIN_ASSIGNEMENT_ROLE_NUM) {
				System.out.println("Assigned value too large:"+role+" for "+kni.assigned().getLocation());
				//result[0] ++;
				continue;
			}
			result[mainRole] ++;
		}
		return result;
	}

	public static double getValueSum(List<SampledValue> svs) {
		double sum = 0;
		for(SampledValue sv: svs) {
			float val = sv.getValue().getFloatValue();
			if(!Float.isNaN(val))
				sum += val;
		}
		return sum;
	}
	
	/** 0: qualityShort, [1] qualityLong, [2]: qualityShort V2, [3]: qualityLong V2*/
	@Deprecated // Use StandardEvalAccess#getQualityValuesForStandardDurations instead
	public static int[] getQualityValues(ApplicationManager appMan, DatapointService dpService) {
		long now = appMan.getFrameworkTime();
		long startShort = now - 4*TimeProcUtil.DAY_MILLIS;
		long startLong = now - 28*TimeProcUtil.DAY_MILLIS;
		int[] resShort = getQualityValues(appMan, dpService, startShort, now, QUALITY_SHORT_MAX_MINUTES);
		int[] resLong = getQualityValues(appMan, dpService, startLong, now, QUALITY_LONG_MAX_MINUTES);
		return new int[] {resShort[0], resShort[1], resLong[0], resLong[1]};
	}
	
	/** [0]: quality V1, [1]: quality V2*/
	@Deprecated // Use StandardEvalAccess#getQualityValues instead
	public static int[] getQualityValues(ApplicationManager appMan, DatapointService dpService,
			long startTime, long endTime, double QUALITY_MAX_MINUTES) {

		ResourceAccess resAcc = appMan.getResourceAccess();
		HardwareInstallConfig hwInstall = ResourceHelper.getTopLevelResource(HardwareInstallConfig.class, resAcc);
		int[] result = new int[] {0,0};
		int countShortOk = 0;
		int countShortOkGold = 0;
		int countEval = 0;
		int countEvalGold =0;
		for(InstallAppDevice dev: hwInstall.knownDevices().getAllElements()) {
			if(dev.isTrash().getValue())
				continue;
			boolean isAssigned = (dev.knownFault().assigned().isActive() && (dev.knownFault().assigned().getValue() > 0));
			for(AlarmConfiguration ac: dev.alarms().getAllElements()) {
				if(!ac.sendAlarm().getValue())
					continue;
				SingleValueResource sens = ac.sensorVal().getLocationResource();
				if(!sens.exists())
					continue;
				float maxGapSize = ac.maxIntervalBetweenNewValues().getValue();
				if(maxGapSize < 0)
					continue;
				Datapoint dp = dpService.getDataPointAsIs(sens);
				if(dp == null)
					continue; //should not occur
				ReadOnlyTimeSeries ts = dp.getTimeSeries();
				if(ts == null) {
					appMan.getLogger().trace("No timeseries for datapoint configured for alarming:"+sens.getLocation());
					continue;
				}
				try {
					List<SampledValue> gaps = TimeSeriesServlet.getGaps(ts, startTime, endTime, (long) ((double)maxGapSize*TimeProcUtil.MINUTE_MILLIS));
					double sum = getValueSum(gaps);
					if(sum <= QUALITY_MAX_MINUTES) {
						countShortOkGold++;
						if(!isAssigned)
							countShortOk++;
					} else if(Boolean.getBoolean("qualitydebug") && (!isAssigned))
						System.out.println("Gaps found for "+QUALITY_MAX_MINUTES+" for "+sens.getLocation());
					countEvalGold++;
					if(!isAssigned)
						countEval++;
				} catch(OutOfMemoryError e) {
					logger.error("OutOfMemory for "+sens.getLocation());
					e.printStackTrace();
				}
			}
		}
		if(countEval == 0)
			result[0] = 100;
		else
			result[0] = (int) (((float)countShortOk) / countEval * 100);
		if(countEvalGold == 0)
			result[1] = 100;
		else
			result[1] = (int) (((float)countShortOkGold) / countEvalGold * 100);
		return result;
	}

	public static int[] getActiveAlarms(ResourceAccess resAcc) {
		HardwareInstallConfig hwInstall = ResourceHelper.getTopLevelResource(HardwareInstallConfig.class, resAcc);
		int[] result = new int[] {0,0,0,0};
		for(InstallAppDevice dev: hwInstall.knownDevices().getAllElements()) {
			if(dev.isTrash().getValue())
				continue;
			int[] devNum = getActiveAlarms(dev);
			result[0] += devNum[0];
			result[1] += devNum[1];
			result[2] += dev.dpNum().getValue();
			result[3]++;
		}
		return result;
	}
	/** index 0: number of datapoints in alarm state<br>
	 *  index 1: number of datapoints for which alarming is configured.
	 * @param object
	 * @return
	 */
	public static int[] getActiveAlarms(InstallAppDevice object) {
		int alNum = 0;
		int alStatusNum = 0;
		for(AlarmConfiguration ac: object.alarms().getAllElements()) {
			if(ac.sendAlarm().getValue()) {
				alNum++;
				IntegerResource status = AlarmingConfigUtil.getAlarmStatus(ac.sensorVal().getLocationResource());
				if(status != null && status.getValue() > 0)
					alStatusNum++;
			}
		}
		return new int[] {alStatusNum, alNum};
	}
	
	public static InstallAppDeviceBase getTemplate(InstallAppDevice iad, List<InstallAppDeviceBase> templates) {
		for(InstallAppDeviceBase template: templates) {
			if(template.devHandlerInfo().getValue().equals(iad.devHandlerInfo().getValue()))
				return template;
		}
		return null;
	}

	public static InstallAppDeviceBase getOrCreateTemplate(InstallAppDevice iad,
			ResourceList<InstallAppDeviceBase> templates, ApplicationManager appMan) {
		InstallAppDeviceBase result = getTemplate(iad, templates.getAllElements());
		if(result != null)
			return result;
		result = templates.add();
		ValueResourceHelper.setCreate(result.devHandlerInfo(), iad.devHandlerInfo().getValue());
		result.device().setAsReference(iad.device());
		OGEMAResourceCopyHelper.copySubResourceIntoDestination(result.alarms(), iad.alarms(), appMan, true);
		result.activate(false);
		return result;
	}
	
	public static String getDeviceHandlerShortId(InstallAppDeviceBase iad) {
		return getDeviceHandlerShortId(iad.devHandlerInfo().getValue());
	}
	public static String getDeviceHandlerShortId(String longId) {
		String[] dhidfull = longId.split("\\.");
		String dhid = dhidfull[dhidfull.length-1];
		return dhid;
	}
	
	public static MessagePriority getMessagePrio(int resourceValue) {
		switch(resourceValue) {
		case 1:
			return MessagePriority.LOW;
		case 2:
			return MessagePriority.MEDIUM;
		case 3:
			return MessagePriority.HIGH;
		default:
			return null;
		}
	}
}
