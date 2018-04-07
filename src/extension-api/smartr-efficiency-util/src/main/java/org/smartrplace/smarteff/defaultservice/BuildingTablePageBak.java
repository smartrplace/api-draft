package org.smartrplace.smarteff.defaultservice;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.smarteff.util.AddEntryButton;
import org.smartrplace.smarteff.util.SPPageUtil;
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

public class BuildingTablePageBak  {
	protected static final String pid = BuildingTablePageBak.class.getSimpleName();
	protected TablePage tablePage;
	public final Provider provider;	
	
	public BuildingTablePageBak() {
		this.provider = new Provider();
	}

	public class TablePage extends ResourceGUITablePage<BuildingData> {
		//private final ApplicationManagerMinimal appManMin;
		private ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
		
		public TablePage(ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage, ApplicationManagerMinimal appManMin) {
			super(exPage.getPage(), null, appManMin, BuildingData.class, false);
			this.exPage = exPage;
			//this.appManMin = appManMin;
			triggerPageBuild();
		}

		@Override
		public void addWidgets(BuildingData object, ResourceGUIHelper<BuildingData> vh, String id,
				OgemaHttpRequest req, Row row, ApplicationManager appMan) {
			ExtensionResourceAccessInitData appData = null;
			if(req != null) appData = exPage.getAccessData(req);

			vh.stringLabel("Name", id, ResourceUtils.getHumanReadableName(object), row);
			vh.floatLabel("Heated Area", id, object.heatedLivingSpace(), row, "%.0f m2");
			SPPageUtil.addResOpenButton("Edit", object, BuildingData.class, vh, id, row, appData);
			if(object.isActive())
				vh.linkingButton("Evaluate All", id, object, row, "Evaluate All", "evalAll.html");
			else
				vh.stringLabel("Evaluate All", id, "Inactive", row);
			vh.linkingButton("Delete", id, object, row, "Delete", "delete.html");
		}

		@Override
		public void addWidgetsAboveTable() {
			Header header = new Header(page, "header"+pid, provider.label(null));
			header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
			page.append(header);
			
			RedirectButton addEntry = new AddEntryButton(page, "addEntry", pid, "Add Building", BuildingData.class, exPage);
			page.append(addEntry);
		}
		
		@Override
		public List<BuildingData> getResourcesInTable(OgemaHttpRequest req) {
			ExtensionResourceAccessInitData appData = exPage.getAccessData(req);
			return ((SmartEffUserData)appData.userData()).buildingData().getAllElements();
		}
	}
	
	public class Provider implements NavigationGUIProvider {
		//private Resource generalData;	
	
		@Override
		public String label(OgemaLocale locale) {
			return "Standard Building Overview Table";
		}
	
		@Override
		public void initPage(ExtensionNavigationPageI<?, ?> pageIn, ApplicationManagerSPExt appManExt) {
			//this.generalData = generalData;
			@SuppressWarnings("unchecked")
			ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> page =
				(ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData>) pageIn;
			tablePage = new TablePage(page, appManExt);
		}
	
		@Override
		public List<EntryType> getEntryTypes() {
			return null;
		}
	}
}
