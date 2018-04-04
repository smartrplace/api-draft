package org.smartrplace.smarteff.admin.gui;

import java.util.Collection;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.TimeResource;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.admin.object.SmartrEffExtResourceTypeData;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;

/**
 * An HTML page, generated from the Java code.
 */
public class ResTypePage extends ObjectGUITablePage<SmartrEffExtResourceTypeData, Resource> {
	public static final float MIN_COMFORT_TEMP = 4;
	public static final float MAX_COMFORT_TEMP = 30;
	public static final float DEFAULT_COMFORT_TEMP = 21;
	
	private final SpEffAdminController app;
	
	ValueResourceTextField<TimeResource> updateInterval;

	public ResTypePage(final WidgetPage<?> page, final SpEffAdminController app,
			SmartrEffExtResourceTypeData initData) {
		super(page, app.appMan, initData);
		this.app = app;
	}
	
	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "header", "Data Type Overview");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
	}
	
	@Override
	public Collection<SmartrEffExtResourceTypeData> getObjectsInTable(OgemaHttpRequest req) {
		Collection<SmartrEffExtResourceTypeData> providers = app.typeAdmin.resourceTypes.values(); 
		return providers;
	}

	@Override
	public void addWidgets(SmartrEffExtResourceTypeData object, ObjectResourceGUIHelper<SmartrEffExtResourceTypeData, Resource> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		vh.stringLabel("Name", id, object.typeDeclaration.resourceName(), row);
		vh.stringLabel("Resource Type", id, object.resType.getName(), row);
		vh.stringLabel("Public", id, ""+object.numberPublic, row);
		vh.stringLabel("ReadOnly", id, ""+object.numberNonEdit, row);
		vh.stringLabel("ReadWrite", id, ""+(object.numberTotal-object.numberNonEdit-object.numberPublic), row);
		vh.linkingButton("Data Explorer", id, object, row, "Resources", "dataExplorer.html");
//if(configRes != null) try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
		
	}
	
	@Override
	public Resource getResource(SmartrEffExtResourceTypeData object, OgemaHttpRequest req) {
		return null;
	}
	
	@Override
	public String getLineId(SmartrEffExtResourceTypeData object) {
		String name = WidgetHelper.getValidWidgetId(object.typeDeclaration.resourceName());
		return name + super.getLineId(object);
	}
}
