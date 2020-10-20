package org.ogema.devicefinder.util;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class DpGroupUtil {
	public static DatapointGroup getDeviceGroup(String devLoc, DatapointService dpService, boolean createIfNotExisting) {
		if(!createIfNotExisting && (!dpService.hasGroup(devLoc)))
				return null;
		DatapointGroup dpGrp = dpService.getGroup(devLoc);
		if(dpGrp.getType() != null && dpGrp.getType().equals(DatapointGroup.DEVICE))
			return dpGrp;
		if(!createIfNotExisting)
			return null;
		dpGrp.setType(DatapointGroup.DEVICE);
		return dpGrp;
	}
	
	public static DatapointGroup getDeviceTypeGroup(InstallAppDevice iad, ApplicationManagerPlus appManPlus) {
		String devLoc = iad.device().getLocation();
		return getDeviceTypeGroup(devLoc, appManPlus.dpService());
	}

	public static DatapointGroup getDeviceTypeGroup(String devLoc, DatapointService dpService) {
		for(DatapointGroup dpGrp: dpService.getAllGroups()) {
			if(dpGrp.getType() != null && dpGrp.getType().equals(DatapointGroup.DEVICE_TYPE) && (dpGrp.getSubGroup(devLoc) != null)) {
				return dpGrp;
			}
		}
		return null;
	}
	
	public static DatapointGroup getDeviceTypeGroup(DeviceHandlerProvider<?> devHand, DatapointService dpService,
			boolean createIfNotExisting) {
		String id = devHand.id();
		if(!createIfNotExisting && (!dpService.hasGroup(id)))
			return null;
		DatapointGroup dpGrp = dpService.getGroup(id);
		if(dpGrp.getType() != null && dpGrp.getType().equals(DatapointGroup.DEVICE_TYPE))
			return dpGrp;
		if(!createIfNotExisting)
			return null;
		dpGrp.setType(DatapointGroup.DEVICE_TYPE);
		dpGrp.setLabel(null, devHand.label(null));
		return dpGrp;
	}
}
