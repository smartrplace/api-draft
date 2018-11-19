package org.smartrplace.tissue.util.resource;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;

/**
 * OGEMA application class providing an application manager for resource operations without
 * requiring calling applications having the required permissions itself
 */
@Component(specVersion = "1.2", immediate = true)
@Service(Application.class)
public class UtilExtendedApp implements Application {
    private static ApplicationManager appMan = null;

    /*
     * This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appManager) {

        // Remember framework references for later.
        appMan = appManager;
     }

     /*
     * Callback called when the application is going to be stopped.
     */
    @Override
    public void stop(AppStopReason reason) {
    	appMan = null;
    }
    
    static ApplicationManager getApplicationManager() {
    	return appMan;
    }
}
