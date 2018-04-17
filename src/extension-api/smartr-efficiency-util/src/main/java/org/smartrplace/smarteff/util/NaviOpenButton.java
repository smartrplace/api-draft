package org.smartrplace.smarteff.util;

import java.util.Arrays;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForCreate;
import org.smartrplace.extenservice.resourcecreate.ExtensionPageSystemAccessForPageOpening;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class NaviOpenButton extends RedirectButton {
	protected static final long serialVersionUID = -4145439981103486352L;
	protected final Class<? extends Resource> defaultType;
	protected final ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
	protected final PageType pageType;
	//doCreate only relevant for pageType EDIT
	protected final boolean doCreate;
	//Override especially for pages that declare opening resource types == 0
	/** Adapt this if {@link EditPageBase#getReqData(OgemaHttpRequest) is changed}*/
	protected Resource getResource(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
		return appData.entryResources().get(0);
	}
	protected Class<? extends Resource> type(ExtensionResourceAccessInitData appData, OgemaHttpRequest req) {
		return defaultType;
	}
	protected NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
			Class<? extends Resource> type, PageType typeRequested, OgemaHttpRequest req) {
		return SPPageUtil.getPageData(appData, type, pageType);
	}
	public NaviOpenButton(WidgetPage<?> page, String id, String pid, String text,
			Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			PageType pageType, boolean doCreate) {
		super(page, id, text);
		this.defaultType = type;
		this.exPage = exPage;
		this.pageType = pageType;
		this.doCreate = doCreate;
	}
	public NaviOpenButton(OgemaWidget parent, String id, String pid, String text,
			Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage,
			PageType pageType, boolean doCreate, OgemaHttpRequest req) {
		super(parent, id, text, "", req);
		this.defaultType = type;
		this.exPage = exPage;
		this.pageType = pageType;
		this.doCreate = doCreate;
	}
	@Override
	public void onGET(OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		if(getPageData(appData, type(appData, req), pageType, req) == null)
			disable(req);
		else enable(req);
	}
	@Override
	public void onPrePOST(String data, OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		NavigationPublicPageData pageData = getPageData(appData, type(appData, req), pageType, req);
		final Resource object = getResource(appData, req);
		final String configId = getConfigId(pageType, object, type(appData, req), appData.systemAccessForPageOpening(), pageData, doCreate);
		if(configId.startsWith(CapabilityHelper.ERROR_START)) setUrl("error/"+configId, req);
		else setUrl(pageData.getUrl()+"?configId="+configId, req);
	}

	static String getConfigId(PageType pageType, Resource object, Class<? extends Resource> type,
			ExtensionPageSystemAccessForPageOpening systemAccess,
			NavigationPublicPageData pageData, boolean doCreate) {
		if((pageType == PageType.EDIT_PAGE) && doCreate) {
			return ((ExtensionPageSystemAccessForCreate)systemAccess).accessCreatePage(pageData, SPPageUtil.getEntryIdx(pageData, type),
				object);
		} else {
			return systemAccess.accessPage(pageData, SPPageUtil.getEntryIdx(pageData, type),
					Arrays.asList(new Resource[]{object}));			
		}
		
	}
}
