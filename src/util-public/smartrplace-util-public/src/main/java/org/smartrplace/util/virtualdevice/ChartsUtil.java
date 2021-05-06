package org.smartrplace.util.virtualdevice;

import java.util.ArrayList;
import java.util.Collection;

import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

public class ChartsUtil {
	public static Collection<InstallAppDevice> getCCUs(DatapointService dpService) {
		Collection<InstallAppDevice> result = DpGroupUtil.managedDeviceResoures("HmInterfaceInfo", dpService);
		return result ;
	}
	
	public static Collection<InstallAppDevice> getHAPs(DatapointService dpService) {
		Collection<InstallAppDevice> result = new ArrayList<>();
		Collection<InstallAppDevice> senss = dpService.managedDeviceResoures(SensorDevice.class);
		for(InstallAppDevice sens: senss) {
			if(DeviceTableRaw.isHAPDevice(sens.device().getLocation(), null))
				result.add(sens);
		}
		return result ;
	}
	
	public static Collection<InstallAppDevice> getControllers(DatapointService dpService) {
		Collection<InstallAppDevice> result = DpGroupUtil.managedDeviceResoures("org.smartrplace.router.model.GlitNetRouter", dpService);
		return result ;
	}
}
