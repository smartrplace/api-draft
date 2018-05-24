package org.smartrplace.util.directobjectgui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.util.directresourcegui.KnownWidgetHolder;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.object.widget.popup.ClosingPopup;
import de.iwes.widgets.object.widget.popup.ClosingPopup.ClosingMode;

/**
 * An HTML page, generated from the Java code.
 */
public abstract class ObjectGUITablePage<T, R extends Resource> implements ObjectGUITableProvider<T, R> {
	public abstract R getResource(T object, OgemaHttpRequest req);
	public abstract void addWidgetsAboveTable();
	protected void addWidgetsBelowTable() {};
	/** For the header we need an object that will be used to generate a header line in the
	 * table. If the method return null (default) the header is generated automatically 
	 * and statically based on the widgetIds or headers set with the ObjectResourceGUIHelper. The object
	 * may be quasi empty, but must return a different toString method result than any other object
	 * used to generate a row in the table. The method is called during the constructor.
	 * @return
	 */
	protected T getHeaderObject() {return null;};
	/** Only relevant if {@link #getHeaderObject()} returns not null. If getHeaderObject is non-null
	 * and getHeaderText returns null then getHeaderWidget is evaluated to get a widget for the respective
	 * header position.*/
	protected String getHeaderText(String columnId, final ObjectResourceGUIHelper<T, R> vh,
			OgemaHttpRequest req) {return "Header n/a";};
	protected OgemaWidget getHeaderWidget(String columnId, final ObjectResourceGUIHelper<T, R> vh,
			OgemaHttpRequest req) {return null;};
	
	/** Overwrite this method to provide set of objects
	 * @param req
	 * @return
	 */
	public abstract Collection<T> getObjectsInTable(OgemaHttpRequest req);
	
	/**Overwrite this if you want to adapt the lineIds*/
	public String getLineId(T object) {
		if (object instanceof Resource) {
			Resource r = (Resource) object;
			return ResourceUtils.getValidResourceName(r.getLocation());
		} else if (object instanceof ResourcePattern<?>) {
			return ResourceUtils.getValidResourceName(((ResourcePattern<?>) object).model.getLocation());
		} else {
			return ResourceUtils.getValidResourceName(object.toString().replace('$', '_'));
		}
	}
	private String pid() {
		return WidgetHelper.getValidWidgetId(this.getClass().getName());
	}

	protected final WidgetPage<?> page;
	protected ObjectGUITableTemplate<T, R> mainTableRowTemplate;
	protected DynamicTable<T> mainTable;
	//protected ObjectResourceGUIHelper<T, R> vhGlobal;
	protected final boolean registerDependentWidgets;
	
	protected final ClosingPopup<T> popMore1;
	protected final KnownWidgetHolder<T> knownWidgets;
	protected final Alert alert;
	protected final ApplicationManager appMan;
	protected final ApplicationManagerMinimal appManMin;
	protected final T initSampleObject;
	protected final T headerObject;
	protected long retardationOnGET = 0;
	
	public ObjectGUITablePage(final WidgetPage<?> page, final ApplicationManager appMan,
			T initSampleObject) {
		this(page, appMan, initSampleObject, true);
	}
	public ObjectGUITablePage(final WidgetPage<?> page, final ApplicationManager appMan,
			T initSampleObject, boolean autoBuildPage) {
		this(page, appMan, initSampleObject, autoBuildPage, true);
	}
	public ObjectGUITablePage(final WidgetPage<?> page, final ApplicationManager appMan,
			T initSampleObject, boolean autoBuildPage, boolean registerDependentWidgets) {
		this(page, appMan, null, initSampleObject, autoBuildPage, true);
	}
	public ObjectGUITablePage(final WidgetPage<?> page,
			final ApplicationManager appMan, final ApplicationManagerMinimal appManMin,
			T initSampleObject, boolean autoBuildPage, boolean registerDependentWidgets) {
		this.page = page;
		this.appMan = appMan;
		this.appManMin = appManMin;
		this.initSampleObject = initSampleObject;
		this.registerDependentWidgets = registerDependentWidgets;
		headerObject = getHeaderObject();
		
		//init all widgets
		alert = new Alert(page, "alert"+pid(), "");
		
		knownWidgets = new KnownWidgetHolder<T>(page, "knownWidgets"+pid());
		page.append(knownWidgets);
		popMore1 = new ClosingPopup<T>(page, "popMore1"+pid(),
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

		mainTableRowTemplate = new ObjectGUITableTemplate<T, R>(
				new ObjectGUITableTemplate.ObjectTableProvider<T>() {

					@Override
					public DynamicTable<T> getTable(OgemaHttpRequest req) {
						return mainTable;
					}
					
				}, initSampleObject, appMan, appManMin, (headerObject != null), registerDependentWidgets) {

			@Override
			protected Row addRow(final T object,
					final  ObjectResourceGUIHelper<T, R> vh, final String id, OgemaHttpRequest req) {
				final Row row = new Row();
				if((headerObject != null) && (req != null) && object.toString().equals(headerObject.toString())) {
					LinkedHashMap<String,Object> map2 = mhInit.getHeader();
					for(String columnId: map2.keySet()) {
						String headerText = getHeaderText(columnId, vh, req);
						if(headerText != null) row.addCell(columnId, headerText);
						else {
							OgemaWidget w = getHeaderWidget(columnId, vh, req);
							row.addCell(columnId, w);
						}
					}
					return row;
				}
				addWidgets(object, vh, id, req, row, appMan);
				return row;
			}

			@Override
			protected R getResource(T object, OgemaHttpRequest req) {
				return ObjectGUITablePage.this.getResource(object, req);
			}

			@Override
			public String getLineId(T object) {
				if((headerObject != null) && object.toString().equals(headerObject.toString())) {
					return DynamicTable.HEADER_ROW_ID;
				}
				String li = ObjectGUITablePage.this.getLineId(object);
				if(li != null) return li;
				return super.getLineId(object);
			}
		};
		mainTable = new DynamicTable<T>(page, "appTable"+pid()) {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				Collection<T> data;
				if(headerObject != null) {
					data = new ArrayList<>();
					data.add(headerObject);
					data.addAll(getObjectsInTable(req));
				} else data = getObjectsInTable(req);
				updateRows(data, req);
				if(retardationOnGET > 0) try {
					Thread.sleep(retardationOnGET);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		mainTable.setRowTemplate(mainTableRowTemplate);
		
		//build page
		addWidgetsAboveTable();
		page.append(alert);
		page.append(mainTable);
		addWidgetsBelowTable();
	}
	
	public WidgetPage<?> getPage() {
		return page;
	}
	
	@SuppressWarnings("deprecation")
	public void triggerOnPost(OgemaWidget governor, OgemaWidget target) {
		if(registerDependentWidgets) governor.registerDependentWidget(target);
		else governor.triggerAction(target, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
}

