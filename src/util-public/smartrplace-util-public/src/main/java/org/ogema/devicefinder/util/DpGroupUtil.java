package org.ogema.devicefinder.util;

import java.util.Collection;
import java.util.Collections;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.model.prototypes.PhysicalElement;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.resource.ResourceHelper;

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
	
	public static InstallAppDevice getInstallAppDevice(PhysicalElement devRes, ResourceAccess resAcc) {
		HardwareInstallConfig hwInstall = ResourceHelper.getTopLevelResource(HardwareInstallConfig.class, resAcc);
		if(hwInstall == null)
			return null;
		for(InstallAppDevice idev: hwInstall.knownDevices().getAllElements()) {
			if(idev.device().equalsLocation(devRes))
				return idev;
		}
		return null;
	}
	
	public static Collection<InstallAppDevice> managedDeviceResoures(String resourceClassName, DatapointService dpService) {
		Collection<Class<? extends Resource>> all = dpService.getManagedDeviceResoureceTypes();
		for(Class<? extends Resource> type: all) {
			if(type.getName().equals(resourceClassName) || type.getSimpleName().equals(resourceClassName)) {
				return dpService.managedDeviceResoures(type);
			}
		}
		return Collections.emptyList();
	}
}
