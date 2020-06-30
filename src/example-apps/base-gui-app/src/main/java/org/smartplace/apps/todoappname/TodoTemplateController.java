package org.smartplace.apps.todoappname;

import java.util.ArrayList;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.accessadmin.api.UserPermissionService;
import org.ogema.core.administration.UserAccount;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.model.locations.BuildingPropertyUnit;
import org.ogema.model.locations.Room;
import org.smartrplace.apps.todoappname.gui.MainPage;
import org.smartrplace.external.accessadmin.config.AccessAdminConfig;
import org.smartrplace.external.accessadmin.config.AccessConfigUser;
import org.smartrplace.gui.filtering.GenericFilterFixedGroup;

import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;

// here the controller logic is implemented
public class TodoTemplateController {

	public OgemaLogger log;
    public ApplicationManager appMan;
    /** This will not be available in the constructor*/
    public UserPermissionService userPermService;
    
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
		this.appManPlus = new ApplicationManagerPlus(appMan);
		appManPlus.setPermMan(initApp.permMan);
		appManPlus.setUserPermService(userPermService);
		
		initConfigurationResource();

		//mainPage = new MainPage(page, appMan);

		WidgetPage<?> pageRes10 = initApp.widgetApp.createWidgetPage("mainpage.html", true);
		mainPage = new MainPage(pageRes10, this);
		initApp.menu.addEntry("Room Setup", pageRes10);
		initApp.configMenuConfig(pageRes10.getMenuConfiguration());

		//WidgetPage<?> pageRes11 = initApp.widgetApp.createWidgetPage("usersetup.html", false);
		//mainPage2 = new MainPage2(pageRes11, this);
		//initApp.menu.addEntry("User Setup", pageRes11);
		//initApp.configMenuConfig(pageRes11.getMenuConfiguration());

		initDemands();
	}

    /*
     * This app uses a central configuration resource, which is accessed here
     */
    private void initConfigurationResource() {
		//TODO provide Util?
		String name = AccessAdminConfig.class.getSimpleName().substring(0, 1).toLowerCase()+AccessAdminConfig.class.getSimpleName().substring(1);
		appConfigData = appMan.getResourceAccess().getResource(name);
		if (appConfigData != null) { // resource already exists (appears in case of non-clean start)
			appMan.getLogger().debug("{} started with previously-existing config resource", getClass().getName());
		}
		else {
			appConfigData = (AccessAdminConfig) appMan.getResourceManagement().createResource(name, AccessAdminConfig.class);
			appConfigData.name().create();
			//TODO provide different sample, provide documentation in code
			appConfigData.name().setValue("sampleName");
			appConfigData.activate(true);
			appMan.getLogger().debug("{} started with new config resource", getClass().getName());
		}
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
