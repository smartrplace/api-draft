package org.smartrplace.smarteff.admin;

import java.util.HashSet;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.smarteff.admin.config.SmartEffAdminData;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.admin.util.ConfigIdAdministration;
import org.smartrplace.smarteff.admin.util.GUIPageAdministation;
import org.smartrplace.smarteff.admin.util.ResourceLockAdministration;
import org.smartrplace.smarteff.admin.util.TypeAdministration;
import org.smartrplace.util.format.ValueFormat;

import de.iwes.widgets.api.widgets.WidgetApp;
import de.iwes.widgets.api.widgets.navigation.NavigationMenu;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class SpEffAdminController {
	public final static String APPCONFIGDATA_LOCATION = ValueFormat.firstLowerCase(SmartEffAdminData.class.getSimpleName());
	
	public final ServiceAccess serviceAccess;
	public final OgemaLogger log;
    public final ApplicationManager appMan;
    public final WidgetApp widgetApp;

	public Set<SmartEffExtensionService> servicesKnown = new HashSet<>();
	public final GUIPageAdministation guiPageAdmin;

	public ResourceLockAdministration lockAdmin = new ResourceLockAdministration();
	public ConfigIdAdministration configIdAdmin = new ConfigIdAdministration();
	public TypeAdministration typeAdmin;
	private UserAdmin userAdmin;
	
	public final ApplicationManagerSPExt appManExt = new ApplicationManagerSPExt() {
		
		@SuppressWarnings("unchecked")
		@Override
		public <T extends ExtensionResourceType> ExtensionResourceTypeDeclaration<T> getTypeDeclaration(
				Class<T> resourceType) {
			return (ExtensionResourceTypeDeclaration<T>) typeAdmin.resourceTypes.get(resourceType).typeDeclaration;
		}
		
		@Override
		public ExtensionResourceType generalData() {
			return userAdmin.getAppConfigData().generalData();
		}

		@Override
		public long getFrameworkTime() {
			if(appMan != null) return appMan.getFrameworkTime();
			return -1;
		}

		@Override
		public OgemaLogger log() {
			if(appMan != null) return appMan.getLogger();
			return null;
		}
	};
	
    public SpEffAdminController(ApplicationManager appMan, ServiceAccess evaluationOCApp, final WidgetApp widgetApp) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.widgetApp = widgetApp;
		this.serviceAccess = evaluationOCApp;
		userAdmin = new UserAdmin(this);
		this.typeAdmin = new TypeAdministration(this);
		guiPageAdmin = new GUIPageAdministation(this);
	}
    
    public void processOpenServices() {
		for(SmartEffExtensionService service: serviceAccess.getEvaluations().values()) {
			processNewService(service);
		}    	
    }

    public void processNewService(SmartEffExtensionService service) {
    	servicesKnown.add(service);
     	
    	typeAdmin.registerService(service);
    	guiPageAdmin.registerService(service);
    	service.start(appManExt);
    }
    
    public void unregisterService(SmartEffExtensionService service) {
    	servicesKnown.remove(service);
    	typeAdmin.unregisterService(service);
    	guiPageAdmin.unregisterService(service);
    }
    
    
	public void close() {
    }

	/** Here the action is performed without checking user permissions*/
	public <T extends SmartEffExtensionResourceType> T addResource(SmartEffExtensionResourceType parent,
			String name, Class<T> type, SmartEffUserDataNonEdit userData, NavigationGUIProvider entryProvider) {
		T result = parent.getSubResource(name, type);
		result.create();
		//entryProvider.initResource(result);
		SmartrEffExtResourceTypeData rtd = typeAdmin.resourceTypes.get(type);
		rtd.registerElement(result);
		return result;
	}
	public void removeResource(SmartEffExtensionResourceType object) {
		// TODO Auto-generated method stub
		
	}

	public NavigationMenu getNavigationMenu() {
		return serviceAccess.getMenu();
	}
	
	public UserAdmin getUserAdmin() {
		return userAdmin;
	}
}
