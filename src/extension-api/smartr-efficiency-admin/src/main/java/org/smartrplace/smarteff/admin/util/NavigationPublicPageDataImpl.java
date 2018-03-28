package org.smartrplace.smarteff.admin.util;

import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.smarteff.admin.object.NavigationPageData;

public class NavigationPublicPageDataImpl extends ProviderPublicDataForCreateImpl implements NavigationPublicPageData {
	private final NavigationPageData internalData;
	
	public NavigationPublicPageDataImpl(NavigationPageData internalData) {
		super(internalData.provider);
		this.internalData = internalData;
	}

	@Override
	public String getUrl() {
		return internalData.url;
	}

}
