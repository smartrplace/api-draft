package org.smartrplace.apps.hw.install.gui.expert;

import org.ogema.core.application.ApplicationManager;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.expert.HardwareInstallControllerExpert;
import org.smartrplace.apps.hw.install.gui.MainPage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public class MainPageExpert extends MainPage {
	
	public MainPageExpert(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller);
	}


	@Override
	protected void finishConstructor() {
		DoorWindowSensorTableExpert winSensTable = new DoorWindowSensorTableExpert(page,
				(HardwareInstallControllerExpert) controller, roomsDrop, alert);
		winSensTable.triggerPageBuild();
		triggerPageBuild();		
	}

	@Override
	public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		Thermostat device = super.addWidgetsInternal(object, vh, id, req, row, appMan);
		addWidgetsCommonExpert(object, vh, id, req, row, appMan, device.location().room());
	}
	
}	
