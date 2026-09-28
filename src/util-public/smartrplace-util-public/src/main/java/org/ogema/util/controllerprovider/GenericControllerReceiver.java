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

	/** Called when a new controller is received after {@link #controllerAndAppmanAvailable(Object, ApplicationManager)}
	 * has already been called, e.g. because the bundle providing the controller has been restarted. In this case
	 * everything created based on the old controller (usually the WidgetApp) is invalid. Override this to release
	 * all of it (e.g. widgetApp.close()) and return true, then {@link #controllerAndAppmanAvailable(Object, ApplicationManager)}
	 * is called again with the new controller. By default nothing is released and the new controller is not used for
	 * initialization.
	 * @param oldController controller used for the previous initialization
	 * @return true if the resources have been released and initialization shall be performed again
	 */
	protected boolean releaseForNewController(C oldController) {
		return false;
	}
	
	/** Call this method as soon as the ApplicationManager has been received*/
	public synchronized void setAppman(ApplicationManager appMan) {
		this.appMan = appMan;
		if(controller != null && (!initDone)) {
			initDone = true;
			controllerAndAppmanAvailable(controller, appMan);
		}
	}
	
	/** Call this when setController in {@link GenericExtensionProvider} is called
	 */
	public synchronized void setController(C controller) {
		C oldController = this.controller;
		this.controller = controller;
		if(appMan == null || controller == null)
			return;
		if(!initDone) {
			initDone = true;
			controllerAndAppmanAvailable(controller, appMan);
		} else if(oldController != controller && releaseForNewController(oldController)) {
			controllerAndAppmanAvailable(controller, appMan);
		}
	}
}
