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
package org.smartrplace.intern.backup.pattern;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.resourcemanager.pattern.ContextSensitivePattern;
import org.ogema.model.action.BackupAction;
import org.ogema.model.prototypes.Configuration;
import org.smartrplace.intern.backup.BackupInstallController;

/**
 * A variant of a ResourcePattern, which is context sensitive. This means, that a context object
 * is injected upon creation. 
 */
public class OGEMADatabaseBackupActionPattern extends ContextSensitivePattern<BackupAction, BackupInstallController> { 
	
	public final StringResource controllingApp = model.controllingApplication();
	public final BooleanResource stateControl = model.stateControl();

	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public
	 */
	public OGEMADatabaseBackupActionPattern(Resource device) {
		super(device);
	}
	
	/**
	 * Custom acceptance check
	 */
	@Override
	public boolean accept() {
		if(!controllingApp.getValue().equals("datalog-resadmin")) return false;
		Resource r = model.getParent();
		if(r instanceof Configuration) {
			Configuration c = model.getParent();
			if(c.name().getValue().equals("DailyBackupToSend")) return true;
		}
		return false;
	}
}
