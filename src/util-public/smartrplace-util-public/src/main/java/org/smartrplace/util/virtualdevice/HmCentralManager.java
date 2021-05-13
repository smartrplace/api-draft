package org.smartrplace.util.virtualdevice;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.model.sensors.Sensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.logconfig.LogHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;

public class HmCentralManager extends SetpointControlManager<TemperatureResource> {
	private static final float DEFAULT_MAX_WRITE_PER_HOUR = 720;

	public static final String paramMaxDutyCycle = "maxDutyCycle";
	public static final String paramMaxWritePerCCUperHour = "maxWritePerCCUperHour";
	public static final String dutyCycleMax = "dutyCycleMax";
	
	protected final Map<String, CCUInstance> knownCCUs = new HashMap<>();

	//For now we cannot use HAP info as device - HAP relation is not easy to obtain. Also Controller relation is not used yet.
	public static class CCUInstance extends RouterInstance {
		public FloatResource dutyCycle = null;
		public FloatResource dutyCycleMax = null;
		
		public ResourceValueListener<FloatResource> dutyCycleListener;
		public float dutyCycleValueMax = 0;
	}
	
	public static class SensorDataHm extends SensorDataTemperature {
		CCUInstance ccu = null;
		
		public SensorDataHm(TemperatureSensor sensor, HmCentralManager ctrl) {
			super(sensor, ctrl);
		}

		@Override
		public RouterInstance ccu() {
			if(ccu == null) {
				ccu = (CCUInstance) ctrl.getCCU(sensor);
				if(ccu == null) {
					System.out.println("WARNING: No CCU found for setpoint sensor:"+sensor.getLocation());
				}
			}
			return ccu;
		}
	}
	
	protected final FloatResource maxDutyCycle;
	protected final FloatResource maxWritePerCCUperHour;
	
	private HmCentralManager(ApplicationManagerPlus appManPlus) {
		super(appManPlus);
		maxDutyCycle = ResourceHelper.getEvalCollection(appMan).getSubResource(paramMaxDutyCycle, FloatResource.class);
		maxWritePerCCUperHour = ResourceHelper.getEvalCollection(appMan).getSubResource(paramMaxWritePerCCUperHour, FloatResource.class);
	}
	
