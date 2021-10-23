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
		public volatile float dutyCycleValueMax = 0;
	}
	
	public static class SensorDataHm extends SensorDataTemperature {
		CCUInstance ccu = null;
		
		public SensorDataHm(TemperatureSensor sensor, HmCentralManager ctrl) {
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
	
	/** Parameter defining alternative limit for conditional writes. Note very relevant most likely*/
	protected final FloatResource maxDutyCycleParam;
	
	/** Parameter overwriting DEFAULT_MAX_WRITE_PER_HOUR. This could be very relevant*/
	protected final FloatResource maxWritePerCCUperHourParam;
	
	private HmCentralManager(ApplicationManagerPlus appManPlus) {
		super(appManPlus);
		maxDutyCycleParam = ResourceHelper.getEvalCollection(appMan).getSubResource(paramMaxDutyCycle, FloatResource.class);
		maxWritePerCCUperHourParam = ResourceHelper.getEvalCollection(appMan).getSubResource(paramMaxWritePerCCUperHour, FloatResource.class);
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
	public Sensor getSensor(TemperatureResource setp) {
		TemperatureSensor sensor = ResourceHelper.getFirstParentOfType(setp, TemperatureSensor.class);
		return sensor;
	}
	
	@Override
	public SensorDataHm registerSensor(TemperatureResource setp, Sensor sensor,
			Map<String, SensorData> knownSensorsInner) {
		String loc = setp.getLocation();
		SensorDataHm result = (SensorDataHm) knownSensorsInner.get(loc);
		if(result != null)
			return result;
		result = new SensorDataHm((TemperatureSensor) sensor, this);
		knownSensorsInner.put(loc, result);
		return result;
	}
	
	@Override
	public boolean isSensorInOverload(SensorData data, float priority) {
		CCUInstance ccu = ((SensorDataHm)data).ccu;
		if(ccu == null)
			return false;
		return isRouterInOverload(ccu, priority);
	}
	
	//Float maxWritePerInterval = null;
	
	/** The current interval is limited based on the number of write operations. Also the dutyCycleMax is evaluated for the previous interval.
	 * Both must be ok to allow for writing
	 * 
	 */
	@Override
	public boolean isRouterInOverloadForced(RouterInstance router, float priority) {
		CCUInstance ccu = (CCUInstance) router;

		float maxDC;
		if(maxDutyCycleParam != null && maxDutyCycleParam.isActive() && priority <= CONDITIONAL_PRIO)
			maxDC = maxDutyCycleParam.getValue();
		else
			maxDC = priority; // 0.97f;

		float maxWritePerInterval;
		if(maxWritePerCCUperHourParam != null && maxWritePerCCUperHourParam.isActive()) {
			maxWritePerInterval = maxWritePerCCUperHourParam.getValue() * DEFAULT_EVAL_INTERVAL / TimeProcUtil.HOUR_MILLIS;
		} else
			maxWritePerInterval = DEFAULT_MAX_WRITE_PER_HOUR * DEFAULT_EVAL_INTERVAL / TimeProcUtil.HOUR_MILLIS;
		float relLoad = ccu.totalWriteCount / maxWritePerInterval;
		if(relLoad > ccu.relativeLoadMax)
			ccu.relativeLoadMax = relLoad;
		if(relLoad >= maxDC) {
			return true;
		}

		//Check also for current duty cycle
		//float curDC;
		if(ccu.dutyCycleValueMax > ccu.relativeLoadMax)
			ccu.relativeLoadMax = ccu.dutyCycleValueMax;
		if(ccu.dutyCycleValueMax >= maxDC) {
			return true;
		}
		
		float prevDC;
		if(ccu.dutyCycleMax != null && ccu.dutyCycleMax.isActive()) {
			prevDC = ccu.dutyCycleMax.getValue();
		//} else if(ccu.dutyCycle != null) {
			//usually max is available and this not used
		//	prevDC = Boolean.getBoolean("org.smartrplace.util.virtualdevice.dutycycle100")?(ccu.dutyCycle.getValue()*0.01f):ccu.dutyCycle.getValue();
		} else {
			//usually max is available and this not used
			if(maxWritePerCCUperHourParam != null && maxWritePerCCUperHourParam.isActive()) {
				prevDC = ccu.totalWritePerHour.getValue() / maxWritePerCCUperHourParam.getValue();
			} else {
				prevDC = ccu.totalWritePerHour.getValue() / DEFAULT_MAX_WRITE_PER_HOUR;
			}
		}
		if(prevDC > ccu.relativeLoadMax)
			ccu.relativeLoadMax = prevDC;
		return (prevDC > maxDC);
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
						float val = Boolean.getBoolean("org.smartrplace.util.virtualdevice.dutycycle100")?(cd.dutyCycle.getValue()*0.01f):cd.dutyCycle.getValue();
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
