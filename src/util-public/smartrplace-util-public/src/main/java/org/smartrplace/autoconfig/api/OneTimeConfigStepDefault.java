package org.smartrplace.autoconfig.api;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.gateway.EvalCollection;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;

public class OneTimeConfigStepDefault implements OneTimeConfigStep {
	protected final String id;
	protected final boolean performUpdates;
	
	public OneTimeConfigStepDefault(String id, ApplicationManager appMan) {
		this.id = id;
		EvalCollection ec = ResourceHelper.getEvalCollection(appMan);
		ec.initDoneStatus().create();
		StringResource defaultInit = ResourceListHelper.getOrCreateNamedElement("default", ec.initDoneStatus());
		this.performUpdates = !InitialConfig.isInitDone(id, defaultInit);
		if(performUpdates)
			InitialConfig.addString(id, defaultInit);			

	}

	@Override
	public boolean performConfig(String variableLocation) {
		return performUpdates;
	}
}
