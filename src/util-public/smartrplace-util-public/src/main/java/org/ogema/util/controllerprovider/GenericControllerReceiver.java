package org.ogema.util.controllerprovider;

import org.ogema.core.application.ApplicationManager;

/**
 * 
 * @author dnestle
 *
 * @param <C> controller class
 */
public abstract class GenericControllerReceiver<C> {
	private volatile C controller;
	private volatile ApplicationManager appMan;
	private volatile boolean initDone = false;
	
	protected abstract void controllerAndAppmanAvailable(C controller, ApplicationManager appMan);

	/** Call this method as soon as the ApplicationManager has been received*/
	public void setAppman(ApplicationManager appMan) {
		this.appMan = appMan;
		if(controller != null && (!initDone)) {
			initDone = true;
			controllerAndAppmanAvailable(controller, appMan);
		}
	}
	
	/** Call this when setController in {@link GenericExtensionProvider} is called
	 */
	public void setController(C controller) {
		this.controller = controller;
		if(appMan != null && (!initDone)) {
			initDone = true;
			controllerAndAppmanAvailable(controller, appMan);
		}
	}
}
