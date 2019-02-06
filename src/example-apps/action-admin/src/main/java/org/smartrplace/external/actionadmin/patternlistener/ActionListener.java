/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
