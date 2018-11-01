/**
 * Copyright 2009 - 2014
 *
 * Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Fraunhofer IIS
 * Fraunhofer ISE
 * Fraunhofer IWES
 *
 * All Rights reserved
 */
package org.smartrplace.external.actionadmin.pattern;

import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.model.action.Action;

public class ActionPattern extends ResourcePattern<Action> { 

	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public.
	 */
	public ActionPattern(Resource device) {
		super(device);
	}

}
