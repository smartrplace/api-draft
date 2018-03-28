package org.smartrplace.smarteff.defaultservice;

import java.util.Arrays;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.model.locations.Building;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPage;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;
import org.smartrplace.util.directresourcegui.ResourceGUITablePage;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.label.Header;
import extensionmodel.smarteff.api.base.BuildingData;
import extensionmodel.smarteff.api.base.SmartEffUserData;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class BuildingTablePage  {
	protected static final String pid = BuildingTablePage.class.getSimpleName();
	protected TablePage tablePage;
	public final Provider provider;	
	
	public BuildingTablePage() {
		this.provider = new Provider();
	}

	public class TablePage extends ResourceGUITablePage<BuildingData> {
		//private final ApplicationManagerMinimal appManMin;
		private ExtensionNavigationPage<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
		
		public TablePage(ExtensionNavigationPage<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage, ApplicationManagerMinimal appManMin) {
			super(exPage.page, null, BuildingData.class);
			this.exPage = exPage;
			//this.appManMin = appManMin;
		}

		@Override
		public void addWidgets(BuildingData object, ResourceGUIHelper<BuildingData> vh, String id,
				OgemaHttpRequest req, Row row, ApplicationManager appMan) {
			ExtensionResourceAccessInitData appData = null;
			if(exPage != null) appData = exPage.init.getSelectedItem(req);

			vh.stringLabel("Name", id, ResourceUtils.getHumanReadableName(object), row);
			vh.floatLabel("Heated Area", id, object.heatedLivingSpace(), row, "%.0f m2");
			if(appData != null) {
				NavigationPublicPageData pageData = getBuildingEditPage(appData);
				if(pageData != null) {
					String configId = appData.systemAccess().accessPage(pageData, getBuildingEntryIdx(pageData),
							Arrays.asList(new ExtensionResourceType[]{object}));
					vh.linkingButton("Edit", id, object, row, "Edit", pageData.getUrl()+"?configId="+configId);
				} else {
					vh.stringLabel("Edit", id, "No Editor", row);
				}
				
			} else {
				vh.registerHeaderEntry("Edit");
			}
			vh.linkingButton("Evaluate All", id, object, row, "Evaluate All", "evalAll.html");
			vh.linkingButton("Delete", id, object, row, "Delete", "delete.html");
		}

		@Override
		public void addWidgetsAboveTable() {
			Header header = new Header(page, "header"+pid, provider.label(null));
			header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
			page.append(header);
			
			RedirectButton addEntry = new RedirectButton(page, "addEntry"+pid, "Add Building") {
				private static final long serialVersionUID = -1629093147343510820L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					if(getBuildingEditPage(exPage.init.getSelectedItem(req)) == null)
						disable(req);
					else enable(req);
				}
				@Override
				public void onPrePOST(String data, OgemaHttpRequest req) {
					ExtensionResourceAccessInitData appData = exPage.init.getSelectedItem(req);
					NavigationPublicPageData pageData = getBuildingEditPage(appData);
					String configId = appData.systemAccess().accessPage(pageData, getBuildingEntryIdx(pageData), null);
					setUrl(pageData.getUrl()+"?configId="+configId, req);
				}
			};
			page.append(addEntry);
		}
		
		private NavigationPublicPageData getBuildingEditPage(ExtensionResourceAccessInitData appData) {
			List<NavigationPublicPageData> pages = appData.systemAccess().getPages(BuildingData.class);
			if(pages.isEmpty()) return null;
			else return pages.get(0);
		}
		private int getBuildingEntryIdx(NavigationPublicPageData navi) {
			int idx = 0;
			for(EntryType et: navi.getEntryType()) {
				if(Building.class.isAssignableFrom(et.getType())) {
					return idx;
				}
				idx++;
			}
			throw new IllegalStateException("Building entry type not found in Building Edit Page!");
		}
		
		@Override
		public List<BuildingData> getResourcesInTable(OgemaHttpRequest req) {
			ExtensionResourceAccessInitData appData = exPage.init.getSelectedItem(req);
			return ((SmartEffUserData)appData.userData()).buildings().getAllElements();
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
			tablePage = new TablePage(page, appManMin);
		}
	
		@Override
		public List<EntryType> getEntryType() {
			return null;
		}
	}
}
