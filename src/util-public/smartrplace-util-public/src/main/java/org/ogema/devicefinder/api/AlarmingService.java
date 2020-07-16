package org.ogema.devicefinder.api;

import java.util.Collection;

public interface AlarmingService {
	boolean registerAlarmingExtension(AlarmingExtension ext);
	Collection<AlarmingExtension> getAlarmingExtionsions();
	AlarmingExtension getAlarmingExtension(String id);
}
