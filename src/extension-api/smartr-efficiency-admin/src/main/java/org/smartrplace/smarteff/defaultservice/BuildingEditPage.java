package org.smartrplace.smarteff.defaultservice;

import java.util.List;

import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPage;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
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
		//private final ApplicationManagerMinimal appManMin;
		//private ExtensionNavigationPage<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
		
		private	ObjectResourceGUIHelper<BuildingData, BuildingData> mh;
		private WidgetPage<?> page;

		public EditPage(ExtensionNavigationPage<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage, ApplicationManagerMinimal appManMin) {
			//this.exPage = exPage;
			//this.appManMin = appManMin;
			//BuildingData bd = null; bd.heatedLivingSpace()
			this.page = exPage.page;
			Alert alert = new Alert(page, "alert"+pid, "");
			page.append(alert);
			mh = new ObjectResourceGUIHelper<BuildingData, BuildingData>(page, (TemplateInitSingleEmpty<BuildingData>)null , null, false) {

				@Override
				protected BuildingData getResource(BuildingData object, OgemaHttpRequest req) {
					return object;
				}
				@Override
				protected BuildingData getGatewayInfo(OgemaHttpRequest req) {
					if(fixedGatewayInfo != null) return fixedGatewayInfo;
					ExtensionResourceAccessInitData appData = exPage.init.getSelectedItem(req);
					return (BuildingData) appData.entryResources().get(0);
				}
				
			};
			StaticTable table = new StaticTable(4, 4, new int[]{1,5,5,1});
			int c = 0;
			table.setContent(c, 0, "Name").setContent(c,1, mh.stringEdit("name", alert));
			c++; //2
			table.setContent(c, 0, "Beheizte Fläche").
					setContent(c,1, mh.floatEdit("heatedLivingSpace", alert, 1, 999999, "Heated Living Space value outside range!"));
		}

	}
	
	public class Provider implements NavigationGUIProvider {
		//private ExtensionResourceType generalData;	
	
		@Override
		public String label(OgemaLocale locale) {
			return "Standard Building Overview Table";
		}
	
		@Override
		public void initPage(ExtensionNavigationPage<?, ?> pageIn, ExtensionResourceType generalData,
				ApplicationManagerMinimal appManMin) {
			//this.generalData = generalData;
			@SuppressWarnings("unchecked")
			ExtensionNavigationPage<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> page =
				(ExtensionNavigationPage<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData>) pageIn;
			//Label test = new Label(page.page, "test", "Hello World!");
			//page.page.append(test);
			editPage = new EditPage(page, appManMin);
		}
	
		@Override
		public List<EntryType> getEntryType() {
			return CapabilityHelper.getStandardEntryTypeList(BuildingData.class);
		}
	}
}
