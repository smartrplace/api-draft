package org.smartrplace.smarteff.admin;

import java.util.Map;

import org.ogema.core.application.Application;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;

import de.iwes.widgets.api.widgets.navigation.NavigationMenu;


public interface ServiceAccess extends Application {
	public Map<String, SmartEffExtensionService> getEvaluations();
	public NavigationMenu getMenu();
}
