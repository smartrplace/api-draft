package org.smartrplace.smarteff.admin.object;

import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPage;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;

import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class NavigationPageData {
	public final NavigationGUIProvider provider;
	public final SmartEffExtensionService parent;
	public final String url;
	public final ExtensionNavigationPage<SmartEffUserDataNonEdit> dataExPage;
	
	public NavigationPageData(NavigationGUIProvider provider, SmartEffExtensionService parent, String url,
			final ExtensionNavigationPage<SmartEffUserDataNonEdit> dataExPage) {
		this.provider = provider;
		this.parent = parent;
		this.url = url;
		this.dataExPage = dataExPage;
	}
	
}
