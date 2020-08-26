package org.ogema.devicefinder.util;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.resource.ResourceHelper;

public class AlarmingConfigUtil {
	public static void copySettings(InstallAppDevice source, InstallAppDevice destination, ApplicationManager appMan) {
		for(AlarmConfiguration alarmSource: source.alarms().getAllElements()) {
			SingleValueResource destSens = ResourceHelper.getRelativeResource(source.device(),alarmSource.sensorVal(), destination.device(), appMan.getResourceAccess());
			if(destSens == null) {
				appMan.getLogger().warn("Alarming "+alarmSource.sensorVal().getLocation()+" not found as relative path for: "+destination.device().getLocation());
				//throw new IllegalStateException("Alarming "+alarmSource.sensorVal().getLocation()+" not found as relative path for: "+destination.device().getLocation());
				continue;
			}
			String destPath = destSens.getLocation();
			for(AlarmConfiguration alarmDest: destination.alarms().getAllElements()) {
				if(alarmDest.sensorVal().getLocation().equals(destPath)) {
					copySettings(alarmSource, alarmDest);
					break;
				}
			}
		}
	}
	
	public static void copySettings(AlarmConfiguration source, AlarmConfiguration destination) {
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

}
