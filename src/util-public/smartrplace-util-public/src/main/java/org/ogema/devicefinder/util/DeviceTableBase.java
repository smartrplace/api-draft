package org.ogema.devicefinder.util;

import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.model.locations.Room;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;

public abstract class DeviceTableBase extends DeviceTableRaw<InstallAppDevice,InstallAppDevice>  {

	/** Required to generate sample resource*/
	protected abstract Class<? extends Resource> getResourceType();
	
	protected final InstalledAppsSelector appSelector;
	protected final DeviceHandlerBase<?> devHand;
	
	public DeviceTableBase(WidgetPage<?> page, ApplicationManagerPlus appMan, Alert alert,
			InstalledAppsSelector appSelector,
			DeviceHandlerBase<?> devHand) {
		super(page, appMan, alert, ResourceHelper.getSampleResource(InstallAppDevice.class));
		this.devHand = devHand;
		if(appSelector != null)
			this.appSelector = appSelector;
		else if(this instanceof InstalledAppsSelector)
			this.appSelector = (InstalledAppsSelector) this;
		else
			throw new IllegalStateException("InstalledAppsSelector must be provided or implemented!");
	}

	protected void addHomematicCCUId(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		if(req != null) {
			String text = getHomematicCCUId(object.device().getLocation());
			vh.stringLabel("RT", id, text, row);
		} else
			vh.registerHeaderEntry("RT");
	}
	
	public Resource addNameWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		
		final String name;
        final String deviceId;
		if(req == null) {
			name = "initResName"; //ResourceHelper.getSampleResource(getResourceType());
            deviceId = "initId";
        }
		else {
			name = getName(object, appManPlus);
            if (object.deviceId().exists())
                deviceId = object.deviceId().getValue();
            else
                deviceId = "n/a";
        }
		vh.stringLabel("Name", id, name, row);
		vh.stringLabel("ID", id, deviceId, row);
		
		final Resource device;
		if(req == null)
			device = ResourceHelper.getSampleResource(getResourceType());
		else
			device = object.device();
		return device;
	}

	@Override
	public List<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
		List<InstallAppDevice> all = appSelector.getDevicesSelected(devHand, req);
		return all;
		/*List<InstallAppDevice> result = new ArrayList<InstallAppDevice>();
		@SuppressWarnings({ "rawtypes", "unchecked" })
		List<ResourcePattern<?>> allPatterns = (List)devHand.getAllPatterns();
		for(InstallAppDevice dev: all) {
			for(ResourcePattern<?> pat: allPatterns) {
				if(pat.model.equalsLocation(dev.device())) {
					result.add(dev);
					break;
				}
			}
			//if(getResourceType().isAssignableFrom(dev.device().getResourceType())) {
			//	result.add(dev);
			//}
		}
		return result;*/
	}
	
	@Override
	public InstallAppDevice getResource(InstallAppDevice object, OgemaHttpRequest req) {
		return object;
	}
	
	public static String getHomematicCCUId(String location) {
		String[] parts = location.split("/");
		String tail;
		if(parts[0].toLowerCase().startsWith("homematicip")) {
			tail = parts[0].substring("homematicip".length());
		} else {
			if(!parts[0].startsWith("homematic")) return "n/a";
			tail = parts[0].substring("homematic".length());			
		}
		if(tail.isEmpty()) return "102";
		else return tail;
	}
	
	public DynamicTable<InstallAppDevice> getMainTable() {
		return mainTable;
	}
	
	public Header getHeaderWidget() {
		return headerWinSens;
	}
}
