package org.smartrplace.util.virtualdevice;

import org.ogema.core.model.units.TemperatureResource;
import org.ogema.model.sensors.TemperatureSensor;
import org.smartrplace.util.virtualdevice.SetpointControlManager.RouterInstance;

public class SensorDataTemperatureDefault extends SensorDataTemperature {
	RouterInstance ccu;
	
	public SensorDataTemperatureDefault(TemperatureSensor sensor, SetpointControlManager<TemperatureResource> ctrl) {
		super(sensor, ctrl);
		this.ccu = ctrl.getCCU(sensor);
		if(ccu == null) {
			System.out.println("WARNING: No Default Router found for setpoint sensor:"+sensor.getLocation());
		}
	}

	@Override
	public RouterInstance ccu() {
		return ccu;
	}
}
