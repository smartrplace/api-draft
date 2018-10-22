package org.smartrplace.external.actionadmin.config;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Configuration;

/** 
 * The global configuration resource type for this app.
 */
public interface ActionAdminConfig extends Configuration {

	ResourceList<ActionAdminProgramConfig> availablePrograms();
	
	StringResource serverIP();
	
	// TODO add global settings

}
