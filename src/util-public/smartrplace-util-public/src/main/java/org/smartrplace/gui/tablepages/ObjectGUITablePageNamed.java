package org.smartrplace.gui.tablepages;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;

public abstract class ObjectGUITablePageNamed <T, R extends Resource> extends ObjectGUITablePage<T, R> {
	public ObjectGUITablePageNamed(WidgetPage<?> page, ApplicationManager appMan, T initSampleObject) {
		super(page, appMan, initSampleObject, false);
		//You have to call triggerPageBuild yourself
	}
	protected abstract String getTypeName(OgemaLocale locale);
	protected String getHeader(OgemaLocale locale) {
		return "View and Configuration for "+getTypeName(locale);
	}
	protected abstract String getLabel(T obj, OgemaHttpRequest req);

	@Override
	public R getResource(T object, OgemaHttpRequest req) {
		return null;
	}

	protected void addNameLabel(T object, ObjectResourceGUIHelper<T, R> vh, String id, Row row,
			OgemaHttpRequest req) {
		if(req == null)
			vh.stringLabel(getTypeName(null), "name"+id, "init", row);
		else
			vh.stringLabel(getTypeName(null), "name"+id, getLabel(object, req), row);
	};
	/** In this version you get back the name value
	 * 
	 * @param object
	 * @param vh
	 * @param id
	 * @param row
	 * @param req
	 * @return name value
	 */
	protected String addNameLabelPlus(T object, ObjectResourceGUIHelper<T, R> vh, String id, Row row,
			OgemaHttpRequest req) {
		String name;
		if(req == null)
			name = "init";
		else
			name = getLabel(object, req);
		vh.stringLabel(getTypeName(null), "name"+id, name, row);
		return name;
	};

	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, WidgetHelper.getValidWidgetId("headerStdPermPage"+this.getClass().getSimpleName())) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				setText(getHeader(req.getLocale()), req);
			}
		};
		page.append(header);
	}
}
