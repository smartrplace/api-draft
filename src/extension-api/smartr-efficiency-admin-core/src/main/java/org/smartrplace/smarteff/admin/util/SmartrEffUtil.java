package org.smartrplace.smarteff.admin.util;

import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.util.format.WidgetHelper;

public class SmartrEffUtil {
	public static String buildId(SmartEffExtensionService service) {
		String id = service.id();
		if(id == null) return service.getClass().getName();
		else return id;
	}
	public static String buildId(ExtensionCapability service) {
		String id = service.id();
		if(id == null) return service.getClass().getName();
		else return id;
	}
	public static String buildValidWidgetId(ExtensionCapability service) {
		String id = WidgetHelper.getValidWidgetId(buildId(service));
		return id;
	}
	
	public static enum AccessType {
		PUBLIC,
		READONLY,
		READWRITE
	}
	public static AccessType getAccessType(ExtensionResourceType res) {
		if(res.getLocation().startsWith(SpEffAdminController.APPCONFIGDATA_LOCATION+"/userDataNonEdit"))
			return AccessType.READONLY;
		else if(res.getLocation().startsWith(SpEffAdminController.APPCONFIGDATA_LOCATION+"/generalData"))
			return AccessType.PUBLIC;
		else return AccessType.READWRITE; 
	}

}
