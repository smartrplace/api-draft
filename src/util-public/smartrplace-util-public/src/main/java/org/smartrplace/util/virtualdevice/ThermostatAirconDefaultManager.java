package org.smartrplace.util.virtualdevice;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.model.sensors.Sensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.resource.ResourceHelper;

public class ThermostatAirconDefaultManager extends SetpointControlManager<TemperatureResource> {

	public static final String paramMaxWriteOnDefaultPerHour = "paramMaxWriteOnDefaultPerHour";
	private static final float DEFAULT_MAX_WRITE_PER_HOUR = 360;
	
	protected final FloatResource maxWriteOnDefaultPerHour;
	protected RouterInstance gateway = null;
	
	private ThermostatAirconDefaultManager(ApplicationManagerPlus appManPlus) {
		super(appManPlus);
		maxWriteOnDefaultPerHour = ResourceHelper.getEvalCollection(appMan).getSubResource(paramMaxWriteOnDefaultPerHour, FloatResource.class);
	}
	
	private static ThermostatAirconDefaultManager instance = null;
	public static ThermostatAirconDefaultManager getInstance(ApplicationManagerPlus appManPlus) {
		if(instance == null)
			instance = new ThermostatAirconDefaultManager(appManPlus);
		return instance;
	}
	
	public static boolean stop() {
		if(instance != null) {
			instance.stopInternal();
			instance = null;
			return true;
		}
		return false;
	}
	
	@Override
	public SensorDataTemperatureDefault registerSensor(TemperatureResource setp) {
		String loc = setp.getLocation();
		SensorDataTemperatureDefault result = (SensorDataTemperatureDefault) knownSensors.get(loc);
		if(result != null)
			return result;
		TemperatureSensor sensor = ResourceHelper.getFirstParentOfType(setp, TemperatureSensor.class);
		result = new SensorDataTemperatureDefault(sensor, this);
		knownSensors.put(loc, result);
		return result;
	}
	
	@Override
	public boolean isSensorInOverload(SensorData data, int priority) {
		RouterInstance ccu = data.ccu();
		if(ccu == null)
			return false;
		return isRouterInOverload(ccu, priority);
	}
	
	Float maxWritePerInterval = null;

	//TODO: We need some averaging most likely
	@Override
	public boolean isRouterInOverload(RouterInstance ccu, int priority) {
		if(maxWritePerInterval == null) {
			if(maxWriteOnDefaultPerHour != null) {
				maxWritePerInterval =  maxWriteOnDefaultPerHour.getValue() * DEFAULT_EVAL_INTERVAL / TimeProcUtil.HOUR_MILLIS;
			} else
				maxWritePerInterval = DEFAULT_MAX_WRITE_PER_HOUR * DEFAULT_EVAL_INTERVAL / TimeProcUtil.HOUR_MILLIS;
		}
		if(ccu.totalWriteCount > maxWritePerInterval)
			return false;

		float curDC;
		float maxDC = priority;
		if(maxWriteOnDefaultPerHour != null) {
			curDC = ccu.totalWritePerHour.getValue() / maxWriteOnDefaultPerHour.getValue();
		} else {
				curDC = ccu.totalWritePerHour.getValue() / DEFAULT_MAX_WRITE_PER_HOUR;
		}
		return (curDC > maxDC);
	}

	@Override
	protected void updateCCUListForced() {
		if(gateway != null)
			return;
		InstallAppDevice iad = ChartsUtil.getGateway(dpService);
		gateway = new RouterInstance();
		initRouterStandardValues(gateway, iad);
	}
	
	@Override
	protected SensorData getSensorDataInstance(Sensor sensor) {
		return new SensorDataTemperatureDefault((TemperatureSensor) sensor, this);
	}

	@Override
	public RouterInstance getCCU(Sensor sensor) {
		return gateway;
	}

	@Override
	protected Collection<RouterInstance> getRouters() {
		if(gateway == null)
			return Collections.emptyList();
		return Arrays.asList(new RouterInstance[] {gateway});
	}
}
