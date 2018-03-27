package org.smartrplace.smarteff.admin.object;

import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;

public class NavigationPageData {
	public final NavigationGUIProvider provider;
	public final SmartEffExtensionService parent;

	public NavigationPageData(NavigationGUIProvider provider, SmartEffExtensionService parent) {
		this.provider = provider;
		this.parent = parent;
	}
	
}
