package org.smartrplace.apps.hw.install.gui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.util.DeviceTableBase.InstalledAppsSelector;
import org.ogema.devicefinder.util.LastContactLabel;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.HardwareInstallController;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.HeaderData;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.textfield.TextField;

public class MainPage extends DeviceTablePageFragment implements InstalledAppsSelector {

	public MainPage(WidgetPage<?> page, HardwareInstallController controller) {
		super(page, controller, null, null);
		super.addWidgetsAboveTable();
		finishConstructor();
	}
	
	protected void finishConstructor() {
		updateTables();
		DoorWindowSensorTable winSensTable = new DoorWindowSensorTable(page, controller, roomsDrop, alert);
		winSensTable.triggerPageBuild();
		triggerPageBuild();		
	}
	
	Set<String> tableProvidersDone = new HashSet<>();
	public void updateTables() {
		synchronized(tableProvidersDone) {
		if(controller.hwInstApp != null) for(DeviceHandlerProvider<?> pe: controller.hwInstApp.getTableProviders().values()) {
			String id = pe.id();
			if(tableProvidersDone.contains(id))
				continue;
			tableProvidersDone.add(id);
			pe.getDeviceTable(page, appMan, alert, this).triggerPageBuild();
		}
		}
	}
	
	@Override
	protected Class<? extends Resource> getResourceType() {
		return Thermostat.class;
	}
	
	@Override
	public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		addWidgetsInternal(object, vh, id, req, row, appMan);
	}
	public Thermostat addWidgetsInternal(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		//if(!(object.device() instanceof Thermostat) && (req != null)) return null;
		final Thermostat device;
		if(req == null)
			device = ResourceHelper.getSampleResource(Thermostat.class);
		else
			device = (Thermostat) object.device();
		//if(!(object.device() instanceof Thermostat)) return;
		final String name;
		if(device.getLocation().toLowerCase().contains("homematic")) {
			name = "Thermostat HM:"+ScheduleViewerOpenButtonEval.getDeviceShortId(device.getLocation());
		} else
			name = ResourceUtils.getHumanReadableShortName(device);
		vh.stringLabel("Name", id, name, row);
		Label setpointFB = vh.floatLabel("Setpoint", id, device.temperatureSensor().deviceFeedback().setpoint(), row, "%.1f");
		if(req != null) {
			TextField setpointSet = new TextField(mainTable, "setpointSet"+id, req) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					setValue(String.format("%.1f", device.temperatureSensor().deviceFeedback().setpoint().getCelsius()), req);
				}
				@Override
				public void onPOSTComplete(String data, OgemaHttpRequest req) {
					String val = getValue(req);
					val = val.replaceAll("[^\\d.]", "");
					try {
						float value  = Float.parseFloat(val);
						if(value < 4.5f || value> 30.5f) {
							alert.showAlert("Allowed range: 4.5 to 30°C", false, req);
						} else
							device.temperatureSensor().settings().setpoint().setCelsius(value);
					} catch (NumberFormatException | NullPointerException e) {
						if(alert != null) alert.showAlert("Entry "+val+" could not be processed!", false, req);
						return;
					}
				}
			};
			row.addCell("Set", setpointSet);
		} else
			vh.registerHeaderEntry("Set");
		Label tempmes = vh.floatLabel("Measurement", id, device.temperatureSensor().reading(), row, "%.1f#min:-200");
		vh.floatLabel("Battery", id, device.battery().internalVoltage().reading(), row, "%.1f#min:0.1");
		Label lastContact = null;
		if(req != null) {
			lastContact = new LastContactLabel(device.temperatureSensor().reading(), appMan, mainTable, "lastContact"+id, req);
			row.addCell(WidgetHelper.getValidWidgetId("Last Contact"), lastContact);
		} else
			vh.registerHeaderEntry("Last Contact");
		
		addWidgetsCommon(object, vh, id, req, row, appMan, device.location().room());
		if(req != null) {
			tempmes.setPollingInterval(DEFAULT_POLL_RATE, req);
			setpointFB.setPollingInterval(DEFAULT_POLL_RATE, req);
			lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
		}
		return device;
	}
	
	@Override
	public void addWidgetsAboveTable() {
		//super.addWidgetsAboveTable();
		Header headerThermostat = new Header(page, "headerThermostat", "Thermostats");
		headerThermostat.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_CENTERED);
		page.append(headerThermostat);
	}

	@Override
	public List<InstallAppDevice> getDevicesSelected() {
		return roomsDrop.getDevicesSelected();
	}

	@Override
	public <T extends Resource> InstallAppDevice addDeviceIfNew(T model) {
		return controller.addDeviceIfNew(model);
	}

	@Override
	public <T extends Resource> InstallAppDevice removeDevice(T model) {
		return controller.removeDevice(model);
	}
}


