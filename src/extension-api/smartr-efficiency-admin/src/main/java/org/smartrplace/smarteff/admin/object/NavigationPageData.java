package org.smartrplace.smarteff.admin.object;

import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPage;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;

public class NavigationPageData {
	public final NavigationGUIProvider provider;
	public final SmartEffExtensionService parent;
	public final String url;
	public final ExtensionNavigationPage dataExPage;
	
	public NavigationPageData(NavigationGUIProvider provider, SmartEffExtensionService parent, String url,
			final ExtensionNavigationPage dataExPage) {
		this.provider = provider;
		this.parent = parent;
		this.url = url;
		this.dataExPage = dataExPage;
	}
	
}
