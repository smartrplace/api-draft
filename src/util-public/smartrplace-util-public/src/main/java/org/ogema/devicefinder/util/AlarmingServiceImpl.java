package org.ogema.devicefinder.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.devicefinder.api.AlarmOngoingGroup;
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
	public Collection<AlarmingExtension> getAlarmingExtensions() {
		return extensions.values();
	}

	@Override
	public AlarmingExtension getAlarmingExtension(String id) {
		return extensions.get(id);
	}

	@Override
	public void registerOngoingAlarmGroup(AlarmOngoingGroup grp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AlarmOngoingGroup getOngoingGroup(String id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<AlarmOngoingGroup> getOngoingGroups(boolean includeFinisheds) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean finishOngoingGroup(String id) {
		// TODO Auto-generated method stub
		return false;
	}

}
