package org.smartrplace.smarteff.admin.protect;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForPageOpening;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.smarteff.admin.util.ConfigIdAdministration;

public class NavigationPageSystemAccessForPageOpening implements ExtensionPageSystemAccessForPageOpening {
	protected final Map<Class<? extends Resource>, List<NavigationPublicPageData>> pageInfo;
	protected final ConfigIdAdministration configIdAdmin;
	
	public NavigationPageSystemAccessForPageOpening(
			Map<Class<? extends Resource>, List<NavigationPublicPageData>> pageInfo,
			ConfigIdAdministration configIdAdmin) {
		this.pageInfo = pageInfo;
		this.configIdAdmin = configIdAdmin;
	}

	@Override
	public List<NavigationPublicPageData> getPages(Class<? extends Resource> type) {
		List<NavigationPublicPageData> result = pageInfo.get(type);
		if(result == null) return Collections.emptyList();
		return result;
	}
	
	@Override
	public String accessPage(NavigationPublicPageData pageData, int entryIdx,
			List<Resource> entryResources) {
		return configIdAdmin.getConfigId(entryIdx, entryResources);
	}
}
