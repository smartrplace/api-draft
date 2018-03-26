package org.smartrplace.smarteff.admin;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;

public class SpEffAdminController {

	public OgemaLogger log;
    public ApplicationManager appMan;

	public final SpEffAdminApp serviceAccess;
	
    public SpEffAdminController(ApplicationManager appMan,SpEffAdminApp evaluationOCApp) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.serviceAccess = evaluationOCApp;
	}

    
	public void close() {
    }
}
