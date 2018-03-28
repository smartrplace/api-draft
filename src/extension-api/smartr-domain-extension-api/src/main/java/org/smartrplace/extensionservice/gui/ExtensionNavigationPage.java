package org.smartrplace.extensionservice.gui;

import java.util.List;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.smartrplace.extenservice.resourcecreate.ExtensionResourceAccessInitData;
import org.smartrplace.extensionservice.ExtensionUserDataNonEdit;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.html.form.button.TemplateRedirectButton;
import de.iwes.widgets.object.widget.init.LoginInitSingleEmpty;
/**
 * Frame for navigation pages
 */
public abstract class ExtensionNavigationPage<T extends ExtensionUserDataNonEdit, C extends ExtensionResourceAccessInitData> {
	protected abstract List<T> getUsers(OgemaHttpRequest req);
	
	public final WidgetPage<?> page;
	public final String url;
	public final String overviewUrl;
	
	public final TemplateInitSingleEmpty<C> init;
	protected LoginInitSingleEmpty<T> loggedIn;
	protected void init(OgemaHttpRequest req) {};
	protected abstract  C getItemById(String configId, OgemaHttpRequest req);

	public ExtensionNavigationPage(final WidgetPage<?> page, String url, String overviewUrl,
			String providerId) {
		this.page = page;
		this.url = url;
		this.overviewUrl = overviewUrl;
		init = new TemplateInitSingleEmpty<C>(page, "init"+providerId, false) {
			private static final long serialVersionUID = 3798965126759319288L;
			
			@Override
			public void init(OgemaHttpRequest req) {
				loggedIn.triggeredInit(req);
				Map<String,String[]> params = getPage().getPageParameters(req);
				C res = null;
				if (params == null || params.isEmpty())
					res = ExtensionNavigationPage.this.getItemById(null, req);
				else {
					String[] patterns = params.get(TemplateRedirectButton.PAGE_CONFIG_PARAMETER);
					if (patterns == null || patterns.length == 0)
						res = ExtensionNavigationPage.this.getItemById(null, req);
					else {
						final String selected = patterns[0];
						try {
							res = ExtensionNavigationPage.this.getItemById(selected, req); // may return null or throw an exception
						} catch (Exception e) { // if the type does not match
							LoggerFactory.getLogger(TemplateInitSingleEmpty.class).info("Empty template widget could not be initialized with the selected value {}",selected,e);
						}
					}
				}
				if (res == null)
					return;
				getData(req).selectItem(res);
				ExtensionNavigationPage.this.init(req);
			}
			@Override
			protected C getItemById(String configId) {
				throw new IllegalStateException("Standard getItemById method replaced to get request !!!");
			}
		};
		page.append(init);
		
		loggedIn = new LoginInitSingleEmpty<T>(page, "loggedIn"+providerId, true) {
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
