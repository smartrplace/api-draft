package de.iwes.sema.overview.impl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application;
import org.ogema.core.application.ApplicationManager;

import de.iwes.tools.apps.collections.api.DisplayableApp;
import de.iwes.tools.apps.collections.base.AppCollection;
import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.widgets.WidgetApp;

@Component
@Service(Application.class)
public class SemaOverview implements Application {
	
	private final static Map<String,String> STATIC_APPS;
	private WidgetApp wApp;
	
	static {
		final Map<String,String> apps = new LinkedHashMap<>();
		apps.put("org.smartrplace.frontends.sema-frontend", "SEMA dashboard");
		apps.put("org.smartrplace.apps.smartrplace-heatcontrol-v2", "Heatcontrol app");
		apps.put("de.iwes.apps.window-opened-detector", "Window open detector");
		apps.put("com.example.app.batterystatemonitoring", "Battery state monitoring");
		apps.put("org.ogema.apps.load-monitoring", "Load monitoring");
		apps.put("de.iwes.apps.activate-log-modus", "Log data transfer configuration");
		apps.put("de.iwes.ogema.rexometer-viewer", "RExometer configuration");
		apps.put("org.ogema.messaging.message-settings", "Messaging configuration");
		apps.put("org.ogema.messaging.message-forwarding", "Message forwarding configuration");
		apps.put("org.smartrplace.store.appstore-gui-dev", "Appstore");
		apps.put("de.iwes.widgets.datalog-bathing-v2", "Bath Heat Control");
		apps.put("de.iwes.tools.schedule-viewer-basic-example", "Schedule Viewer Expert");
		apps.put("org.smartrplace.apps.heatcontrol-overview", "Heat Control Overview Extension");
		apps.put("de.iwes.sema.highscore", "SEMA High Score");
		STATIC_APPS = Collections.unmodifiableMap(apps);
	}
	
	@Reference
	private OgemaGuiService guiService;
	
	private AppCollection<DisplayableApp> apps;

	@Override
	public void start(ApplicationManager appManager) {
		wApp = guiService.createWidgetApp("/de/iee/sema/apps-overview", appManager);
		apps = new AppCollection<DisplayableApp>(appManager, wApp) {

			@Override
			protected String pageTitle() {
				return "SEMA Apps";
			}

			@Override
			protected Map<String, String> staticApps() {
				return STATIC_APPS;
			}
		};
	}

	@Override
	public void stop(AppStopReason reason) {
		final WidgetApp wApp = this.wApp;
		this.wApp = null;
		this.apps = null;
		if (wApp != null)
			wApp.close();
	}

	
	
}
