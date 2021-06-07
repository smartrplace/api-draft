package org.ogema.devicefinder.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.resource.ResourceHelper;

public class DpGroupUtil {
	public static final long IAD_CASH_TIME = 5*TimeProcUtil.MINUTE_MILLIS;
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
	
	public static class CashedIAD {
		public long lastAppDevCashed;
		public InstallAppDevice iad;
	}
	public static Map<String, CashedIAD> cashedIAD = new HashMap<>();
	public static InstallAppDevice getInstallAppDeviceForSubCashed(Resource res, ApplicationManager appMan) {
		String loc = res.getLocation();
		CashedIAD result = cashedIAD.get(loc);
		long now = appMan.getFrameworkTime();
		if(result == null) {
			result = new CashedIAD();
			cashedIAD.put(loc, result);
		} else if(now - result.lastAppDevCashed < IAD_CASH_TIME) {
			return result.iad;		
		}
		result.lastAppDevCashed = now;
		HardwareInstallConfig hwInstall = ResourceHelper.getTopLevelResource(HardwareInstallConfig.class, appMan.getResourceAccess());
		if(hwInstall == null)
			return null;
		for(InstallAppDevice idev: hwInstall.knownDevices().getAllElements()) {
			if(loc.startsWith(idev.device().getLocation())) {
				result.iad = idev;
				return idev;
			}
		}
		result.iad = null;
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
