package org.smartrplace.smarteff.defaultservice;

import org.ogema.core.model.Resource;

public class CapabilityHelper {
	public static final String ERROR_START = "ERROR: ";
	
	public static String getnewDecoratorName(String baseName, Resource parent) {
		return getnewDecoratorName(baseName, parent, "_");
	}
	public static String getnewDecoratorName(String baseName, Resource parent, String separator) {
		int i=0;
		String name = baseName+separator+i;
		while(parent.getSubResource(name) != null) {
			i++;
			name = baseName+separator+i;
		}
		return name;
	}

}
