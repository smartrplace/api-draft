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
import org.smartrplace.intern.backup.logic.XWiki;
import org.smartrplace.intern.backup.pattern.BackupPattern;

/**
 * A pattern listener for the TemplateContextPattern. It is informed by the framework 
 * about new pattern matches and patterns that no longer match.
 * @deprecated unused
 */
@Deprecated
public class BackupActionListener implements PatternListener<BackupPattern> {
	
	private final BackupInstallController app;
	public final List<BackupPattern> availablePatterns = new ArrayList<>();
	
 	public BackupActionListener(BackupInstallController templateProcess) {
		this.app = templateProcess;
	}
	
 	Timer timer;
 	/** Note that in the pattern accept method you have access to the app controller context
 	 * in this template/listener variant
 	 */
	@Override
	public void patternAvailable(final BackupPattern pattern) {
		availablePatterns.add(pattern);

		if(pattern.triggerByTimer.isActive() && (!pattern.triggerByTimer.getValue())) return;
		XWiki.backupXWiki(pattern, app.appMan);
		timer = app.appMan.createTimer(24*3600*1000, new TimerListener() {
			@Override
			public void timerElapsed(Timer timer) {
				XWiki.backupXWiki(pattern, app.appMan);
			}
		});
	}
	
	@Override
	public void patternUnavailable(BackupPattern pattern) {
		timer.destroy();
		
		availablePatterns.remove(pattern);
	}
	
	
}
