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
package org.smartrplace.apps.hw.install.expert;

import org.ogema.core.application.ApplicationManager;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.apps.hw.install.gui.expert.MainPageExpert;

import de.iwes.widgets.api.widgets.WidgetPage;

// here the controller logic is implemented
public class HardwareInstallControllerExpert extends HardwareInstallController {

	public HardwareInstallControllerExpert(ApplicationManager appMan, WidgetPage<?> page) {
		super(appMan, page);
	}

	@Override
	protected MainPage getMainPage(WidgetPage<?> page) {
		return new MainPageExpert(page, this);
	}

}
