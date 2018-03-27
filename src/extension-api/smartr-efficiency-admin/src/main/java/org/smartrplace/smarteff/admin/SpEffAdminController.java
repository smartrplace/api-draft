package org.smartrplace.smarteff.admin;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.gui.DataEntryProvider;
import org.smartrplace.smarteff.admin.config.SmartEffAdminData;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.admin.util.GUIPageAdministation;
import org.smartrplace.smarteff.admin.util.SmartrEffUtil;

import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class SpEffAdminController {
	public final static String APPCONFIGDATA_LOCATION = SmartEffAdminData.class.getSimpleName().substring(0, 1).toLowerCase()+SmartEffAdminData.class.getSimpleName().substring(1);
	
	public OgemaLogger log;
    public ApplicationManager appMan;

	public final SpEffAdminApp serviceAccess;
	public SmartEffAdminData appConfigData;
	public Set<SmartEffExtensionService> servicesKnown = new HashSet<>();
	public Map<Class<? extends SmartEffExtensionResourceType>, SmartrEffExtResourceTypeData> resourceTypes = new HashMap<>();
	public GUIPageAdministation guiPageAdmin = new GUIPageAdministation();
	
    public SpEffAdminController(ApplicationManager appMan,SpEffAdminApp evaluationOCApp) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.serviceAccess = evaluationOCApp;
		
		initConfigurationResource();
	}

    private void initConfigurationResource() {
		String configResourceDefaultName = APPCONFIGDATA_LOCATION;
		appConfigData = appMan.getResourceAccess().getResource(configResourceDefaultName);
		if (appConfigData != null) { // resource already exists (appears in case of non-clean start)
			appMan.getLogger().debug("{} started with previously-existing config resource", getClass().getName());
		}
		else {
			appConfigData = (SmartEffAdminData) appMan.getResourceManagement().createResource(configResourceDefaultName,SmartEffAdminData.class);
			appConfigData.activate(true);
			appMan.getLogger().debug("{} started with new config resource", getClass().getName());
		}
    }

    public void processNewService(SmartEffExtensionService service) {
    	servicesKnown.add(service);
     	
    	for(ExtensionResourceTypeDeclaration<? extends SmartEffExtensionResourceType> rtd: service.resourcesDefined()) {
    		Class<? extends SmartEffExtensionResourceType> rt = rtd.resourceType();
    		SmartrEffExtResourceTypeData data = resourceTypes.get(rt);
    		if(data == null) {
    			data = new SmartrEffExtResourceTypeData(rtd, service, this);
    			resourceTypes.put(rt, data );    			
    		} else data.addParent(service);
    	}
    	guiPageAdmin.registerService(service);
    }
    
    public void unregisterService(SmartEffExtensionService service) {
    	servicesKnown.remove(service);
    	for(ExtensionResourceTypeDeclaration<? extends SmartEffExtensionResourceType> rtd: service.resourcesDefined()) {
    		Class<? extends SmartEffExtensionResourceType> rt = rtd.resourceType();
    		SmartrEffExtResourceTypeData data = resourceTypes.get(rt);
    		if(data == null) {
    			//should not occur
    			log.error("Resource type "+rt.getName()+" not found when service "+SmartrEffUtil.buildId(service)+ "unregistered!");
    		} else if(data.removeParent(service)) resourceTypes.remove(rt);
    	}
    	guiPageAdmin.unregisterService(service);
    }
    
    
	public void close() {
    }

	/** Here the action is performed without checking user permissions*/
	public <T extends SmartEffExtensionResourceType> T addResource(SmartEffExtensionResourceType parent,
			String name, Class<T> type, SmartEffUserDataNonEdit userData, DataEntryProvider<T> entryProvider) {
		T result = parent.getSubResource(name, type);
		result.create();
		entryProvider.initResource(result);
		SmartrEffExtResourceTypeData rtd = resourceTypes.get(type);
		rtd.registerElement(result);
		return result;
	}
	public void removeResource(SmartEffExtensionResourceType object) {
		// TODO Auto-generated method stub
		
	}
}
