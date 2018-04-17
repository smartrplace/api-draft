package org.smartrplace.smarteff.util;

import java.util.Arrays;
import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extenservice.proposal.ProjectProposal;
import org.smartrplace.extenservice.proposal.ProposalPublicData;
import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForCreate;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration.Cardinality;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.smarteff.defaultservice.BaseDataService;
import org.smartrplace.smarteff.defaultservice.ResultTablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public class SPPageUtil {
	public static OgemaWidget addOpenButton(String columnName, Resource object,
			ObjectResourceGUIHelper<?,?> vh, String id, Row row,
			NavigationPublicPageData pageData,
			ExtensionPageSystemAccessForCreate systemAccess,
			String text, String alternativeText, boolean openWhenLocked,
			PageType pageType) {
		if(pageData != null) {
			if((!systemAccess.isLocked(object)) || openWhenLocked) {
				//String configId = NaviOpenButton.getConfigId(pageType, object, type, systemAccess, pageData);
				//Here we never create a new resource
				Class<? extends Resource> type = object.getResourceType();
				String configId = systemAccess.accessPage(pageData, getEntryIdx(pageData, type),
						Arrays.asList(new Resource[]{object}));
				return vh.linkingButton(columnName, id, null, row, text, pageData.getUrl()+"?configId="+configId);
			} else {
				return vh.stringLabel(columnName, id, alternativeText, row);						
			}
		} else {
			return vh.stringLabel(columnName, id, alternativeText+"*", row);
		}
	}
	public static OgemaWidget addResEditOpenButton(String columnName, Resource object,
			ObjectResourceGUIHelper<?,?> vh, String id, Row row,
			ExtensionResourceAccessInitData appData) {
		if(appData != null) {
			NavigationPublicPageData pageData = getPageData(appData, object.getResourceType(), PageType.EDIT_PAGE);
			return addOpenButton(columnName, object, vh, id, row, pageData, appData.systemAccess(),
					"Edit", "Locked", false, PageType.EDIT_PAGE);
		} else {
			vh.registerHeaderEntry(columnName);
			return null;
		}
	}
	public static OgemaWidget addResTableOpenButton(String columnName, Resource object,
			ObjectResourceGUIHelper<?,?> vh, String id, Row row,
			ExtensionResourceAccessInitData appData) {
		if(appData != null) {
			NavigationPublicPageData pageData = getPageData(appData, object.getResourceType(), PageType.TABLE_PAGE);
			return addOpenButton(columnName, object, vh, id, row, pageData, appData.systemAccess(),
					"Open Table", "No Page", true, PageType.TABLE_PAGE);
		} else {
			vh.registerHeaderEntry(columnName);
			return null;
		}
	}
	public static OgemaWidget addProviderTableOpenButton(String columnName, Resource object,
			ObjectResourceGUIHelper<?,?> vh, String id, Row row,
			ExtensionResourceAccessInitData appData) {
		if(appData != null) {
			List<ProposalPublicData> provs = appData.systemAccessForPageOpening().getProposalProviders(object.getResourceType());
			if(provs.isEmpty()) {
				return vh.stringLabel("Evaluations", id, "No Providers", row);
			} else {
				NavigationPublicPageData pageData = appData.systemAccessForPageOpening().getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.PROPOSALTABLE_PROVIDER));
				return addOpenButton(columnName, object, vh, id, row, pageData, appData.systemAccess(),
						"Evaluations", "No BaseEval", true, PageType.TABLE_PAGE);
			}
		} else {
			vh.registerHeaderEntry(columnName);
			return null;
		}
	}
	public static OgemaWidget addResultTableOpenButton(String columnName, Resource object,
			ObjectResourceGUIHelper<?,?> vh, String id, Row row,
			ExtensionResourceAccessInitData appData) {
		if(appData != null) {
			List<ProjectProposal> resultsAvail = object.getSubResources(ResultTablePage.TYPE_SHOWN, true);
			if(resultsAvail.isEmpty()) {
				return vh.stringLabel("Results", id, "No Results", row);
			} else {
				NavigationPublicPageData pageData = appData.systemAccessForPageOpening().getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.RESULTTABLE_PROVIDER));
				return addOpenButton(columnName, object, vh, id, row, pageData, appData.systemAccess(),
						"Results", "No BaseResult", true, PageType.TABLE_PAGE);
			}
		} else {
			vh.registerHeaderEntry(columnName);
			return null;
		}
	}
	
	static NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
			Class<? extends Resource> type, PageType typeRequested) {
		return appData.systemAccessForPageOpening().getMaximumPriorityPage(type, typeRequested);
		/*
		List<NavigationPublicPageData> pages = appData.systemAccessForPageOpening().getPages(type);
		if(pages.isEmpty()) return null;
		if(typeRequested == null) return pages.get(0);
		for(NavigationPublicPageData p: pages) {
			if(p.getPageType() == typeRequested) return p;
		}
		return null;*/
	}
	static int getEntryIdx(NavigationPublicPageData navi, Class<? extends Resource> type) {
		int idx = 0;
		for(EntryType et: navi.getEntryTypes()) {
			if(et.getType().isAssignableFrom(type)) {
				return idx;
			}
			idx++;
		}
		throw new IllegalStateException(type.getSimpleName()+" not found in "+navi.getUrl());
	}
	
	public static String buildId(SmartEffExtensionService service) {
		String id = service.id();
		if(id == null) return service.getClass().getName();
		else return id;
	}
	public static String buildId(ExtensionCapability service) {
		String id = service.id();
		if(id == null) return service.getClass().getName();
		else return id;
	}
	public static String buildValidWidgetId(ExtensionCapability service) {
		String id = WidgetHelper.getValidWidgetId(buildId(service));
		return id;
	}

	public static String getProviderURL(ExtensionCapability navi) {
		return WidgetHelper.getValidWidgetId(buildId(navi))+".html";	
	}

	public static boolean isMulti(Cardinality card) {
		if(card == Cardinality.MULTIPLE_OPTIONAL || card == Cardinality.MULTIPLE_REQUIRED) return true;
		return false;
	}

}
