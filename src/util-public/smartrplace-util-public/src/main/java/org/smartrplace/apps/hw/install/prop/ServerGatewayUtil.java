package org.smartrplace.apps.hw.install.prop;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.smartrplace.system.guiappstore.config.AppstoreConfig;
import org.smartrplace.system.guiappstore.config.AppstoreSystemUpdates;
import org.smartrplace.system.guiappstore.config.GatewayData;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;

public class ServerGatewayUtil {
    public static AppstoreConfig initAppstoreConfigurationResource(ApplicationManager appMan) {
		//TODO provide Util?
		String name = AppstoreConfig.class.getSimpleName().substring(0, 1).toLowerCase()+AppstoreConfig.class.getSimpleName().substring(1);
		AppstoreConfig appConfigData = appMan.getResourceAccess().getResource(name);
		if (appConfigData != null) { // resource already exists (appears in case of non-clean start)
			appMan.getLogger().debug("{} started with previously-existing config resource", name);
		}
		else {
			appConfigData = (AppstoreConfig) appMan.getResourceManagement().createResource(name, AppstoreConfig.class);
			//appConfigData.appData().create();
			//appConfigData.appGroupData().create();
			appConfigData.gatewayData().create();
			appConfigData.gatewayGroupData().create();
			//appConfigData.systemUpdates().create();

			appConfigData.testingGroup().create();
			ValueResourceHelper.setCreate(appConfigData.testingGroup().name(), "Testing Group");
			appConfigData.gatewayGroupData().add(appConfigData.testingGroup());
			appConfigData.mainGroup().create();
			ValueResourceHelper.setCreate(appConfigData.mainGroup().name(), "Main Group");
			appConfigData.gatewayGroupData().add(appConfigData.mainGroup());
			
			appConfigData.activate(true);
			appMan.getLogger().debug("{} started with new config resource", name);
		}
		return appConfigData;
    }

    public static AppstoreSystemUpdates initAppstoreSystemUpdates(ApplicationManager appMan) {
		//TODO provide Util?
		String name = AppstoreSystemUpdates.class.getSimpleName().substring(0, 1).toLowerCase()+AppstoreSystemUpdates.class.getSimpleName().substring(1);
		AppstoreSystemUpdates appConfigData = appMan.getResourceAccess().getResource(name);
		if (appConfigData != null) { // resource already exists (appears in case of non-clean start)
			appMan.getLogger().debug("{} started with previously-existing config resource", name);
		}
		else {
			appConfigData = (AppstoreSystemUpdates) appMan.getResourceManagement().createResource(name, AppstoreSystemUpdates.class);
			appConfigData.appData().create();
			appConfigData.systemUpdates().create();
			
			appConfigData.activate(true);
			appMan.getLogger().debug("{} started with new config resource", name);
		}
		Integer initCurrentLastPart = Integer.getInteger("org.smartrplace.apps.hw.install.prop.initForDebug", null);
		if(initCurrentLastPart != null) {
			ValueResourceHelper.setCreate(appConfigData.currentLastVersionPart(), initCurrentLastPart);
		}
		return appConfigData;
    }
    
    public static GatewayData getOrCreateGwData(String gwId, ResourceList<GatewayData> appstoreData) {
    	List<String> gwIds = ViaHeartbeatUtil.getAlternativeGwIds(gwId);
    	for(String gi: gwIds) {
        	GatewayData gw = ResourceListHelper.getNamedElementFlex(gi, appstoreData);
        	if(gw != null)
        		return gw;
    	}
    	GatewayData gw = ResourceListHelper.getOrCreateNamedElement(gwId, appstoreData);
		if(ValueResourceHelper.setIfNew(gw.remoteSlotsGatewayId(), gwId)) {
			gw.activate(true);
		}
    	return gw;
    }
    
    public static String getGatewayBaseUrl(GatewayData gwd) {
    	if(gwd.guiLink().isActive() && (!gwd.guiLink().getValue().isEmpty()))
    		return gwd.guiLink().getValue();
    	String customer = gwd.customer().getValue();
    	if(customer != null && !customer.isEmpty())
    		return "https://"+customer+".smartrplace.de";
    	return null;
    }
}
