package org.ogema.devicefinder.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.api.PatternListenerExtended;

public class PatternListenerExtendedImpl<P extends ResourcePattern<R>, R extends Resource> implements PatternListenerExtended<P, R>{

	private final InstalledAppsSelector app;
	private final DeviceHandlerBase<R> devHandler;
	public final List<P> availablePatterns = new ArrayList<>();
	
 	public PatternListenerExtendedImpl(InstalledAppsSelector app, DeviceHandlerBase<R> devHandler) {
		this.app = app;
		this.devHandler = devHandler;
	}
	
	@Override
	public void patternAvailable(P pattern) {
		availablePatterns.add(pattern);
		
		app.addDeviceIfNew(pattern.model, devHandler);
		
		app.startSimulation(devHandler, pattern.model.getLocationResource());
		/*if(!Boolean.getBoolean("org.ogema.sim.simulateRemoteGateway"))
			return;
		devHandler.startSimulationForDevice(pattern.model.getLocationResource(), app.getRoomSimulation(pattern.model),
				app.getAppManForSimulationStart());*/
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
