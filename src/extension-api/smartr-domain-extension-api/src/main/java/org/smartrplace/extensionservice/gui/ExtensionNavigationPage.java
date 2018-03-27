package org.smartrplace.extensionservice.gui;

import java.util.List;

import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.object.widget.init.LoginInitSingleEmpty;
/**
 * Frame for navigation pages
 */
public abstract class ExtensionNavigationPage<T extends ExtensionUserDataNonEdit> {
	protected abstract List<T> getUsers(OgemaHttpRequest req);
	
	public final WidgetPage<?> page;
	public final String url;
	public final String overviewUrl;
	
	private final TemplateInitSingleEmpty<String> init;
	protected LoginInitSingleEmpty<T> loggedIn;
	protected abstract void init(OgemaHttpRequest req);

	public ExtensionNavigationPage(final WidgetPage<?> page, String url, String overviewUrl,
			String providerId) {
		this.page = page;
		this.url = url;
		this.overviewUrl = overviewUrl;
		init = new TemplateInitSingleEmpty<String>(page, "init"+providerId, false) {
			private static final long serialVersionUID = 3798965126759319288L;
			
			@Override
			public void init(OgemaHttpRequest req) {
				super.init(req);
				ExtensionNavigationPage.this.init(req);
			}
			@Override
			protected String getItemById(String configId) {
				return configId;
			}
		};
		page.append(init);
		
		loggedIn = new LoginInitSingleEmpty<T>(page, "loggedIn"+providerId) {
			private static final long serialVersionUID = 6446396416992821986L;

			@Override
			protected List<T> getUsers(OgemaHttpRequest req) {
				return ExtensionNavigationPage.this.getUsers(req);
			}
		};
		page.append(loggedIn);
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
