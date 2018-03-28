package org.smartrplace.extensionservice.gui;

import org.smartrplace.extenservice.resourcecreate.ProviderPublicDataForCreate;

import de.iwes.widgets.template.LabelledItem;

public interface NavigationPublicPageData extends ProviderPublicDataForCreate, LabelledItem {

	/**Relative URL on which the page can be accessed*/
	String getUrl();
}
