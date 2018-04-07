package org.smartrplace.extensionservice.gui;

import org.smartrplace.extenservice.resourcecreate.ProviderPublicDataForCreate;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PagePriority;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;

import de.iwes.widgets.template.LabelledItem;

public interface NavigationPublicPageData extends ProviderPublicDataForCreate, LabelledItem {

	/**Relative URL on which the page can be accessed*/
	String getUrl();
	
	/** see {@link PageType}*/
	PageType getPageType();
	
	/** see {@link PagePriority}*/
	PagePriority getPriority();
}
