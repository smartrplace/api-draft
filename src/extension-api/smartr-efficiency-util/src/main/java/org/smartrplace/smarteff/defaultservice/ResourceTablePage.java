package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.StringResource;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extenservice.resourcecreate.ProviderPublicDataForCreate.PagePriority;
import org.smartrplace.extensionservice.ExtensionCapabilityPublicData.EntryType;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.gui.ExtensionNavigationPageI;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.PageType;
import org.smartrplace.extensionservice.gui.NavigationPublicPageData;
import org.smartrplace.smarteff.util.AddEditButton;
import org.smartrplace.smarteff.util.AddEntryButton;
import org.smartrplace.smarteff.util.CapabilityHelper;
import org.smartrplace.smarteff.util.NaviPageBase;
import org.smartrplace.smarteff.util.SPPageUtil;
import org.smartrplace.smarteff.util.TableOpenButton;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;
import org.smartrplace.util.directresourcegui.ResourceGUITablePage;

import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.RedirectButton;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

public class ResourceTablePage extends NaviPageBase<Resource> {
	protected TablePage tablePage;
	
	public ResourceTablePage() {
		super();
	}

	public class TablePage extends ResourceGUITablePage<Resource> {
		//private final ApplicationManagerMinimal appManMin;
		private ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage;
		
		public TablePage(ExtensionNavigationPageI<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> exPage, ApplicationManagerMinimal appManMin) {
			super(exPage.getPage(), null, appManMin, Resource.class, false);
			this.exPage = exPage;
			//this.appManMin = appManMin;
			triggerPageBuild();
		}

		@Override
		public void addWidgets(Resource object, ResourceGUIHelper<Resource> vh, String id,
				OgemaHttpRequest req, Row row, ApplicationManager appMan) {
			ExtensionResourceAccessInitData appData = null;
			if(req != null) appData = exPage.getAccessData(req);
			vh.stringLabel("Type", id, object.getResourceType().getSimpleName(), row);
			if(object.exists()) {
				vh.stringLabel("Name", id, getSimpleName(object), row);
				SPPageUtil.addResEditOpenButton("Edit", object, object.getResourceType(), vh, id, row, appData);
				SPPageUtil.addResTableOpenButton("Open", object, object.getResourceType(), vh, id, row, appData);
				if(object.isActive())
					vh.linkingButton("Evaluate All", id, object, row, "Evaluate All", "evalAll.html");
				else
					vh.stringLabel("Evaluate All", id, "Inactive", row);
				vh.linkingButton("Delete", id, object, row, "Delete", "delete.html");
				ExtensionResourceTypeDeclaration<? extends Resource> typeDecl = appManExt.getTypeDeclaration(object.getResourceType());
				if(SPPageUtil.isMulti(typeDecl.cardinality())) {
					AddEntryButton addButton = new AddEntryButton(vh.getParent(), id, pid(), "Add Sub Resource", object.getResourceType(), exPage, req);
					row.addCell("AddResource", addButton);					
				} else {
					vh.stringLabel("AddResource", id, "SingleResource", row);
				}
			} else {
				vh.registerHeaderEntry("Name");
				vh.registerHeaderEntry("Edit");
				vh.registerHeaderEntry("Open");
				vh.registerHeaderEntry("Evaluate All");
				vh.registerHeaderEntry("Delete");
				if(req != null) {
					AddEntryButton addButton = new AddEntryButton(vh.getParent(), id, pid(), "Add Sub Resource", object.getResourceType(), exPage, req);
					row.addCell("AddResource", addButton);
				} else vh.registerHeaderEntry("AddResource");
			}
		}

		@Override
		public void addWidgetsAboveTable() {
			RedirectButton editResource = new AddEditButton(page, "editEntry", pid(), "Edit", Resource.class, exPage) {
				private static final long serialVersionUID = 1L;
				@Override
				protected Class<? extends Resource> type(ExtensionResourceAccessInitData appData,
						OgemaHttpRequest req) {
					return getResource(appData, req).getResourceType();
				}
			};
			StaticTable topTable = new StaticTable(1, 3);
			TableOpenButton allResourceButton2 = new TableOpenButton(page, "allResourceButton", pid(), "All Resources", resourceType, exPage) {
				private static final long serialVersionUID = 1L;
				@Override
				protected NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
						Class<? extends Resource> type, PageType typeRequested, OgemaHttpRequest req) {
					return appData.systemAccessForPageOpening().getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.RESOURCEALL_NAVI_PROVIDER));//super.getPageData(appData, type, typeRequested);
				}
			};
			TableOpenButton proposalTableOpenButton = new TableOpenButton(page, "proposalTableOpenButton", pid(), "Proposal providers", resourceType, exPage) {
				private static final long serialVersionUID = 1L;
				@Override
				protected NavigationPublicPageData getPageData(ExtensionResourceAccessInitData appData,
						Class<? extends Resource> type, PageType typeRequested, OgemaHttpRequest req) {
					return appData.systemAccessForPageOpening().getPageByProvider(SPPageUtil.getProviderURL(BaseDataService.PROPOSALTABLE_PROVIDER));//super.getPageData(appData, type, typeRequested);
				}
			};
			topTable.setContent(0, 0, editResource).setContent(0, 1, allResourceButton2).setContent(0, 2, proposalTableOpenButton);
			page.append(topTable);
		}
		
		@Override
		public List<Resource> getResourcesInTable(OgemaHttpRequest req) {
			return provideResourcesInTable(req);
		}
	}

	protected List<Resource> provideResourcesInTable(OgemaHttpRequest req) {
		Class<? extends Resource> resourceType = getReqData(req).getResourceType();
		List<Class<? extends Resource>> types = appManExt.getSubTypes(resourceType);
		List<Resource> result = new ArrayList<>();
		Resource parent = getReqData(req);
		for(Class<? extends Resource> t: types) {
			List<? extends Resource> resOfType = parent.getSubResources(t, false);
			if(resOfType.isEmpty()) {
				result.add(parent.getSubResource("Virtual"+t.getSimpleName(), t));
			} else result.addAll(resOfType);
		}
		return result;
	}
	
	@Override
	protected Class<Resource> typeClass() {
		return Resource.class;
	}
	
	@Override //optional
	public String pid() {
		return ResourceTablePage.class.getSimpleName();
	}

	@Override
	protected String label(OgemaLocale locale) {
		return "Generic Resource Overview Table";
	}

	@Override
	protected void addWidgets() {
		tablePage = new TablePage(exPage, appManExt);		
	}

	@Override
	protected String getHeader(OgemaHttpRequest req) {
		return "Resources by Type in "+super.getHeader(req);
	}
	
	@Override
	protected List<EntryType> getEntryTypes() {
		return CapabilityHelper.getStandardEntryTypeList(typeClass());
	}

	@Override
	protected PageType getPageType() {
		return PageType.TABLE_PAGE;
	}
	
	@Override
	protected PagePriority getPriority() {
		return PagePriority.SECONDARY;
	}
	
	public static String getSimpleName(Resource resource) {
		Resource name = resource.getSubResource("name");
		if ((name != null) && (name instanceof StringResource)) {
			String val = ((StringResource) (name)).getValue();
			if (name.isActive() && (!val.trim().isEmpty()))
				return val;
		}
		return resource.getName();
	}

}
