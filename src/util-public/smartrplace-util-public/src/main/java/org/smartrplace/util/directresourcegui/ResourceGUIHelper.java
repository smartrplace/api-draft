package org.smartrplace.util.directresourcegui;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.TemplateInitSingleEmpty;
import de.iwes.widgets.resource.widget.init.ResourceInitSingleEmpty;

/** Variant of {@link ObjectResourceGUIHelper} that uses a resource type as template type. If you are
 * using a resource as template type this version is recommended as it provides full support for
 * item edit / display pages with a ResourceInitSingleEmpty
 */
public class ResourceGUIHelper<T extends Resource> extends ObjectResourceGUIHelper<T, T> {
	//one of init or fixedGatewayInfo them must be null
	protected final ResourceInitSingleEmpty<T> init;

	public ResourceGUIHelper(WidgetPage<?> page, ResourceInitSingleEmpty<T> init,
			ApplicationManager appMan, boolean acceptMissingResources) {
		super(page, (TemplateInitSingleEmpty<T>)null, appMan, acceptMissingResources);
		this.init = init;
	}
	public ResourceGUIHelper(WidgetPage<?> page, T fixedGatewayInfo,
			ApplicationManager appMan, boolean acceptMissingResources) {
		super(page, fixedGatewayInfo, appMan, acceptMissingResources);
		this.init = null;
	}
	public ResourceGUIHelper(OgemaWidget parent, OgemaHttpRequest req, T fixedGatewayInfo,
			ApplicationManager appMan, boolean acceptMissingResources) {
		super(parent, req, fixedGatewayInfo, appMan, acceptMissingResources);
		this.init = null;
	}
	
	@Override
	protected T getGatewayInfo(OgemaHttpRequest req) {
		if(fixedGatewayInfo != null) return fixedGatewayInfo;
		return init.getSelectedItem(req);
	}

	@Override
	protected T getResource(T object, OgemaHttpRequest req) {
		return object;
	}
}
