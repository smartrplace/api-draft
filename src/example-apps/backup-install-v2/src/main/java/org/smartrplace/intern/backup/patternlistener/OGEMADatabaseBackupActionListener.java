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
package org.smartrplace.intern.backup.patternlistener;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.resourcemanager.pattern.PatternListener;

import org.smartrplace.intern.backup.BackupInstallController;
import org.smartrplace.intern.backup.pattern.OGEMADatabaseBackupActionPattern;

/**
 * A pattern listener for the TemplateContextPattern. It is informed by the framework 
 * about new pattern matches and patterns that no longer match.
 * @deprecated unused
 */
@Deprecated
public class OGEMADatabaseBackupActionListener implements PatternListener<OGEMADatabaseBackupActionPattern> {
	
	private final BackupInstallController app;
	public final List<OGEMADatabaseBackupActionPattern> availablePatterns = new ArrayList<>();
	
 	public OGEMADatabaseBackupActionListener(BackupInstallController templateProcess) {
		this.app = templateProcess;
	}
	
 	/** Note that in the pattern accept method you have access to the app controller context
 	 * in this template/listener variant
 	 */
	@Override
	public void patternAvailable(OGEMADatabaseBackupActionPattern pattern) {
		availablePatterns.add(pattern);
//		app.databaseBackupAction = pattern;
	}
	
	@Override
	public void patternUnavailable(OGEMADatabaseBackupActionPattern pattern) {
		// TODO process remove
		
		availablePatterns.remove(pattern);
	}
	
	
}
