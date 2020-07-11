package org.smartrplace.apps.todoappname;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.devicefinder.api.DatapointService;
import org.smartrplace.apps.todoappname.gui.MainPage;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;

import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;

// here the controller logic is implemented
public class TodoTemplateController {

	public final OgemaLogger log;
    public final ApplicationManager appMan;
    /** This will not be available in the constructor*/
    public final UserPermissionService userPermService;
    public final DatapointService dpService;
    
	public AccessAdminConfig appConfigData;
	public TodoTemplateApp accessAdminApp;
    public final ApplicationManagerPlus appManPlus;
	
	public MainPage mainPage;
	WidgetApp widgetApp;

	
	public TodoTemplateController(ApplicationManager appMan, TodoTemplateApp initApp) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.accessAdminApp = initApp;
		this.userPermService = initApp.userAccService;
		this.dpService = initApp.dpService;
		this.appManPlus = new ApplicationManagerPlus(appMan);
		appManPlus.setPermMan(initApp.permMan);
		appManPlus.setUserPermService(userPermService);
		appManPlus.setDpService(dpService);
		
		WidgetPage<?> pageRes10 = initApp.widgetApp.createWidgetPage("mainpage.html", true);
		mainPage = new MainPage(pageRes10, this);
		initApp.menu.addEntry("Room Setup", pageRes10);
		initApp.configMenuConfig(pageRes10.getMenuConfiguration());

		// TODO: If you need more than one page see how to add more pages as the template commented out below
		// You have to implement a class for each page.
		
		//WidgetPage<?> pageRes11 = initApp.widgetApp.createWidgetPage("usersetup.html", false);
		//mainPage2 = new MainPage2(pageRes11, this);
		//initApp.menu.addEntry("User Setup", pageRes11);
		//initApp.configMenuConfig(pageRes11.getMenuConfiguration());

		initDemands();
	}

     /*
     * register ResourcePatternDemands. The listeners will be informed about new and disappearing
     * patterns in the OGEMA resource tree
     */
    public void initDemands() {
    }

	public void close() {
	}
}
