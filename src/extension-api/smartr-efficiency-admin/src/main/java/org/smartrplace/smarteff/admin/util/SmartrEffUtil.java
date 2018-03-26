package org.smartrplace.smarteff.admin.util;

import org.smartrplace.efficiency.api.base.SmartEffExtensionService;

public class SmartrEffUtil {
	public static String buildId(SmartEffExtensionService service) {
		String id = service.id();
		if(id == null) return service.getClass().getName();
		else return id;
	}
}
