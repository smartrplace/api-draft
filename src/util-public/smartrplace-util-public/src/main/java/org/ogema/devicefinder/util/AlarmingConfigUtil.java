package org.ogema.devicefinder.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.devicefinder.api.AlarmingService;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;

public class AlarmingConfigUtil {
	public static IntegerResource getAlarmStatus(ValueResource reading) {
		return getAlarmStatus(reading, true);
	}
	public static IntegerResource getAlarmStatus(ValueResource reading, boolean mustBeActive) {
		//Resource parent = reading.getParent();
		//IntegerResource alarmStatus = parent.getSubResource(AlarmingService.ALARMSTATUS_RES_NAME,
		//		IntegerResource.class);
		IntegerResource alarmStatus = reading.getSubResource(AlarmingService.ALARMSTATUS_RES_NAME,
				IntegerResource.class);
		if(!mustBeActive)
			return alarmStatus;
		return alarmStatus.isActive()?alarmStatus:null;		
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
			DatapointGroup devTypeGrp = DpGroupUtil.getDeviceTypeGroup(dev, appManPlus);
			//GenericFilterFixedSingle<String> selected = (GenericFilterFixedSingle<String>) deviceDrop.getSelectedItem(req);
			if(devTypeGrp == null || (!devTypeGrp.id().equals(devTypeId)))
				continue;
			if(DeviceTableRaw.isTemplate(dev, null)) {
				return dev;
			}
		}
		return null;
	}
	
	public static void copySettings(InstallAppDevice source, InstallAppDevice destination, ApplicationManager appMan) {
		copySettings(source, destination, appMan, false);
	}
	public static void copySettings(InstallAppDevice source, InstallAppDevice destination, ApplicationManager appMan,
			boolean setSendAlarms) {
		for(AlarmConfiguration alarmSource: source.alarms().getAllElements()) {
			SingleValueResource destSens = ResourceHelper.getRelativeResource(source.device(),alarmSource.sensorVal(), destination.device());
			if(destSens == null || (!destSens.exists())) {
				//appMan.getLogger().warn("Alarming "+alarmSource.sensorVal().getLocation()+" not found as relative path for: "+destination.device().getLocation());
				//throw new IllegalStateException("Alarming "+alarmSource.sensorVal().getLocation()+" not found as relative path for: "+destination.device().getLocation());
				continue;
			}
			String destPath = destSens.getLocation();
			for(AlarmConfiguration alarmDest: destination.alarms().getAllElements()) {
				if(alarmDest.sensorVal().getLocation().equals(destPath)) {
					copySettings(alarmSource, alarmDest, setSendAlarms);
					break;
				}
			}
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
				}
			}
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
}
