package org.smartrplace.extensionservice.gui;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.html.form.button.RedirectButton;
/**
 * Frame for navigation pages
 */
public class ExtensionNavigationPage {
	public final WidgetPage<?> page;
	public final String url;
	public final String overviewUrl;
	
	public ExtensionNavigationPage(final WidgetPage<?> page, String url, String overviewUrl) {
		this.page = page;
		this.url = url;
		this.overviewUrl = overviewUrl;
	}
	
	public void finalize(StaticTable table) {
		String mainUrl = overviewUrl;
		if(mainUrl != null) {
			RedirectButton mainPageBut = new RedirectButton(page, "mainPageBut", "Main page",
					mainUrl);
			page.append(mainPageBut);
		}
	}
	public static void registerDependentWidgets(OgemaWidget governor, StaticTable table) {
		for(OgemaWidget el: table.getSubWidgets()) {
			governor.triggerOnPOST(el);
		}
	}
}
