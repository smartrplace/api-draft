package org.smartrplace.smarteff.admin.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPage;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.EntryType;
import org.smartrplace.smarteff.admin.SpEffAdminApp;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.admin.object.NavigationPageData;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData.ServiceCapabilities;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class GUIPageAdministation {
	public List<NavigationPageData> startPages = new ArrayList<>();
	public Map<Class<? extends ExtensionResourceType>, List<NavigationPageData>> navigationPages = new HashMap<>();
	private final SpEffAdminController app;
	
	public GUIPageAdministation(SpEffAdminController app) {
		this.app = app;
	}

	public NavigationPageData selectedStartPage;
	
	protected final static Logger logger = LoggerFactory.getLogger(SpEffAdminApp.class);
	
	public void registerService(SmartEffExtensionService service) {
    	ServiceCapabilities caps = SmartrEffExtResourceTypeData.getServiceCaps(service);
    	for(NavigationGUIProvider navi: caps.naviProviders) {
    		String id = WidgetHelper.getValidWidgetId(SmartrEffUtil.buildId(navi));
    		String url = WidgetHelper.getValidWidgetId(SmartrEffUtil.buildId(navi))+".html";
    		WidgetPage<?> page = app.widgetApp.createWidgetPage(url);
    		
  			ExtensionNavigationPage<SmartEffUserDataNonEdit> dataExPage = new ExtensionNavigationPage<SmartEffUserDataNonEdit>(page, url, "dataExplorer.html",
					id) {

				@Override
				protected List<SmartEffUserDataNonEdit> getUsers(OgemaHttpRequest req) {
					return app.appConfigData.userDataNonEdit().getAllElements();
				}
				@Override
				protected void init(OgemaHttpRequest req) {
					if(navi.getEntryType() == null) {
						SmartEffUserDataNonEdit userDataNonEdit = loggedIn.getSelectedItem(req);
						NavigationPageCallback listener = new NavigationPageCallback();
						navi.setUserData(-1, null, userDataNonEdit.editableData(),
								userDataNonEdit, listener, req);						
					} else {
						
					}
				}
			};
    		navi.initPage(dataExPage, app.appConfigData.generalData());
    		
    		NavigationPageData data = new NavigationPageData(navi, service, url, dataExPage);
			if(navi.getEntryType() == null) startPages.add(data);
			else for(EntryType t: navi.getEntryType()) {
				List<NavigationPageData> list = navigationPages.get(t.getType());
				if(list == null) {
					list = new ArrayList<>();
					navigationPages.put(t.getType(), list);
				}
				list.add(data);
			}
    	}
    	
	}
	
	public void unregisterService(SmartEffExtensionService service) {
    	ServiceCapabilities caps = SmartrEffExtResourceTypeData.getServiceCaps(service);
    	List<NavigationPageData> toRemove = new ArrayList<>();
    	String serviceId = SmartrEffUtil.buildId(service);
    	for(NavigationPageData navi: startPages) {
    		if(SmartrEffUtil.buildId(navi.parent).equals(serviceId)) toRemove.add(navi);
    	}
    	startPages.removeAll(toRemove);
    	for(NavigationGUIProvider navi: caps.naviProviders) {
    		String naviId = SmartrEffUtil.buildId(navi);
 			if(navi.getEntryType() == null) continue;
			else for(EntryType t: navi.getEntryType()) {
				List<NavigationPageData> list = navigationPages.get(t.getType());
				if(list == null) {
					logger.error("Navigationpages have no entry for "+t.getType().getName()+" when deregistering "+serviceId);
					continue;
				}
				for(NavigationPageData l:list) {
					if(SmartrEffUtil.buildId(l.provider).equals(naviId)) {
						list.remove(l);
						break;
					}
				}
				if(list.isEmpty()) navigationPages.remove(t.getType());
			}
    		
    	}
	}
	
	public Collection<NavigationPageData> getAllProviders() {
		 Set<NavigationPageData> result = new HashSet<>();
		 result.addAll(startPages);
		 for(List<NavigationPageData> navi: navigationPages.values()) result.addAll(navi);
		 return result;
	}
}
