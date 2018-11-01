package org.smartrplace.external.actionadmin.gui;

import java.util.List;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.model.action.Action;
import org.ogema.tools.resource.util.ResourceUtils;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;

/**
 * An HTML page, generated from the Java code.
 */
public class MainPage {
	
	public final long UPDATE_RATE = 5*1000;
	
	public final DynamicTable<Action> gatewayTable;
	
	public MainPage(final WidgetPage<?> page, final ApplicationManager appMan) {

		Header header = new Header(page, "header", "Actions available on the system");
		header.addDefaultStyle(HeaderData.CENTERED);
		page.append(header).linebreak();
		
		Alert alert = new Alert(page, "alert", "");
		alert.setDefaultVisibility(false);
		page.append(alert).linebreak();
		
		gatewayTable = new DynamicTable<Action>(page, "gatewayTable", true);
		List<Action> acl = appMan.getResourceAccess().getResources(Action.class);
		Action ac = null;
		if(!acl.isEmpty()) ac = acl.get(0);
		ActionTemplate template = new ActionTemplate(page, alert, appMan, ac);
		gatewayTable.setRowTemplate(template);
		
		page.append(gatewayTable);
	}
	
	public void addRowIfNotExisting(Action info) {
		String id = ResourceUtils.getValidResourceName(info.getLocation());		
		Set<String> rows = gatewayTable.getRows(null);
		if (!rows.contains(id)) {
			gatewayTable.addItem(info, null);
		}
	}		

}
