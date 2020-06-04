package org.ogema.devicefinder.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import de.iwes.widgets.html.complextable.RowTemplate.Row;

public abstract class DeviceTableBase extends DeviceTableRaw<InstallAppDevice,InstallAppDevice>  {

	protected abstract Class<? extends Resource> getResourceType();
	
	protected final InstalledAppsSelector appSelector;
	
	public DeviceTableBase(WidgetPage<?> page, ApplicationManager appMan, Alert alert,
			InstalledAppsSelector appSelector) {
		super(page, appMan, alert, ResourceHelper.getSampleResource(InstallAppDevice.class));
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
		if(req == null)
			name = "initResName"; //ResourceHelper.getSampleResource(getResourceType());
		else
			name = getName(object);
		vh.stringLabel("Name", id, name, row);
		
		final Resource device;
		if(req == null)
			device = ResourceHelper.getSampleResource(getResourceType());
		else
			device = object.device();
		return device;
	}

	@Override
	public List<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
		List<InstallAppDevice> all = appSelector.getDevicesSelected();
		List<InstallAppDevice> result = new ArrayList<InstallAppDevice>();
		for(InstallAppDevice dev: all) {
			if(getResourceType().isAssignableFrom(dev.device().getResourceType())) {
				result.add(dev);
			}
		}
		return result;
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
}
