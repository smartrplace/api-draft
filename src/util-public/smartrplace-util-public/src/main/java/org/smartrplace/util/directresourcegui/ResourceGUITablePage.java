package org.smartrplace.util.directresourcegui;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.object.widget.popup.ClosingPopup;
import de.iwes.widgets.object.widget.popup.ClosingPopup.ClosingMode;
import de.iwes.widgets.resource.widget.table.ResourceTable;

/**
 * An HTML page, generated from the Java code.
 */
public abstract class ResourceGUITablePage<T extends Resource> implements ResourceGUITableProvider<T> {
	protected final WidgetPage<?> page;
	protected ResourceGUITableTemplate<T> mainTableRowTemplate;
	protected ResourceTable<T> mainTable;
	//protected ResourceGUIHelper<T> vhGlobal;
	
	protected final boolean registerDependentWidgets;
	
	protected final ClosingPopup<T> popMore1;
	protected final KnownWidgetHolder<T> knownWidgets;
	protected final Alert alert;
	protected final Class<T> resourceType;
	
	protected final ApplicationManager appMan;
	protected final ApplicationManagerMinimal appManMin;
	protected long retardationOnGET = 0;
	/*protected long getFrameworkTime() {
		if(appMan != null) return appMan.getFrameworkTime();
		return appManMin.getFrameworkTime();
	}*/
	private String pid() {
		return WidgetHelper.getValidWidgetId(this.getClass().getName());
	}
	
	public abstract void addWidgetsAboveTable();
	/** Overwrite this method to provide a different set of resources
	 * 
	 * @param req
	 * @return
	 */
	public List<T> getResourcesInTable(OgemaHttpRequest req) {
		return appMan.getResourceAccess().getResources(resourceType);
	}
	
	public ResourceGUITablePage(final WidgetPage<?> page, final ApplicationManager appMan,
			Class<T> resourceType) {
		this(page, appMan, null, resourceType, true);
	}
	/**
	 * 
	 * @param page
	 * @param appMan
	 * @param appManMin
	 * @param resourceType
	 * @param autoBuildPage if false you have to call triggerPageBuild in an super class
	 * constructor to start addWidgetsAboveTable
	 */
	public ResourceGUITablePage(final WidgetPage<?> page, final ApplicationManager appMan,
			final ApplicationManagerMinimal appManMin, Class<T> resourceType,
			boolean autoBuildPage) {
		this(page, appMan, appManMin, resourceType, autoBuildPage, true);
	}
	public ResourceGUITablePage(final WidgetPage<?> page, final ApplicationManager appMan,
			final ApplicationManagerMinimal appManMin, Class<T> resourceType,
			boolean autoBuildPage, final boolean registerDependentWidgets) {
		this.page = page;
		this.appMan = appMan;
		this.appManMin = appManMin;
		this.resourceType = resourceType;
		this.registerDependentWidgets = registerDependentWidgets;

		//init all widgets
		
		knownWidgets = new KnownWidgetHolder<T>(page, "knownWidgets");
		page.append(knownWidgets);
		alert = new Alert(page, "alert", "");
		popMore1 = new ClosingPopup<T>(page, "popMore1",
				"More Information", true, ClosingMode.CLOSE) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				T item = getItem(req);
				if(item == null) return;
			}
		};
		if(autoBuildPage) triggerPageBuild();
	}

	public void triggerPageBuild() {
		page.append(popMore1);
		mainTableRowTemplate = new ResourceGUITableTemplate<T>(
				new ResourceGUITableTemplate.TableProvider<T>() {

					@Override
					public ResourceTable<T> getTable(OgemaHttpRequest req) {
						return mainTable;
					}
					
				}, resourceType, appMan, appManMin, registerDependentWidgets) {

			@Override
			protected Row addRow(final T object,
					final ResourceGUIHelper<T> vh, final String id, OgemaHttpRequest req) {
				final Row row = new Row();
				addWidgets(object, vh, id, req, row, appMan);
				return row;
			}
		};
		mainTable = new ResourceTable<T>(page, "appTable"+pid(), mainTableRowTemplate) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				List<T> data = getResourcesInTable(req);
				updateRows(data, req);
				if(retardationOnGET > 0) try {
					Thread.sleep(retardationOnGET);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		
		//build page
		addWidgetsAboveTable();
		page.append(alert);
		page.append(mainTable);
	}
	
	public WidgetPage<?> getPage() {
		return page;
	}
	
	@Override
	public void addWidgets(final T object,
			final ObjectResourceGUIHelper<T, T> vh, final String id, OgemaHttpRequest req, Row row,
			ApplicationManager appMan) {
		addWidgets(object, (ResourceGUIHelper<T>)vh, id, req, row, appMan);
	}
	
	@Override
	public T getResource(T object, OgemaHttpRequest req) {
		throw new IllegalStateException("getResource should not be used with ResourceGUITablePage");
	}
	
	@SuppressWarnings("deprecation")
	public void triggerOnPost(OgemaWidget governor, OgemaWidget target) {
		if(registerDependentWidgets) governor.registerDependentWidget(target);
		else governor.triggerAction(target, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}}

