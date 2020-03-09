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
import org.ogema.model.action.BackupAction;
import org.ogema.model.alignedinterval.RepeatingOperationConfiguration;
import org.ogema.util.action.ActionPattern;
import org.smartrplace.intern.backup.BackupInstallController;
import org.smartrplace.intern.backup.logic.XWiki;

/**
 * A variant of a ResourcePattern, which is context sensitive. This means, that a context object
 * is injected upon creation. 
 */
public class BackupPattern extends ActionPattern<BackupAction, BackupInstallController> { 
	
	public StringResource backupDataName = model.backupDataName();
	@Existence(required=CreateMode.OPTIONAL)
	public RepeatingOperationConfiguration operationTimes = model.destinationConfig().operationTimes();
	@Existence(required=CreateMode.OPTIONAL)
	public BooleanResource doZip = model.destinationConfig().doZip();
	public StringResource destination = model.destinationConfig().path();
	public StringResource xwikiPw = model.getSubResource("xwikiPw", StringResource.class);

	@Existence(required=CreateMode.OPTIONAL)
	public BooleanResource triggerByTimer = model.triggerByTimer();
	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public
	 */
	public BackupPattern(Resource device) {
		super(device);
	}
	
	@Override
	protected String getControllingApplication() {
		return SCPTransferPattern.APP_NAME;
	}

	@Override
	protected void performAction() {
		XWiki.backupXWiki(this, context.appMan);;		
	}
}
