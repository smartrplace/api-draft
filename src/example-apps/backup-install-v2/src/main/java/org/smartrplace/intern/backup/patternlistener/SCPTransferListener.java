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
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.resourcemanager.pattern.PatternListener;
import org.smartrplace.intern.backup.BackupInstallController;
import org.smartrplace.intern.backup.pattern.SCPTransferPattern;

/**
 * A pattern listener for the TemplateContextPattern. It is informed by the framework 
 * about new pattern matches and patterns that no longer match.
 */
public class SCPTransferListener implements PatternListener<SCPTransferPattern>, ResourceValueListener<TimeResource> {
	
	private final BackupInstallController app;
	public final List<SCPTransferPattern> availablePatterns = new ArrayList<>();
	
 	public SCPTransferListener(BackupInstallController templateProcess) {
		this.app = templateProcess;
	}
	
 	private Timer timer;
 	boolean createDateDir;
 	/** Note that in the pattern accept method you have access to the app controller context
 	 * in this template/listener variant
 	 */
	@Override
	public void patternAvailable(final SCPTransferPattern pattern) {
System.out.println("    Pattern available for "+pattern.model.getLocation());
		if (timer != null) {
System.out.println("    Timer destroy for "+pattern.model.getLocation());
			timer.destroy();
		}
		availablePatterns.add(pattern);

		//if(pattern.sourceInfo.getValue().contains("box")) {
		//	createDateDir = false;
		//} else 
		createDateDir = true;
		try {
			pattern.performAction();
		} catch(Exception e) {
			e.printStackTrace();
			System.out.println("    Go on with Timer...");
		}
System.out.println("    Perform action done for "+pattern.model.getLocation());
		long interval;
		if(pattern.operationTimeInterval.isActive()) {
			interval = pattern.operationTimeInterval.getValue();
		} else {
			interval = 24*3600*1000;
		}
System.out.println("Starting timer with interval "+interval);
		timer = app.appMan.createTimer(interval, new TimerListener() {
			@Override
			public void timerElapsed(Timer timer) {
System.out.println("    Perform Backup ZIP Action by timer");
				pattern.performAction();
			}
		});
		if(pattern.operationTimeInterval.isActive()) {
			pattern.operationTimeInterval.addValueListener(this);
		}
	}
	
	@Override
	public void patternUnavailable(SCPTransferPattern pattern) {
System.out.println("    BACKUP-INSTALL-V2: patternUnavailable!");
		timer.destroy();
		pattern.operationTimeInterval.removeValueListener(this);
		availablePatterns.remove(pattern);
	}
	
	@Override
	public void resourceChanged(TimeResource resource) {
		if (resource.getValue() > 5*60000) {
System.out.println("    BACKUP-INSTALL-V2, set timer interval:"+resource.getValue());
			timer.setTimingInterval(resource.getValue());
		}
	}
	
	
}
