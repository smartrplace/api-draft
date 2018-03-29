package org.smartrplace.smarteff.admin.gui;

import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.smarteff.admin.util.SmartrEffUtil;
import org.smartrplace.smarteff.admin.util.SmartrEffUtil.AccessType;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.object.widget.init.LoginInitSingleEmpty;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;
import de.iwes.widgets.template.DefaultDisplayTemplate;
import extensionmodel.smarteff.api.base.SmartEffUserDataNonEdit;

/**
 * An HTML page, generated from the Java code.
 */
public class DataExplorerPage extends ObjectGUITablePage<SmartEffExtensionResourceType, Resource> {
	protected static final String pid = DataExplorerPage.class.getSimpleName();

	public static final float MIN_COMFORT_TEMP = 4;
	public static final float MAX_COMFORT_TEMP = 30;
	public static final float DEFAULT_COMFORT_TEMP = 21;
	
	private final SpEffAdminController app;
	
	ValueResourceTextField<TimeResource> updateInterval;
	private LoginInitSingleEmpty<SmartEffUserDataNonEdit> loggedIn;
	private TemplateDropdown<SmartrEffExtResourceTypeData> selectProvider;

	public DataExplorerPage(final WidgetPage<?> page, final SpEffAdminController app,
			SmartEffExtensionResourceType initData) {
		super(page, app.appMan, initData);
		this.app = app;
	}
	
	private <T extends ExtensionResourceType> List<T> getNonEditResourcesToAccess(Class<T> type, SmartEffUserDataNonEdit userData) {
		List<T> result = userData.getSubResources(type, true);
		return result ;
	}
	private <T extends ExtensionResourceType> List<T> getEditableResourcesToAccess(Class<T> resType, SmartEffUserDataNonEdit userData) {
		List<T> result = userData.editableData().getSubResources(resType, true);
		return result ;
	}
	private <T extends ExtensionResourceType> List<T> getPublicResources(Class<T> type) {
		List<T> result = app.appConfigData.generalData().getSubResources(type, true);
		return result ;
	}
	private <T extends ExtensionResourceType> List<T> getAllResourcesToAccess(Class<T> resType, SmartEffUserDataNonEdit userData) {
		List<T> result = getEditableResourcesToAccess(resType, userData);
		result.addAll(getNonEditResourcesToAccess(resType, userData));
		result.addAll(getPublicResources(resType));
		return result ;
	}
	
	@Override
	public void addWidgetsAboveTable() {
		loggedIn = new LoginInitSingleEmpty<SmartEffUserDataNonEdit>(page, "loggedIn", true) {
			private static final long serialVersionUID = 6446396416992821986L;

			@Override
			protected List<SmartEffUserDataNonEdit> getUsers(OgemaHttpRequest req) {
				return app.appConfigData.userDataNonEdit().getAllElements();
			}
		};
		page.append(loggedIn);
		TemplateInitSingleEmpty<SmartrEffExtResourceTypeData> init = new TemplateInitSingleEmpty<SmartrEffExtResourceTypeData>(page, "init", false) {
			private static final long serialVersionUID = 1L;

			@Override
			protected SmartrEffExtResourceTypeData getItemById(String configId) {
				for(SmartrEffExtResourceTypeData eval: app.typeAdmin.resourceTypes.values()) {
					if(ResourceUtils.getValidResourceName(eval.resType.getName()).equals(configId)) return eval;
				}
				return null;
			}
			@Override
			public void init(OgemaHttpRequest req) {
				super.init(req);
				loggedIn.triggeredInit(req);
				Collection<SmartrEffExtResourceTypeData> items = app.typeAdmin.resourceTypes.values();
				selectProvider.update(items , req);
				SmartrEffExtResourceTypeData eval = getSelectedItem(req);
				selectProvider.selectItem(eval, req);
				System.out.println("Data Explorer: Finished init");
			}
		};
		page.append(init);
		
		Header header = new Header(page, "header", "Data Explorer");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
		
		selectProvider = new  TemplateDropdown<SmartrEffExtResourceTypeData>(page, "selectProvider");
		selectProvider.setTemplate(new DefaultDisplayTemplate<SmartrEffExtResourceTypeData>() {
			@Override
			public String getLabel(SmartrEffExtResourceTypeData object, OgemaLocale locale) {
				return object.typeDeclaration.resourceName();
			}
			
		});
		page.append(selectProvider);
		//Note: Synchronization issues with triggerAction
		//init.triggerOnPOST(selectProvider);
		//selectProvider.triggerOnPOST(mainTable);
		init.registerDependentWidget(selectProvider);
		selectProvider.registerDependentWidget(mainTable);
	}
	
	@Override
	public Collection<SmartEffExtensionResourceType> getObjectsInTable(OgemaHttpRequest req) {
		SmartrEffExtResourceTypeData item = selectProvider.getSelectedItem(req);
		if(item == null) throw new IllegalStateException("Widget dependencies not processed correctly!");
		System.out.println("Item:"+item.resType.getName());
		System.out.println("loggedIn:"+loggedIn);
		List<? extends ExtensionResourceType> list1 = getAllResourcesToAccess(item.resType, loggedIn.getSelectedItem(req));
		@SuppressWarnings("unchecked")
		List<SmartEffExtensionResourceType> result = (List<SmartEffExtensionResourceType>)list1 ; 
		return result;
	}

	@Override
	public void addWidgets(SmartEffExtensionResourceType object, ObjectResourceGUIHelper<SmartEffExtensionResourceType, Resource> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		//if(configRes != null) try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
		id = pid + id;
		vh.stringLabel("Name", id, ResourceUtils.getHumanReadableName(object), row);
		vh.stringLabel("Elements", id, ""+object.getSubResources(false).size(), row);
		vh.linkingButton("Export", id, object, row, "Export", "export.html");
		vh.linkingButton("View", id, object, row, "Export", "view.html");
		vh.linkingButton("Evaluate", id, object, row, "Export", "evaluate.html");
		if(!(SmartrEffUtil.getAccessType(object) == AccessType.READWRITE)) {
			vh.stringLabel("Edit", id, "--", row);
			vh.stringLabel("Delete", id, "--", row);
		} else {
			vh.linkingButton("Edit", id, object, row, "", "edit.html");
			Button deleteButton = new Button(vh.getParent(), "Delete"+pid, "Delete", req) {
				private static final long serialVersionUID = -6168031482180238199L;
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					app.removeResource(object);
				}
			};
			row.addCell("Delete", deleteButton);
		}
	}
	
	@Override
	public Resource getResource(SmartEffExtensionResourceType object, OgemaHttpRequest req) {
		return null;
	}
	
	@Override
	public String getLineId(SmartEffExtensionResourceType object) {
		String name = WidgetHelper.getValidWidgetId(ResourceUtils.getHumanReadableName(object));
		return name + super.getLineId(object);
	}
}
