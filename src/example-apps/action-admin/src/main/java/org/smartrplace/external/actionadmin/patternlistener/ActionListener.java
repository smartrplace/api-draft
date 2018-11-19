package org.smartrplace.external.actionadmin.patternlistener;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.resourcemanager.pattern.PatternListener;

import org.smartrplace.external.actionadmin.ActionAdminController;
import org.smartrplace.external.actionadmin.pattern.ActionPattern;

/**
 * A pattern listener for the TemplatePattern. It is informed by the framework 
 * about new pattern matches and patterns that no longer match.
 */
public class ActionListener implements PatternListener<ActionPattern> {
	
	private final ActionAdminController app;
	public final List<ActionPattern> availablePatterns = new ArrayList<>();
	
 	public ActionListener(ActionAdminController templateProcess) {
		this.app = templateProcess;
	}
	
	@Override
	public void patternAvailable(ActionPattern pattern) {
		availablePatterns.add(pattern);
		
		app.mainPage.addRowIfNotExisting(pattern.model);
		app.processInterdependies();
	}
	@Override
	public void patternUnavailable(ActionPattern pattern) {
		// TODO process remove
		
		availablePatterns.remove(pattern);
		app.processInterdependies();
	}
	
	
}
