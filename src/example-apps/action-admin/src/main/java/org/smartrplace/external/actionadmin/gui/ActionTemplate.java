package org.smartrplace.external.actionadmin.gui;

import java.util.Collections;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.model.action.Action;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.util.directresourcegui.ResourceGUIHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetGroup;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate;

public class ActionTemplate extends RowTemplate<Action> {
	
	private final WidgetPage<?> page;
	private final Alert alert;
	private final ApplicationManager appMan;
	private final WidgetGroup globalGroup;
	
	ResourceGUIHelper<Action> mhinit = null;
	private boolean isInInit = false;
	
	public ActionTemplate(WidgetPage<?> page, Alert alert, ApplicationManager appMan,
			Action sampleAction) {
		this.page = page;
		this.alert = alert;
		this.appMan = appMan;
		this.globalGroup = page.registerWidgetGroup("globalGroup", Collections.<OgemaWidget> emptyList());
		globalGroup.setPollingInterval(5000);
		
		if(sampleAction != null) {
			isInInit = true;
			addRow(sampleAction, null);
			isInInit = false;
		}
	}
	
	public Map<String,Object>  getHeader() {
		if(mhinit != null) {
			Map<String,Object> map2 = mhinit.getHeader();
			return map2;
		}
		
		return null;
	}
 	
	@Override
	public Row addRow(final Action action, OgemaHttpRequest req) {
		Row row = new Row();
		String id;
		
		ResourceGUIHelper<Action> mh = new ResourceGUIHelper<>(page, action, appMan, false);
		if(isInInit) {
			mhinit = mh = new ResourceGUIHelper<Action>(page, (Action)null, appMan, false);
			id = "";
		}
		else {
			id = getLineId(action);
		}
		mh.resourceLabel("id", id, action, row, 0);
		mh.resourceLabel("type", id, action, row, 11);
		mh.resourceLabel("parenttype", id, action.getParent(), row, 11);
		mh.stringLabel("application", id, action.controllingApplication(), row);
		mh.stringLabel("description", id,  action.description(), row);
		mh.booleanEdit("stateControl", id, action.stateControl(), row);
		mh.booleanEdit("stateFeedback", id, action.stateFeedback(), row);
		
		return row;
	}
	
	@Override
	public String getLineId(Action action) {
		String id = ResourceUtils.getValidResourceName(action.getLocation());
		return id;
	}
}