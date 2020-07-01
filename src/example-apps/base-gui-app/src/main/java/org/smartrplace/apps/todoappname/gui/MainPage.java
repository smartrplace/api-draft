package org.smartrplace.apps.todoappname.gui;

import org.smartrplace.apps.todoappname.TodoTemplateController;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;

/**
 * An HTML page, generated from the Java code.
 */
public class MainPage {
	
	public final long UPDATE_RATE = 5*1000;
	
	protected final TodoTemplateController controller;
	
	public MainPage(final WidgetPage<?> page, final TodoTemplateController controller) {

		this.controller = controller;
		
		Header header = new Header(page, "header", "User and room access administration");
		header.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_CENTERED);
		page.append(header).linebreak();
		
		Alert alert = new Alert(page, "alert", "");
		alert.setDefaultVisibility(false);
		page.append(alert).linebreak();
		
	}
}
