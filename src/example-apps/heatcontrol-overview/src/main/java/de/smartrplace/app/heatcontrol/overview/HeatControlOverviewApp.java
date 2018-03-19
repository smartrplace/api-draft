package de.smartrplace.app.heatcontrol.overview;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.smartrplace.apps.heatcontrol.extensionapi.GUIInitDataProvider;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtPoint;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtRoomData;

import de.smartrplace.app.heatcontrol.overview.gui.MainPage;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

/**
 * Template OGEMA application class
 */
@Component(specVersion = "1.2", immediate = true)
@Service(Application.class)
public class HeatControlOverviewApp implements Application {
	public static final String urlPath = "/org/sp/app/heatoverview";

    private OgemaLogger log;
    private ApplicationManager appMan;
    private HeatControlOverviewController controller;

	private WidgetApp widgetApp;

	@Reference
	private OgemaGuiService guiService;
	@Reference
	public HeatControlExtPoint heatExtPoint;
	
	public MainPage mainPage;
	
	/*
     * This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appManager) {

        // Remember framework references for later.
        appMan = appManager;
        log = appManager.getLogger();

        // 
        controller = new HeatControlOverviewController(appMan, this);
		
		//register a web page with dynamically generated HTML
		widgetApp = guiService.createWidgetApp(urlPath, appManager);
		WidgetPage<?> page = widgetApp.createStartPage();
		HeatControlExtRoomData initData = null;
		if(controller.serviceAccess.heatExtPoint instanceof GUIInitDataProvider) {
			initData = ((GUIInitDataProvider)controller.serviceAccess.heatExtPoint).getInitObject(HeatControlExtRoomData.class);
		}
		mainPage = new MainPage(page, controller, initData);


		NavigationMenu menu = new NavigationMenu("Select Page");
		menu.addEntry("Overview Page", page);
		
		MenuConfiguration mc = page.getMenuConfiguration();
		mc.setCustomNavigation(menu);
     }

     /*
     * Callback called when the application is going to be stopped.
     */
    @Override
    public void stop(AppStopReason reason) {
    	if (widgetApp != null) widgetApp.close();
		if (controller != null)
    		controller.close();
        log.info("{} stopped", getClass().getName());
    }
 }
