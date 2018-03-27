package org.smartrplace.smarteff.admin.gui;

import java.util.Collection;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider.EntryType;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.admin.object.NavigationPageData;
import org.smartrplace.smarteff.admin.util.SmartrEffUtil;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;

/**
 * An HTML page, generated from the Java code.
 */
public class NaviOverviewPage extends ObjectGUITablePage<NavigationPageData, Resource> {
	public static final float MIN_COMFORT_TEMP = 4;
	public static final float MAX_COMFORT_TEMP = 30;
	public static final float DEFAULT_COMFORT_TEMP = 21;
	
	private final SpEffAdminController app;
	
	ValueResourceTextField<TimeResource> updateInterval;

	public NaviOverviewPage(final WidgetPage<?> page, final SpEffAdminController app,
			NavigationPageData initData) {
		super(page, app.appMan, initData);
		this.app = app;
	}
	
	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "header", "Navigation Page Overview");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
	}
	
	@Override
	public Collection<NavigationPageData> getObjectsInTable(OgemaHttpRequest req) {
		Collection<NavigationPageData> providers = app.guiPageAdmin.getAllProviders(); 
		return providers;
	}

	@Override
	public void addWidgets(NavigationPageData object, ObjectResourceGUIHelper<NavigationPageData, Resource> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		if(req != null)
			vh.stringLabel("Name", id, object.provider.label(req.getLocale()), row);
		else
			vh.registerHeaderEntry("Name");
		String text = null;
		if(object.provider.getEntryType() == null) text = "Start Page";
		else for(EntryType t: object.provider.getEntryType()) {
			if(text == null) text = t.getType().getSimpleName();
			else text += "; "+t.getType().getSimpleName();
		}
		vh.stringLabel("Entry Types", id, text, row);
		if(object.provider.getEntryType() == null)
			vh.linkingButton("Open", id, object, row, "Open", object.url);
		else
			vh.stringLabel("Open", id, "--", row);
	}
	
	@Override
	public Resource getResource(NavigationPageData object, OgemaHttpRequest req) {
		return null;
	}
	
	@Override
	public String getLineId(NavigationPageData object) {
		String name = SmartrEffUtil.buildValidWidgetId(object.provider);
		return name + super.getLineId(object);
	}
}
