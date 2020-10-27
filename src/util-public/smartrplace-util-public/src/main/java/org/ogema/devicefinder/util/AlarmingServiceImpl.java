package org.ogema.devicefinder.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.devicefinder.api.AlarmOngoingGroup;
import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.AlarmingService;
import org.ogema.recordreplay.testing.RecReplayObserver;

import de.iwes.util.resource.ValueResourceHelper;

public class AlarmingServiceImpl implements AlarmingService {
	Map<String, AlarmingExtension> extensions = new HashMap<>();
	Map<String, RecReplayObserver> observers = new HashMap<>();
	Map<String, AlarmOngoingGroup> groups = new HashMap<>();
	
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
		if(groups.containsKey(grp.id()))
			throw new IllegalArgumentException("Alarming group with id "+grp.id()+" already exists!");
		groups.put(grp.id(), grp);
	}

	@Override
	public AlarmOngoingGroup getOngoingGroup(String id) {
		return groups.get(id);
	}

	@Override
	public Collection<AlarmOngoingGroup> getOngoingGroups(boolean includeFinisheds) {
		return groups.values();
	}

	@Override
	public boolean finishOngoingGroup(String id) {
		AlarmOngoingGroup grp = getOngoingGroup(id);
		if(grp == null)
			return false;
		ValueResourceHelper.setCreate(grp.getResource().isFinished(), true);
		return true;
	}

	@Override
	public void registerRecReplayObserver(RecReplayObserver observer) {
		observers.put(observer.id(), observer);
	}

	@Override
	public RecReplayObserver getRecReplayObserver(String id) {
		return observers.get(id);
	}

	@Override
	public Collection<RecReplayObserver> getAllObservers() {
		return observers.values();
	}

}
