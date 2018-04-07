package org.smartrplace.smarteff.admin.protect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForPageOpening;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PagePriority;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.smarteff.admin.util.ConfigIdAdministration;
import org.smartrplace.smarteff.admin.util.SmartrEffUtil;

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
		List<NavigationPublicPageData> resultAll = pageInfo.get(type);
		List<NavigationPublicPageData> resultRes = pageInfo.get(Resource.class);
		if(resultRes != null) resultAll.addAll(resultRes);
		if(resultAll == null) return Collections.emptyList();
		List<NavigationPublicPageData> result = new ArrayList<>();
		for(NavigationPublicPageData r: resultAll) {
			if(r.getPriority() != PagePriority.HIDDEN) result.add(r);
		}
		return result;
	}

	@Override
	public NavigationPublicPageData getMaximumPriorityPage(Class<? extends Resource> type, PageType pageType) {
		List<NavigationPublicPageData> resultAll = getPages(type);
		if(resultAll == null || resultAll.isEmpty()) return null;
		NavigationPublicPageData result = null;
		for(NavigationPublicPageData r: resultAll) {
			if(r.getPageType() != pageType) continue;
			if(result == null) result = r;
			else if(SmartrEffUtil.comparePagePriorities(r.getPriority(), result.getPriority()) > 0) {
				result = r;
			}
		}
		return result;
	}

	@Override
	public NavigationPublicPageData getPageByProvider(String url) {
		for(List<NavigationPublicPageData> list: pageInfo.values()) {
			for(NavigationPublicPageData navi: list) {
				if(navi.getUrl().equals(url)) return navi;
			}
		}
		return null;
	}

	@Override
	public String accessPage(NavigationPublicPageData pageData, int entryIdx,
			List<Resource> entryResources) {
		return configIdAdmin.getConfigId(entryIdx, entryResources);
	}
}
