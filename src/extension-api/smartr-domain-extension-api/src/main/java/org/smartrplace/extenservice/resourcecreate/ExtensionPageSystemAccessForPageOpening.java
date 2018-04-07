package org.smartrplace.extenservice.resourcecreate;

import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;

public interface ExtensionPageSystemAccessForPageOpening {
	List<NavigationPublicPageData> getPages(Class<? extends Resource> type);
	NavigationPublicPageData getMaximumPriorityPage(Class<? extends Resource> type, PageType pageType);
	NavigationPublicPageData getPageByProvider(String url);
	
	/**Get configId to put as parameter into page request when opening new page*/
	String accessPage(NavigationPublicPageData pageData, int entryIdx, List<Resource> entryResources);
}
