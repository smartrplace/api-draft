package org.smartrplace.smarteff.admin;

import java.util.Arrays;
import java.util.List;

import org.ogema.core.model.Resource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ExtensionUserData;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.smarteff.admin.config.SmartEffAdminData;
import org.smartrplace.smarteff.admin.gui.ExtensionNavigationPage;
import org.smartrplace.smarteff.admin.protect.ExtensionResourceAccessInitDataImpl;
import org.smartrplace.smarteff.admin.protect.NavigationPageSystemAccess;
import org.smartrplace.smarteff.admin.protect.NavigationPageSystemAccessForPageOpening;
import org.smartrplace.smarteff.admin.util.ConfigIdAdministration.ConfigInfo;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class UserAdmin {
	SmartEffUserDataNonEdit userDataNE = null;
	private final SpEffAdminController app;
	private SmartEffAdminData appConfigData;
	
	public UserAdmin(SpEffAdminController app) {
		this.app = app;
		if(app == null) return;
		userDataNE = app.appMan.getResourceAccess().getResource("master");
		if(!userDataNE.isActive()) {
			initTestData();
		}		
	}

	public SmartEffUserDataNonEdit getUserData() {
        return userDataNE;
	}
	public Resource getAllUserResource() {
        return userDataNE;
	}
	public List<SmartEffUserDataNonEdit> getAllUserData() {
		return Arrays.asList(new SmartEffUserDataNonEdit[] {userDataNE});
	}
	
	public ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> getNaviPage(final WidgetPage<?> page, String url, String overviewUrl,
			String providerId, NavigationGUIProvider navi) {
		return new ExtensionNavigationPage<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData>(page, url, "dataExplorer.html",
				providerId) {

			@Override
			protected List<SmartEffUserDataNonEdit> getUsers(OgemaHttpRequest req) {
				return getAllUserData();
			}
			@Override
			protected ExtensionResourceAccessInitData getItemById(String configId, OgemaHttpRequest req) {
				//SmartEffUserDataNonEdit userDataNonEdit = loggedIn.getSelectedItem(req);
				SmartEffUserDataNonEdit userDataNonEdit = userDataNE;
				NavigationPageSystemAccess systemAccess = new NavigationPageSystemAccess(userDataNonEdit.ogemaUserName().getValue(),
						(navi!=null?navi.label(req.getLocale()):providerId),
						app.guiPageAdmin.navigationPublicData, app.lockAdmin, app.configIdAdmin, app.typeAdmin, app.appManExt,
						app.guiPageAdmin.proposalInfo);
				if((navi == null) || (navi.getEntryTypes() == null) || (configId == null)) {
					ExtensionResourceAccessInitData result = new ExtensionResourceAccessInitDataImpl(-1, null,
							userDataNonEdit.editableData().getLocationResource(), userDataNonEdit, systemAccess);
					return result;
				} else {
					ConfigInfo c = app.configIdAdmin.getConfigInfo(configId);
					ExtensionResourceAccessInitData result = new ExtensionResourceAccessInitDataImpl(c.entryIdx,
							c.entryResources,
							userDataNonEdit.editableData().getLocationResource(), userDataNonEdit, systemAccess);
					return result;
				}
			}
		};

	}

	protected void initTestData() {
		SmartEffUserDataNonEdit data = userDataNE.create();
		ValueResourceHelper.setIfNew(data.ogemaUserName(), "master");
		data.editableData().create();
		data.activate(true);
	}
	
    protected void initConfigurationResource() {
		String configResourceDefaultName = SpEffAdminController.APPCONFIGDATA_LOCATION;
		appConfigData = app.appMan.getResourceAccess().getResource(configResourceDefaultName);
		if (appConfigData != null) { // resource already exists (appears in case of non-clean start)
			app.appMan.getLogger().debug("{} started with previously-existing config resource", getClass().getName());
		}
		else {
			appConfigData = (SmartEffAdminData) app.appMan.getResourceManagement().createResource(configResourceDefaultName,SmartEffAdminData.class);
			appConfigData.activate(true);
			app.appMan.getLogger().debug("{} started with new config resource", getClass().getName());
		}
    }

	public SmartEffAdminData getAppConfigData() {
		return appConfigData;
	}
	
	public ExtensionResourceAccessInitData getAccessData(String configId, OgemaHttpRequest req,
			NavigationGUIProvider navi) {
		return getAccessData(configId, req, navi, getUserData(), app);
	}	
	protected ExtensionResourceAccessInitData getAccessData(String configId, OgemaHttpRequest req,
			NavigationGUIProvider navi, SmartEffUserDataNonEdit userDataNonEdit,
			SpEffAdminController app) {
		if(navi == null || navi.getEntryTypes() == null || configId == null) {
			ExtensionUserData editableData = null;
			NavigationPageSystemAccessForPageOpening systemAccess;
			if(userDataNonEdit != null) {
				editableData = userDataNonEdit.editableData().getLocationResource();
				systemAccess = new NavigationPageSystemAccess(userDataNonEdit.ogemaUserName().getValue(),
						navi.label(req.getLocale()),
						app.guiPageAdmin.navigationPublicData, app.lockAdmin, app.configIdAdmin, app.typeAdmin, app.appManExt,
						app.guiPageAdmin.proposalInfo);
			} else {
				systemAccess = new NavigationPageSystemAccessForPageOpening(
					app.guiPageAdmin.navigationPublicData, app.configIdAdmin,
					app.guiPageAdmin.proposalInfo);
			}
			ExtensionResourceAccessInitData result = new ExtensionResourceAccessInitDataImpl(-1, null,
					editableData , userDataNonEdit, systemAccess);
			return result;
		} else {
			NavigationPageSystemAccess systemAccess = new NavigationPageSystemAccess(userDataNonEdit.ogemaUserName().getValue(),
					navi.label(req.getLocale()),
					app.guiPageAdmin.navigationPublicData, app.lockAdmin, app.configIdAdmin, app.typeAdmin, app.appManExt,
					app.guiPageAdmin.proposalInfo);
			ConfigInfo c = app.configIdAdmin.getConfigInfo(configId);
			ExtensionResourceAccessInitData result = new ExtensionResourceAccessInitDataImpl(c.entryIdx,
					c.entryResources,
					userDataNonEdit.editableData().getLocationResource(), userDataNonEdit, systemAccess);
			return result;
		}
	}

}
