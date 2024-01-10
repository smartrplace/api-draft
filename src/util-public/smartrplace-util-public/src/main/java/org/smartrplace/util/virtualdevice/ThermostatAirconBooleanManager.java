package org.smartrplace.util.virtualdevice;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.model.sensors.Sensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.resource.ResourceHelper;

public class ThermostatAirconBooleanManager extends SetpointControlManager<BooleanResource> {

	public static final String paramMaxWriteOnDefaultPerHour = "paramMaxWriteOnDefaultPerHour";
	private static final float DEFAULT_MAX_WRITE_PER_HOUR = 360;
	
	/** Parameter overwriting DEFAULT_MAX_WRITE_PER_HOUR. This could be very relevant*/
	protected final FloatResource maxWriteOnDefaultPerHour;
	protected RouterInstance gateway = null;
	
	private ThermostatAirconBooleanManager(ApplicationManagerPlus appManPlus) {
		super(appManPlus);
		maxWriteOnDefaultPerHour = ResourceHelper.getEvalCollection(appMan).getSubResource(paramMaxWriteOnDefaultPerHour, FloatResource.class);
	}
	
	private static ThermostatAirconBooleanManager instance = null;
	public static ThermostatAirconBooleanManager getInstance(ApplicationManagerPlus appManPlus) {
		if(instance == null)
			instance = new ThermostatAirconBooleanManager(appManPlus);
		instance.setHeatcontrolLogger(appManPlus.appMan());
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
	public Resource getSensor(BooleanResource setp) {
		Resource sensor = setp.getParent(); //ResourceHelper.getFirstParentOfType(setp, TemperatureSensor.class);
		return sensor;
	}
	
	@Override
	public SensorDataBool2Bool registerSensor(BooleanResource setp, Resource sensor,
			Map<String, SensorData> knownSensorsInner) {
		String loc = setp.getLocation();
		SensorDataBool2Bool result = (SensorDataBool2Bool) knownSensorsInner.get(loc);
		if(result != null)
			return result;
		log.debug("Create and register SensorDataTemperatureDefault (Aircon) for "+loc);
		result = new SensorDataBool2Bool(sensor, setp, this);
		knownSensorsInner.put(loc, result);
		return result;
	}
	
	@Override
	public boolean isSensorInOverload(SensorData data, float maxDC) {
		RouterInstance ccu = data.ccu();
		if(ccu == null)
			return false;
		return isRouterInOverload(ccu, maxDC);
	}
	
	//Float maxWritePerInterval = null;

	@Override
	/** Simplified implementation for air conditioners*/
	protected boolean isRouterInOverloadForced(RouterInstance ccu, float maxDC) {
		float maxWritePerInterval;
		if(maxWriteOnDefaultPerHour != null && maxWriteOnDefaultPerHour.isActive()) {
			maxWritePerInterval =  maxWriteOnDefaultPerHour.getValue() * DEFAULT_EVAL_INTERVAL / TimeProcUtil.HOUR_MILLIS;
		} else
			maxWritePerInterval = DEFAULT_MAX_WRITE_PER_HOUR * DEFAULT_EVAL_INTERVAL / TimeProcUtil.HOUR_MILLIS;
		if(ccu.totalWriteCount > maxWritePerInterval)
			return true;

		float curDC;
		if(maxWriteOnDefaultPerHour != null && maxWriteOnDefaultPerHour.isActive()) {
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
	
	/*@Override
	protected SensorData getSensorDataInstance(Resource sensor) {
		return new SensorDataTemperatureDefault((TemperatureSensor) sensor, this);
	}*/

	@Override
	public RouterInstance getCCU(Resource sensor) {
		return gateway;
	}

	@Override
	protected Collection<RouterInstance> getRouters() {
		if(gateway == null)
			return Collections.emptyList();
		return Arrays.asList(new RouterInstance[] {gateway});
	}
}
