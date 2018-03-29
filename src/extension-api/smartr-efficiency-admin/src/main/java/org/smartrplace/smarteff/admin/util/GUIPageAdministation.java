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
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPage;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.smarteff.admin.SpEffAdminApp;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.admin.object.NavigationPageData;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData.ServiceCapabilities;
import org.smartrplace.smarteff.admin.protect.ExtensionResourceAccessInitDataImpl;
import org.smartrplace.smarteff.admin.protect.NavigationPageSystemAccess;
import org.smartrplace.smarteff.admin.protect.NavigationPublicPageDataImpl;
import org.smartrplace.smarteff.admin.util.ConfigIdAdministration.ConfigInfo;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.navigation.MenuConfiguration;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class GUIPageAdministation {
	public List<NavigationPageData> startPages = new ArrayList<>();
	public List<NavigationPageData> navigationPages = new ArrayList<>();
	public Map<Class<? extends ExtensionResourceType>, List<NavigationPublicPageData>> navigationPublicData = new HashMap<>();
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
    		NavigationMenu menu = app.getNavigationMenu();
    		MenuConfiguration mc = page.getMenuConfiguration();
    		mc.setCustomNavigation(menu);
  		
  			ExtensionNavigationPage<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> dataExPage = new ExtensionNavigationPage<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData>(page, url, "dataExplorer.html",
					id) {

				@Override
				protected List<SmartEffUserDataNonEdit> getUsers(OgemaHttpRequest req) {
					return app.appConfigData.userDataNonEdit().getAllElements();
				}
				@Override
				protected ExtensionResourceAccessInitData getItemById(String configId, OgemaHttpRequest req) {
					SmartEffUserDataNonEdit userDataNonEdit = loggedIn.getSelectedItem(req);
					NavigationPageSystemAccess systemAccess = new NavigationPageSystemAccess(userDataNonEdit.ogemaUserName().getValue(),
							navi.label(req.getLocale()),
							navigationPublicData, app.lockAdmin, app.configIdAdmin, app.typeAdmin, app.appManExt);
					if(navi.getEntryTypes() == null || configId == null) {
						ExtensionResourceAccessInitData result = new ExtensionResourceAccessInitDataImpl(-1, null,
								userDataNonEdit.editableData().getLocationResource(), userDataNonEdit, systemAccess);
						return result;
					} else {
						ConfigInfo c = app.configIdAdmin.getConfigInfo(configId);
						ExtensionResourceAccessInitData result = new ExtensionResourceAccessInitDataImpl(c.entryIdx,
								c.entryResources,
								userDataNonEdit.editableData().getLocationResource(), userDataNonEdit, systemAccess);
						return result;
					}
				}
			};
    		navi.initPage(dataExPage, app.appManExt);
    		
    		NavigationPageData data = new NavigationPageData(navi, service, url, dataExPage);
			if(navi.getEntryTypes() == null) startPages.add(data);
			else {
				navigationPages.add(data);
				for(EntryType t: navi.getEntryTypes()) {
					List<NavigationPublicPageData> listPub = navigationPublicData.get(t.getType());
					if(listPub == null) {
						listPub = new ArrayList<>();
						navigationPublicData.put(t.getType(), listPub);
					}
					NavigationPublicPageData dataPub = new NavigationPublicPageDataImpl(data);
					listPub.add(dataPub);
				}
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
    	navigationPages.removeAll(toRemove);
    	for(NavigationGUIProvider navi: caps.naviProviders) {
    		String naviId = SmartrEffUtil.buildId(navi);
 			if(navi.getEntryTypes() == null) continue;
			else for(EntryType t: navi.getEntryTypes()) {
				List<NavigationPublicPageData> listPub = navigationPublicData.get(t.getType());
				if(listPub == null) {
					logger.error("Navigation Public pages have no entry for "+t.getType().getName()+" when deregistering "+serviceId);
					continue;
				}
				for(NavigationPublicPageData l:listPub) {
					if(l.id().equals(naviId)) {
						listPub.remove(l);
						break;
					}
				}
				if(listPub.isEmpty()) navigationPublicData.remove(t.getType());
			}
    		
    	}
	}
	
	public Collection<NavigationPageData> getAllProviders() {
		 Set<NavigationPageData> result = new HashSet<>();
		 result.addAll(startPages);
		 result.addAll(navigationPages);
		 return result;
	}
}
