package org.smartrplace.util.virtualdevice;

import org.ogema.core.model.units.TemperatureResource;
import org.ogema.model.sensors.TemperatureSensor;
import org.smartrplace.util.virtualdevice.SetpointControlManager.RouterInstance;

public class SensorDataTemperatureDefault extends SensorDataTemperature {
	RouterInstance ccu = null;
	
	public SensorDataTemperatureDefault(TemperatureSensor sensor, SetpointControlManager<TemperatureResource> ctrl) {
		super(sensor, ctrl);
	}

	@Override
	public RouterInstance ccu() {
		if(ccu == null) {
			ccu = ctrl.getCCU(sensorRes);
			if(ccu == null) {
				System.out.println("WARNING: No Default Router found for setpoint sensor:"+sensorRes.getLocation());
			}
		}
		return ccu;
	}
}
