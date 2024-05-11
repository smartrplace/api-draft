package org.smartrplace.alarming.check;

import org.ogema.core.model.Resource;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public interface StandardActionForType<R extends Resource> {

	/** Implement to perform action
	 * 
	 * @param iad
	 * @param device
	 * @param issue
	 * @return String result. If null nothing is appended.
	 */
	String performAction(InstallAppDevice iad, R device);
	
}
