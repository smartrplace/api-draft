package org.smartrplace.extenservice.resourcecreate;

import java.util.List;

import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;

public interface ExtensionPageSystemAccessForPageOpening {
	List<NavigationPublicPageData> getPages(Class<? extends ExtensionResourceType> type);
	
	/**Get configId to put as parameter into page request when opening new page*/
	String accessPage(NavigationPublicPageData pageData, int entryIdx, List<ExtensionResourceType> entryResources);
}
