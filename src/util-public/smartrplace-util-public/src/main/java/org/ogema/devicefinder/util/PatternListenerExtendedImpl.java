package org.ogema.devicefinder.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.api.PatternListenerExtended;
import org.ogema.model.prototypes.PhysicalElement;

public class PatternListenerExtendedImpl<P extends ResourcePattern<R>, R extends PhysicalElement> implements PatternListenerExtended<P, R>{

	private final InstalledAppsSelector app;
	private final DeviceHandlerBase<R> devHandler;
	public final List<P> availablePatterns = Collections.synchronizedList(new ArrayList<P>());
	
 	public PatternListenerExtendedImpl(InstalledAppsSelector app, DeviceHandlerBase<R> devHandler) {
if(app == null) {
	System.out.println("App null!");
	throw new IllegalStateException("Giving a null app!");
}
 		this.app = app;
		this.devHandler = devHandler;
	}
	
	@Override
	public void patternAvailable(P pattern) {
		availablePatterns.add(pattern);
		
		try {
			app.addDeviceIfNew(pattern.model, devHandler);
		} catch(NullPointerException e) {
			throw(e);
		}
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
		synchronized (availablePatterns) {
			return new ArrayList<>(availablePatterns);
		}
	}

}
