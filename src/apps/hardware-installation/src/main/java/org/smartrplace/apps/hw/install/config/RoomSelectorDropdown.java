package org.smartrplace.apps.hw.install.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.HardwareInstallController;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.template.DefaultDisplayTemplate;

public class RoomSelectorDropdown extends TemplateDropdown<String> {
	private static final long serialVersionUID = 1L;
	private final ResourceAccess resAcc;
	private final HardwareInstallController controller;
	private final Map<String, Room> knownRooms = new HashMap<String, Room>();
	
	public RoomSelectorDropdown(WidgetPage<?> page, String id, HardwareInstallController controller) {
		super(page, id);
		this.controller = controller;
		this.resAcc = controller.appMan.getResourceAccess();
		setTemplate(new DefaultDisplayTemplate<String>() {
			@Override
			public String getLabel(String arg0, OgemaLocale arg1) {
				if(arg0.equals("allDevices"))
					return "All Devices";
				if(arg0.equals("devicesInRooms"))
					return "Devices configured for a room";
				if(arg0.equals("devicesNOTInRooms"))
					return "Devices NOT configured for a room";
				Room room = knownRooms.get(arg0); 
				//ResourceHelperSP.getSubResource(null, arg0, Room.class, resAcc);
				if(room == null) return ("unknown:arg0");
				return ResourceUtils.getHumanReadableShortName(room);
			}
		});
	}

	@Override
	public void onGET(OgemaHttpRequest req) {
		List<Room> rooms = resAcc.getResources(Room.class);
		List<String> items = new ArrayList<>();
		items.add("allDevices");
		items.add("devicesInRooms");
		items.add("devicesNOTInRooms");
		for(Room room: rooms) {
			addItem(room, items);
		}
		update(items, req);
		selectItem(controller.appConfigData.room().getValue(), req);
	}
	
	@Override
	public void onPOSTComplete(String data, OgemaHttpRequest req) {
		String item = getSelectedItem(req);
		controller.appConfigData.room().setValue(item);
	}

	protected void addItem(Room room, List<String> items) {
		knownRooms.put(room.getLocation(), room);
		items.add(room.getLocation());
	}
	
	public List<InstallAppDevice> getDevicesSelected() {
		List<InstallAppDevice> devicesSelected = new ArrayList<>();
		String arg0 = controller.appConfigData.room().getValue();
		for(InstallAppDevice dev: controller.appConfigData.knownDevices().getAllElements()) {
			if(arg0.equals("allDevices"))
				devicesSelected.add(dev);
			else if(arg0.equals("devicesInRooms"))
				if(dev.device().location().room().exists())
					devicesSelected.add(dev);
			else if(arg0.equals("devicesNOTInRooms"))
				if(!dev.device().location().room().exists())
					devicesSelected.add(dev);
			else {
				if(dev.device().location().room().getLocation().equals(arg0))
					devicesSelected.add(dev);
			}
		}
		return devicesSelected;
	}
}
