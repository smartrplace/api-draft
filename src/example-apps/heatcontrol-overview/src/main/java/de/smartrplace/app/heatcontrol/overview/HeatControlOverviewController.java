package de.smartrplace.app.heatcontrol.overview;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;

public class HeatControlOverviewController {

	public OgemaLogger log;
    public ApplicationManager appMan;

	public final HeatControlOverviewApp serviceAccess;
	
    public HeatControlOverviewController(ApplicationManager appMan,HeatControlOverviewApp evaluationOCApp) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.serviceAccess = evaluationOCApp;
	}

    
	public void close() {
    }
}
