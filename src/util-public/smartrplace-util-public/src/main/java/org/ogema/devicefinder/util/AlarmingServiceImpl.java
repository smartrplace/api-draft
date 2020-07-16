package org.ogema.devicefinder.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.AlarmingService;

public class AlarmingServiceImpl implements AlarmingService {
	Map<String, AlarmingExtension> extensions = new HashMap<>();
	
	@Override
	public boolean registerAlarmingExtension(AlarmingExtension ext) {
		extensions.put(ext.id(), ext);
		return true;
	}

	@Override
	public Collection<AlarmingExtension> getAlarmingExtionsions() {
		return extensions.values();
	}

	@Override
	public AlarmingExtension getAlarmingExtension(String id) {
		return extensions.get(id);
	}

}
