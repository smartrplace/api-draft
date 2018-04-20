package org.smartrplace.util.format;

import org.ogema.tools.resource.util.ResourceUtils;

public class WidgetHelper {
	public static String getValidWidgetId(String id) {
		return ResourceUtils.getValidResourceName(id).replace("$", "_");
	}
}
