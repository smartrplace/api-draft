package org.smartrplace.util.virtualdevice;

import java.util.ArrayList;
import java.util.Collection;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.sensors.CO2Sensor;
import org.ogema.model.sensors.GenericFloatSensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gateway.device.GatewayDevice;
import org.smartrplace.gateway.device.KnownIssueDataGw;
import org.smartrplace.gateway.device.MemoryTimeseriesPST;

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
	
	public static InstallAppDevice getGateway(DatapointService dpService) {
		Collection<InstallAppDevice> result = dpService.managedDeviceResoures(GatewayDevice.class);
		if(result.size() > 1)
			throw new IllegalStateException("Found more than one gateway in InstalledAppDevices:"+result.size());
		if(result.isEmpty())
			return null;
		return result.iterator().next();
	}

	public static InstallAppDevice getJobSupervisionPST(DatapointService dpService) {
		Collection<InstallAppDevice> result = dpService.managedDeviceResoures(MemoryTimeseriesPST.class);
		if(result.size() > 2)
			throw new IllegalStateException("Found more than one resource of type MemoryTimeseriesPST in InstalledAppDevices:"+result.size());
		if(result.size() == 2) {
			for(InstallAppDevice iad: result) {
				Resource parent = iad.device().getLocationResource().getParent();
				if(parent == null || (!(parent instanceof KnownIssueDataGw)))
					return iad;
			}
			return null;
		}
		if(result.isEmpty())
			return null;
		return result.iterator().next();
	}
	
	public static Collection<FloatResource> getCO2Sensors(DatapointService dpService) {
		Collection<FloatResource> result = new ArrayList<>();
		Collection<InstallAppDevice> senss = dpService.managedDeviceResoures(SensorDevice.class);
		for(InstallAppDevice sens: senss) {
			if(DeviceTableRaw.isSmartProtectDevice(sens.device().getLocation()))
				result.add(((SensorDevice)sens.device()).sensors().getSubResource("co2", CO2Sensor.class).reading());
			if(DeviceTableRaw.isCO2wMBUSDevice(sens.device().getLocation(), DeviceTableRaw.getSubResInfo(sens.device())))
				result.add(sens.device().getSubResource("USER_DEFINED_0_0", GenericFloatSensor.class).reading());
		}
		Collection<InstallAppDevice> hms = dpService.managedDeviceResoures(CO2Sensor.class);
		for(InstallAppDevice sens: hms) {
			result.add(((CO2Sensor)sens.device()).reading());
		}
		return result ;
	}
}
