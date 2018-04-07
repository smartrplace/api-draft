package org.smartrplace.smarteff.util;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class AddEntryButton extends RedirectButton {
	private static final long serialVersionUID = -4145439981103486352L;
	private final Class<? extends Resource> type;
	private final ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
	
	public AddEntryButton(WidgetPage<?> page, String id, String pid, String text,
			Class<? extends Resource> type,
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage) {
		super(page, id, text);
		this.type = type;
		this.exPage = exPage;
	}
	@Override
	public void onGET(OgemaHttpRequest req) {
		if(SPPageUtil.getPageData(exPage.getAccessData(req), type) == null)
			disable(req);
		else enable(req);
	}
	@Override
	public void onPrePOST(String data, OgemaHttpRequest req) {
		ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
		NavigationPublicPageData pageData = SPPageUtil.getPageData(appData, type);
		String configId = appData.systemAccess().accessCreatePage(pageData, SPPageUtil.getEntryIdx(pageData, type),
				appData.userData());
		if(configId.startsWith(CapabilityHelper.ERROR_START)) setUrl("error/"+configId, req);
		else setUrl(pageData.getUrl()+"?configId="+configId, req);
	}

}
