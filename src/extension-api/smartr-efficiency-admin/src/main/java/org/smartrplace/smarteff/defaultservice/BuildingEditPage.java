package org.smartrplace.smarteff.defaultservice;

import java.util.List;

import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPage;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.smarteff.admin.util.TypeAdministration;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.directresourcegui.ResourceEditPage;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.html.form.label.Header;
import extensionmodel.smarteff.api.base.BuildingData;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class BuildingEditPage  {
	protected static final String pid = BuildingEditPage.class.getSimpleName();
	protected EditPage editPage;
	public final Provider provider;	
	
	public BuildingEditPage() {
		this.provider = new Provider();
	}

	public class EditPage {
		//private final ApplicationManagerSPExt appManExt;
		private ExtensionNavigationPage<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
		
		private	ObjectResourceGUIHelper<BuildingData, BuildingData> mh;
		private WidgetPage<?> page;

		public EditPage(ExtensionNavigationPage<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage, ApplicationManagerSPExt appManExt) {
			this.exPage = exPage;
			//this.appManExt = appManExt;
			//BuildingData bd = null; bd.heatedLivingSpace()
			this.page = exPage.page;
			Alert alert = new Alert(page, "alert"+pid, "");
			page.append(alert);
			
			Header header = new Header(page, "header"+pid) {
				private static final long serialVersionUID = 1L;

				@Override
				public void onGET(OgemaHttpRequest req) {
					setText(provider.label(req.getLocale())+" for user "+getReqData(req).getParent().getParent().getName(), req);
				}
			};
			header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
			page.append(header);

			
			mh = new ObjectResourceGUIHelper<BuildingData, BuildingData>(page, (TemplateInitSingleEmpty<BuildingData>)null , null, false) {

				@Override
				protected BuildingData getResource(BuildingData object, OgemaHttpRequest req) {
					return object;
				}
				@Override
				protected BuildingData getGatewayInfo(OgemaHttpRequest req) {
					return getReqData(req);
				}
				
			};
			StaticTable table = new StaticTable(3, 4, new int[]{1,5,5,1});
			int c = 0;
			table.setContent(c, 0, "Name").setContent(c,1, mh.stringEdit("name", alert));
			c++; //2
			table.setContent(c, 0, "Beheizte Fläche").
					setContent(c,1, mh.floatEdit("heatedLivingSpace", alert, 1, 999999, "Heated Living Space value outside range!"));
			c++;
			Button activateButton = new Button(page, "activateButton", "activate") {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					BuildingData res = getReqData(req);
					if(res.isActive()) {
						setWidgetVisibility(false, req);
					} else {
						setWidgetVisibility(true, req);
						if(checkResource(res)) enable(req);
						else disable(req);
					}
				}
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					ExtensionResourceAccessInitData appData = exPage.init.getSelectedItem(req);
					BuildingData res = getReqData(req);
					appData.systemAccess().activateResource(res);
				}
			};
			table.setContent(c, 0, activateButton);
			page.append(table);
			exPage.init.registerDependentWidget(header);
			ResourceEditPage.registerDependentWidgets(exPage.init, table);
		}
		
		private BuildingData getReqData(OgemaHttpRequest req) {
			ExtensionResourceAccessInitData appData = exPage.init.getSelectedItem(req);
			return (BuildingData) appData.entryResources().get(0);
		}

	}
	
	public class Provider implements NavigationGUIProvider {
		//private ExtensionResourceType generalData;	
	
		@Override
		public String label(OgemaLocale locale) {
			return "Standard Building Edit Page";
		}
	
		@Override
		public void initPage(ExtensionNavigationPage<?, ?> pageIn, ApplicationManagerSPExt appManExt) {
			//this.generalData = generalData;
			@SuppressWarnings("unchecked")
			ExtensionNavigationPage<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> page =
				(ExtensionNavigationPage<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData>) pageIn;
			//Label test = new Label(page.page, "test", "Hello World!");
			//page.page.append(test);
			editPage = new EditPage(page, appManExt);
		}
	
		@Override
		public List<EntryType> getEntryTypes() {
			return TypeAdministration.getStandardEntryTypeList(BuildingData.class);
		}
	}
	
	public boolean checkResource(BuildingData data) {
		String name = data.name().getValue();
		if(name.isEmpty()) return false;
		List<BuildingData> otherOfType = data.getParent().getSubResources(BuildingData.class, false);
		for(BuildingData ot: otherOfType) {
			if(ot.equalsLocation(data)) continue;
			if(ot.name().getValue().equals(name)) return false;
		}
		if(data.heatedLivingSpace().getValue() <= 0) return false;
		return true;
	}
}
