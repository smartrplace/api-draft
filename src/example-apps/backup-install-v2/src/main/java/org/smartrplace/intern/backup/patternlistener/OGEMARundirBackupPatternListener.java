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

import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.resourcemanager.pattern.PatternListener;
import org.smartrplace.intern.backup.BackupInstallController;
import org.smartrplace.intern.backup.logic.OGEMARundir;
import org.smartrplace.intern.backup.pattern.OGEMARundirBackupPattern;

import de.iwes.util.timer.AbsoluteTimeHelper;

/**
 * A pattern listener for the TemplateContextPattern. It is informed by the framework 
 * about new pattern matches and patterns that no longer match.
 * @deprecated still needed?
 */
@Deprecated 
public class OGEMARundirBackupPatternListener implements PatternListener<OGEMARundirBackupPattern> {
	
	private final BackupInstallController app;
	public final List<OGEMARundirBackupPattern> availablePatterns = new ArrayList<>();
	
 	public OGEMARundirBackupPatternListener(BackupInstallController templateProcess) {
		this.app = templateProcess;
	}
	
 	Timer timer;
 	/** Note that in the pattern accept method you have access to the app controller context
 	 * in this template/listener variant
 	 */
	@Override
	public void patternAvailable(final OGEMARundirBackupPattern pattern) {
		availablePatterns.add(pattern);

		OGEMARundir.backupRundir(pattern, app.appMan);
		long interval;
		if(pattern.operationTimeType.isActive()) {
			interval = AbsoluteTimeHelper.getStandardInterval(pattern.operationTimeType.getValue());
		} else {
			interval = 24*3600*1000;
		}
		timer = app.appMan.createTimer(interval, new TimerListener() {
			@Override
			public void timerElapsed(Timer timer) {
				OGEMARundir.backupRundir(pattern, app.appMan);
			}
		});		
	}
	
	@Override
	public void patternUnavailable(OGEMARundirBackupPattern pattern) {
		timer.destroy();
		
		availablePatterns.remove(pattern);
	}
	
	
}