	private static HmCentralManager instance = null;
	public static HmCentralManager getInstance(ApplicationManagerPlus appManPlus) {
		if(instance == null)
			instance = new HmCentralManager(appManPlus);
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
	public SensorDataHm registerSensor(TemperatureResource setp) {
		String loc = setp.getLocation();
		SensorDataHm result = (SensorDataHm) knownSensors.get(loc);
		if(result != null)
			return result;
		TemperatureSensor sensor = ResourceHelper.getFirstParentOfType(setp, TemperatureSensor.class);
		result = new SensorDataHm(sensor, this);
		knownSensors.put(loc, result);
		return result;
	}
	
	@Override
	public boolean isSensorInOverload(SensorData data, int priority) {
		CCUInstance ccu = ((SensorDataHm)data).ccu;
		if(ccu == null)
			return false;
		return isRouterInOverload(ccu, priority);
	}
	
	Float maxWritePerInterval = null;
	
	//TODO: We need some averaging most likely
	@Override
	public boolean isRouterInOverload(RouterInstance router, int priority) {
		CCUInstance ccu = (CCUInstance) router;
		if(maxWritePerInterval == null) {
			if(maxWritePerCCUperHour != null) {
				maxWritePerInterval = maxWritePerCCUperHour.getValue() * DEFAULT_EVAL_INTERVAL / TimeProcUtil.HOUR_MILLIS;
			} else
				maxWritePerInterval = DEFAULT_MAX_WRITE_PER_HOUR * DEFAULT_EVAL_INTERVAL / TimeProcUtil.HOUR_MILLIS;
		}
		if(ccu.totalWriteCount > maxWritePerInterval)
			return false;
		
		float curDC;
		float maxDC;
		if(maxDutyCycle != null && priority <= CONDITIONAL_PRIO)
			maxDC = maxDutyCycle.getValue();
		else
			maxDC = priority; // 0.97f;
		if(ccu.dutyCycleMax != null) {
			curDC = ccu.dutyCycleMax.getValue();
		} else if(ccu.dutyCycle != null) {
			curDC = ccu.dutyCycle.getValue();
		} else {
			//float maxWritePerHour;
			if(maxWritePerCCUperHour != null) {
				curDC = ccu.totalWritePerHour.getValue() / maxWritePerCCUperHour.getValue();
			} else {
				curDC = ccu.totalWritePerHour.getValue() / DEFAULT_MAX_WRITE_PER_HOUR;
			}
		}
		return (curDC > maxDC);

	}

	@Override
	protected void updateCCUListForced() {
		Collection<InstallAppDevice> ccus = ChartsUtil.getCCUs(dpService);
		for(InstallAppDevice ccu: ccus) {
			if(knownCCUs.containsKey(ccu.getLocation()))
				continue;
			CCUInstance cd = new CCUInstance();
			initRouterStandardValues(cd, ccu);
			knownCCUs.put(ccu.getLocation(), cd);
			
			//application specific
			PhysicalElement dev = ccu.device();
			cd.dutyCycle = dev.getSubResource("dutyCycle", GenericFloatSensor.class).reading();
			if(cd.dutyCycle != null && cd.dutyCycle.isActive()) {
				cd.dutyCycleMax = ccu.getSubResource(dutyCycleMax, FloatResource.class);
				cd.dutyCycleListener = new ResourceValueListener<FloatResource>() {
	
					@Override
					public void resourceChanged(FloatResource resource) {
						float val = cd.dutyCycle.getValue();
						if(val > cd.dutyCycleValueMax)
							cd.dutyCycleValueMax = val;
					}
				};
				cd.dutyCycle.addValueListener(cd.dutyCycleListener);
			}
		}
	}
	
	public RouterInstance getCCU(TemperatureResource setpoint) {
		PhysicalElement dev = LogHelper.getDeviceResource(setpoint, false);
		return getCCU(dev);
	}

	public RouterInstance getCCU(Sensor sensor) {
		PhysicalElement dev = LogHelper.getDeviceResource(sensor, false);
		return getCCU(dev);
	}
	
	public RouterInstance getCCU(PhysicalElement device) {
		updateCCUList();
		for(RouterInstance ccu: knownCCUs.values()) {
			Resource parent = ccu.device.device().getLocationResource().getParent();
			if(parent == null)
				continue;
			if(device.getLocation().startsWith(parent.getLocation()))
				return ccu;
		}
		return null;
	}

	@Override
	protected SensorData getSensorDataInstance(Sensor sensor) {
		return new SensorDataHm((TemperatureSensor) sensor, this);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected Collection<RouterInstance> getRouters() {
		updateCCUList();
		return (Collection)knownCCUs.values();
	}
	
	@Override
	protected void writeDataReporting(RouterInstance ccu, long deltaT) {
		super.writeDataReporting(ccu, deltaT);
		
		CCUInstance ccuMy = (CCUInstance)ccu;
		
		if(ccuMy.dutyCycleMax != null) {
			ValueResourceHelper.setCreate(ccuMy.dutyCycleMax, ccuMy.dutyCycleValueMax);
		}
		ccuMy.dutyCycleValueMax = 0;		
	}
	
	@Override
	public long knownSetpointValueOmitDuration(TemperatureResource temperatureSetpoint) {
		RouterInstance ccu = getCCU(temperatureSetpoint);
		if(ccu != null && isRouterInOverload(ccu, CONDITIONAL_PRIO)) {
			return 5*TimeProcUtil.MINUTE_MILLIS;
		}
		return super.knownSetpointValueOmitDuration(temperatureSetpoint);
	}
}
