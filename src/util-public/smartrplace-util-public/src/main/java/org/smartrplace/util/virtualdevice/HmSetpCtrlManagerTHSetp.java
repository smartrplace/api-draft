package org.smartrplace.util.virtualdevice;

import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.Resource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.model.sensors.Sensor;
import org.ogema.model.sensors.TemperatureSensor;

import de.iwes.util.resource.ResourceHelper;

public class HmSetpCtrlManagerTHSetp extends HmSetpCtrlManager<TemperatureResource> {
	public static class SensorDataHmTemperature extends SensorDataTemperature {
		CCUInstance ccu = null;
		
		public SensorDataHmTemperature(TemperatureSensor sensor, HmSetpCtrlManagerTHSetp ctrl) {
			super(sensor, ctrl);
		}

		@Override
		public RouterInstance ccu() {
			if(ccu == null) {
				ccu = (CCUInstance) ctrl.getCCU(sensorRes);
				if(ccu == null) {
					System.out.println("WARNING: No CCU found for setpoint sensor:"+sensorRes.getLocation());
				}
			}
			return ccu;
		}
	}
	
	private HmSetpCtrlManagerTHSetp(ApplicationManagerPlus appManPlus) {
		super(appManPlus, PENDING_TimeForMissingFeedback_DEFAULT);
	}
	
	private static HmSetpCtrlManagerTHSetp instance = null;
	public static HmSetpCtrlManagerTHSetp getInstance(ApplicationManagerPlus appManPlus) {
		if(instance == null)
			instance = new HmSetpCtrlManagerTHSetp(appManPlus);
		return instance;
	}
	
	/** Additional instances*/
	
	
	public static boolean stop() {
		if(instance != null) {
			instance.stopInternal();
			instance = null;
			return true;
		}
		return false;
	}
	
	@Override
	public Sensor getSensor(TemperatureResource setp) {
		TemperatureSensor sensor = ResourceHelper.getFirstParentOfType(setp, TemperatureSensor.class);
		return sensor;
	}
	
	@Override
	public SensorDataHmTemperature registerSensor(TemperatureResource setp, Resource sensor,
			Map<String, SensorData> knownSensorsInner) {
		String loc = setp.getLocation();
		SensorDataHmTemperature result = (SensorDataHmTemperature) knownSensorsInner.get(loc);
		if(result != null)
			return result;
		result = new SensorDataHmTemperature((TemperatureSensor) sensor, this);
		knownSensorsInner.put(loc, result);
		return result;
	}
	
	@Override
	public boolean isSensorInOverload(SensorData data, float maxDC) {
		CCUInstance ccu = ((SensorDataHmTemperature)data).ccu;
		if(ccu == null)
			return false;
		return isRouterInOverload(ccu, maxDC);
	}
	
	/*@Override
	protected SensorData getSensorDataInstance(Resource sensor) {
		return new SensorDataHmTemperature((TemperatureSensor) sensor, this);
	}*/
}