package org.smartrplace.apps.hw.install.patternlistener;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.resourcemanager.pattern.PatternListener;

import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.pattern.DoorWindowSensorPattern;

/**
 * A pattern listener for the TemplatePattern. It is informed by the framework 
 * about new pattern matches and patterns that no longer match.
 */
public class DoorWindowSensorListener implements PatternListener<DoorWindowSensorPattern> {
	
	private final HardwareInstallController app;
	public final List<DoorWindowSensorPattern> availablePatterns = new ArrayList<>();
	
 	public DoorWindowSensorListener(HardwareInstallController controller) {
		this.app = controller;
	}
	
	@Override
	public void patternAvailable(DoorWindowSensorPattern pattern) {
		availablePatterns.add(pattern);
		
		//TODO: work on pattern
		app.processInterdependies();
	}
	@Override
	public void patternUnavailable(DoorWindowSensorPattern pattern) {
		// TODO process remove
		
		availablePatterns.remove(pattern);
		app.processInterdependies();
	}
	
	
}
