package org.ogema.devicefinder.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.PatternListenerExtended;
import org.ogema.devicefinder.util.DeviceTableBase.InstalledAppsSelector;

public class PatternListenerExtendedImpl<P extends ResourcePattern<R>, R extends Resource> implements PatternListenerExtended<P, R>{

	private final InstalledAppsSelector app;
	public final List<P> availablePatterns = new ArrayList<>();
	
 	public PatternListenerExtendedImpl(InstalledAppsSelector app) {
		this.app = app;
	}
	
	@Override
	public void patternAvailable(P pattern) {
		availablePatterns.add(pattern);
		
		app.addDeviceIfNew(pattern.model);
	}
	@Override
	public void patternUnavailable(P pattern) {
		app.removeDevice(pattern.model);
		availablePatterns.remove(pattern);
	}

	@Override
	public List<P> getAllPatterns() {
		return availablePatterns;
	}

}
