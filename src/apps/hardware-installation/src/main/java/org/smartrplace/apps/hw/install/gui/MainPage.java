package org.smartrplace.apps.hw.install.gui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.apps.roomsim.service.api.util.RoomSimConfigPatternI;
import org.ogema.apps.roomsim.service.api.util.SingleRoomSimulationBaseImpl;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.util.LastContactLabel;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
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
		//DoorWindowSensorTable winSensTable = new DoorWindowSensorTable(page, controller, this, alert);
		//winSensTable.triggerPageBuild();
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
			pe.getDeviceTable(page, alert, this).triggerPageBuild();
		}
		}
	}
	
	@Override
	protected Class<? extends Resource> getResourceType() {
		return Resource.class;
	}
	
	@Override
	public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		//addWidgetsInternal(object, vh, id, req, row, appMan);
	}
	
	@Override
	public void addWidgetsAboveTable() {
		//super.addWidgetsAboveTable();
		/* moved
		Header headerThermostat = new Header(page, "headerThermostat", "Thermostats (old)");
		headerThermostat.addDefaultStyle(HeaderData.TEXT_ALIGNMENT_CENTERED);
		page.append(headerThermostat);
		*/
	}

	@Override
	public List<InstallAppDevice> getDevicesSelected() {
		List<InstallAppDevice> all = roomsDrop.getDevicesSelected();
		if (installFilterDrop != null)  // FIXME seems to always be null here
			all = installFilterDrop.getDevicesSelected(all);
		return all;
	}

	@Override
	public InstallAppDevice getInstallResource(Resource device) {
		for(InstallAppDevice dev: controller.appConfigData.knownDevices().getAllElements()) {
			if(dev.device().equalsLocation(device))
				return dev;
		}
		return null;
	}
	
	@Override
	public <T extends Resource> InstallAppDevice addDeviceIfNew(T model, DeviceHandlerProvider<T> tableProvider) {
		return controller.addDeviceIfNew(model, tableProvider);
	}

	@Override
	public <T extends Resource> InstallAppDevice removeDevice(T model) {
		return controller.removeDevice(model);
	}

	//@Override
	//public ApplicationManager getAppManForSimulationStart() {
	//	return appMan;
	//}

	@Override
	public <T extends Resource> void startSimulation(DeviceHandlerProvider<T> tableProvider, T device) {
		controller.startSimulation(tableProvider, device);
	}
	
	Map<String, SingleRoomSimulationBaseImpl> roomSimulations = new HashMap<>();
	@Override
	public <T extends Resource> SingleRoomSimulationBase getRoomSimulation(T model) {
		Room room = ResourceUtils.getDeviceLocationRoom(model);
		if(room == null)
			return null;
		SingleRoomSimulationBaseImpl roomSim = roomSimulations.get(room.getLocation());
		if(roomSim != null)
			return roomSim;

		//TODO: Provide real implementation
		RoomSimConfigPatternI configPattern = new RoomSimConfigPatternI() {
			
			@Override
			public TemperatureResource simulatedTemperature() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public FloatResource simulatedHumidity() {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public IntegerResource personInRoomNonPersistent() {
				// TODO Auto-generated method stub
				return null;
			}
		};
		/*roomSim =  new SingleRoomSimulationBaseImpl(room, configPattern , appMan.getLogger()) {

			@Override
			public float getVolume() {
				return 0;
			}
			
		};
		roomSimulations.put(room.getLocation(), roomSim);
		return roomSim;*/
		return null;
	}
}


