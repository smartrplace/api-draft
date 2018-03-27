package org.smartrplace.smarteff.admin;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.smarteff.admin.gui.DataExplorerPage;
import org.smartrplace.smarteff.admin.gui.ResTypePage;
import org.smartrplace.smarteff.admin.gui.ServiceDetailPage;
import org.smartrplace.smarteff.admin.gui.ServicePage;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.admin.util.SmartrEffUtil;
import org.smartrplace.smarteff.defaultservice.BaseDataService;

import de.iwes.timeseries.eval.base.provider.BasicEvaluationProvider;
import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;

/**
 * Template OGEMA application class
 */
@References({
	@Reference(
		name="evaluationProviders",
		referenceInterface=SmartEffExtensionService.class,
		cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
		policy=ReferencePolicy.DYNAMIC,
		bind="addProvider",
		unbind="removeProvider"),
})
@Component(specVersion = "1.2", immediate = true)
@Service(Application.class)
public class SpEffAdminApp implements Application {
	public static final String urlPath = "/org/sp/smarteff/admin";

    private OgemaLogger log;
    private ApplicationManager appMan;
    private SpEffAdminController controller;

	private WidgetApp widgetApp;

	@Reference
	private OgemaGuiService guiService;
	
	public ServicePage mainPage;
	public ServiceDetailPage offlineEvalPage;
	public ResTypePage resTypePage;
	public DataExplorerPage dataExPage;
	
	private final Map<String,SmartEffExtensionService> evaluationProviders = Collections.synchronizedMap(new LinkedHashMap<String,SmartEffExtensionService>());
	public Map<String, SmartEffExtensionService> getEvaluations() {
		return evaluationProviders;
	}

	public BasicEvaluationProvider basicEvalProvider = null;

	/*
     * This is the entry point to the application.
     */
 	@Override
    public void start(ApplicationManager appManager) {

        // Remember framework references for later.
        appMan = appManager;
        log = appManager.getLogger();

        // 
        controller = new SpEffAdminController(appMan, this);
		
		//register a web page with dynamically generated HTML
		widgetApp = guiService.createWidgetApp(urlPath, appManager);
		WidgetPage<?> page = widgetApp.createStartPage();
		mainPage = new ServicePage(page, controller);
		WidgetPage<?> page1 = widgetApp.createWidgetPage("Details.html");
		offlineEvalPage = new ServiceDetailPage(page1, controller);
		WidgetPage<?> page2 = widgetApp.createWidgetPage("resTypes.html");
		SmartrEffExtResourceTypeData rtd = new SmartrEffExtResourceTypeData(BaseDataService.BUILDING_DATA, null, null);
		resTypePage = new ResTypePage(page2, controller, rtd );
		WidgetPage<?> page3 = widgetApp.createWidgetPage("dataExplorer.html");
		dataExPage = new DataExplorerPage(page3, controller, controller.appConfigData.generalData());

		NavigationMenu menu = new NavigationMenu("Select Page");
		menu.addEntry("Overview Page", page);
		menu.addEntry("Details Page", page1);
		
		MenuConfiguration mc = page.getMenuConfiguration();
		mc.setCustomNavigation(menu);
		mc = page1.getMenuConfiguration();
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
    
    protected void addProvider(SmartEffExtensionService provider) {
    	evaluationProviders.put(SmartrEffUtil.buildId(provider), provider);
    }
    
    protected void removeProvider(SmartEffExtensionService provider) {
    	evaluationProviders.remove(SmartrEffUtil.buildId(provider));
    }

}
