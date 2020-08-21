package org.ogema.devicefinder.api;

import java.util.Collection;

public interface AlarmingService {
	public static final String ALARMSTATUS_RES_NAME = "alarmStatus";

	boolean registerAlarmingExtension(AlarmingExtension ext);
	Collection<AlarmingExtension> getAlarmingExtensions();
	AlarmingExtension getAlarmingExtension(String id);
}
