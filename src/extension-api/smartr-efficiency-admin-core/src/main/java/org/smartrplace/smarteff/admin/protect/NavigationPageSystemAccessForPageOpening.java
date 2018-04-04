package org.smartrplace.smarteff.admin.protect;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForPageOpening;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.smarteff.admin.util.ConfigIdAdministration;

public class NavigationPageSystemAccessForPageOpening implements ExtensionPageSystemAccessForPageOpening {
	protected final Map<Class<? extends ExtensionResourceType>, List<NavigationPublicPageData>> pageInfo;
	protected final ConfigIdAdministration configIdAdmin;
	
	public NavigationPageSystemAccessForPageOpening(
			Map<Class<? extends ExtensionResourceType>, List<NavigationPublicPageData>> pageInfo,
			ConfigIdAdministration configIdAdmin) {
		this.pageInfo = pageInfo;
		this.configIdAdmin = configIdAdmin;
	}

	@Override
	public List<NavigationPublicPageData> getPages(Class<? extends ExtensionResourceType> type) {
		List<NavigationPublicPageData> result = pageInfo.get(type);
		if(result == null) return Collections.emptyList();
		return result;
	}
	
	@Override
	public String accessPage(NavigationPublicPageData pageData, int entryIdx,
			List<ExtensionResourceType> entryResources) {
		return configIdAdmin.getConfigId(entryIdx, entryResources);
	}
}
