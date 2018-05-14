package org.smartrplace.util.directobjectgui;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.util.directresourcegui.LabelProvider;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.template.DisplayTemplate;
/**
 * An HTML page, generated from the Java code.
 */
public abstract class ObjectGUIEditPage<T, R extends Resource> {
	public abstract R getResource(OgemaHttpRequest req);
	
	/** Overwrite this method to provide set of objects
	 * @param req
	 * @return
	 */
	public abstract Collection<T> getObjectsInTable(OgemaHttpRequest req);
	
	public final long UPDATE_RATE = 5*1000;

	protected final ApplicationManager appMan;
	
	//private Label storeLabel = null;
	protected final WidgetPage<?> page;
	protected final TemplateInitSingleEmpty<T> init;
	protected final TemplateDropdown<T> drop;
	protected final T aggregatedData;
	
	protected Map<String, T> knownItems = new HashMap<>();
	
	/**Overwrite this to provide different url or set to null to avoid having the "Main Page" button*/
	protected String getOverviewPageUrl() {
		return "index.html";
	}
	
	public ObjectGUIEditPage(final WidgetPage<?> page, final ApplicationManager appMan,
			T aggregatedData, final Class<T> resourceType, final LabelProvider<T> dropLabels) {
		this.appMan = appMan;
		this.page = page;
		this.aggregatedData = aggregatedData;
		init = new TemplateInitSingleEmpty<T>(page, "init", true) {
			private static final long serialVersionUID = 1L;
			@Override
			public T getSelectedItem(OgemaHttpRequest req) {
				T res = super.getSelectedItem(req);
				if(res!= null) return res;
				//res.remoteData().connectionCounter()
				res =  ObjectGUIEditPage.this.aggregatedData;
				if(res!= null) return res;
				return drop.getSelectedItem(req);
			}
			@Override
			public void updateDependentWidgets(OgemaHttpRequest req) {
				T res = getSelectedItem(req);
				Collection<? extends T> items = getObjectsInTable(req);
				drop.update(items , req);
				drop.selectItem(res, req);
				/*for( T opt: drop.getItems(req)) {
					if(opt.equalsLocation(res)) {
						drop.selectItem(opt, req);
						break;
					}
				}*/
			}
			@Override
			protected T getItemById(String configId) {
				return knownItems.get(configId);
			}
		};
		
		drop = new TemplateDropdown<T>(page, "drop") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				init.selectItem(getSelectedItem(req), req);
			}
		};
		drop.setTemplate(new DisplayTemplate<T>() {
			@Override
			public String getLabel(T object, OgemaLocale locale) {
				return dropLabels.getLabel(object);
			}
			
			@Override
			public String getId(T object) {
				String result = ResourceUtils.getValidResourceName(object.toString());
				knownItems.put(result, object);
				return result;
			}
		});
		//Note: Synchronization issues with triggerAction
		//init.triggerOnPOST(drop);
		init.registerDependentWidget(drop);
		page.append(drop);
		
	}
	
	protected void finalize(StaticTable table) {
		if(table != null) registerDependentWidgets(drop, table);
		String mainUrl = getOverviewPageUrl();
		if(mainUrl != null) {
			RedirectButton mainPageBut = new RedirectButton(page, "mainPageBut", "Main page",
					mainUrl);
			page.append(mainPageBut);
		}
	}
	public static void registerDependentWidgets(OgemaWidget governor, StaticTable table) {
		for(OgemaWidget el: table.getSubWidgets()) {
			//Note: Synchronization issues with triggerAction
			//governor.triggerOnPOST(el);
			governor.registerDependentWidget(el);
		}
	}
}
