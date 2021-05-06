package org.smartrplace.util.virtualdevice;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.logconfig.LogHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;

public class HmCentralManager {
	public static final long CCU_UPDATE_INTERVAL = 10000;
	public static long DEFAULT_EVAL_INTERVAL = 5*TimeProcUtil.MINUTE_MILLIS;
	public static final String conditionalWritePerHour = "conditionalWritePerHour";
	public static final String totalWritePerHour = "totalWritePerHour";
	public static final String minimumAverageRequestDistance = "minimumAverageRequestDistance";
	
	public static final String paramMaxDutyCycle = "maxDutyCycle";
	public static final String paramMaxWritePerCCUperHour = "maxWritePerCCUperHour";
	
	//For now we cannot use HAP info as device - HAP relation is not easy to obtain. Also Controller relation is not used yet.
	public static class CCUInstance {
		int totalWriteCount = 0;
		int conditionalWriteCount = 0;
		//public CCUInstance mainCCUforHAP = null;
		InstallAppDevice device;
		public FloatResource dutyCycle = null;
		public FloatResource conditionalWritePerHour;
		public FloatResource totalWritePerHour;
		public FloatResource minimumAverageRequestDistance;
	}
	public static class SensorData {
		TemperatureSensor sensor;
		TemperatureResource setpoint;
		TemperatureResource feedback;
		CCUInstance ccu;
		ResourceValueListener<TemperatureResource> setpointListener;
		
		//boolean conditionalWriteDone = false;
		
		public SensorData(TemperatureSensor sensor, HmCentralManager ctrl) {
			sensor = sensor.getLocationResource();
			this.sensor = sensor;
			this.setpoint = sensor.settings().setpoint();
			this.feedback = sensor.deviceFeedback().setpoint();
			this.ccu = ctrl.getCCU(sensor);
			if(ccu == null) {
				System.out.println("WARNING: No CCU found for setpoint sensor:"+sensor.getLocation());
			}
			this.setpointListener = new ResourceValueListener<TemperatureResource>() {
				@Override
				public void resourceChanged(TemperatureResource resource) {
					if(ccu != null) {
						ctrl.reportSetpointRequest(ccu);
					}
				}
			};
			this.setpoint.addValueListener(setpointListener, true);
		}
	}
	protected final ApplicationManager appMan;
	protected final DatapointService dpService;
	protected final Map<String, SensorData> knownSensors = new HashMap<>();
	protected final Map<String, CCUInstance> knownCCUs = new HashMap<>();
	
	protected final FloatResource maxDutyCycle;
	protected final FloatResource maxWritePerCCUperHour;
	
	long nextEvalInterval;
	long intervalStart;
		
	private HmCentralManager(ApplicationManagerPlus appManPlus) {
		this.appMan = appManPlus.appMan();
		this.dpService = appManPlus.dpService();
		maxDutyCycle = ResourceHelper.getEvalCollection(appMan).getSubResource(paramMaxDutyCycle, FloatResource.class);
		maxWritePerCCUperHour = ResourceHelper.getEvalCollection(appMan).getSubResource(paramMaxWritePerCCUperHour, FloatResource.class);
		intervalStart = appMan.getFrameworkTime();
		nextEvalInterval = intervalStart + DEFAULT_EVAL_INTERVAL;
	}
	
	private static HmCentralManager instance = null;
	public static HmCentralManager getInstance(ApplicationManagerPlus appManPlus) {
		if(instance == null)
			instance = new HmCentralManager(appManPlus);
		return instance;
	}
	
	public SensorData registerSensor(TemperatureResource setp) {
		String loc = setp.getLocation();
		SensorData result = knownSensors.get(loc);
		if(result != null)
			return result;
		TemperatureSensor sensor = ResourceHelper.getFirstParentOfType(setp, TemperatureSensor.class);
		result = new SensorData(sensor, this);
		knownSensors.put(loc, result);
		return result;
	}
	
	//TODO: Implement prioritization
	public boolean requestSetpointWrite(TemperatureResource setp, float setpoint) {
		SensorData sens = registerSensor(setp);
		if(sens.ccu != null)
			sens.ccu.conditionalWriteCount++;
		boolean isOverload = isSensorInOverload(sens);
		if(!isOverload) {
			sens.setpoint.setValue(setpoint);
			return true;
		}
		return false;
	}
	
	public boolean isSensorInOverload(SensorData data) {
		if(data.ccu == null)
			return false;
		return isCCUInOverload(data.ccu);
	}
	
	//TODO: We need some averaging most likely
	public boolean isCCUInOverload(CCUInstance ccu) {
		if(ccu.dutyCycle != null) {
			float maxDC;
			if(maxDutyCycle != null)
				maxDC = maxDutyCycle.getValue();
			else
				maxDC = 0.97f;
			float curDC = ccu.dutyCycle.getValue();
			return (curDC > maxDC);
		}
		float maxWritePerHour;
		if(maxWritePerCCUperHour != null)
			maxWritePerHour = maxWritePerCCUperHour.getValue();
		else
			maxWritePerHour = 360;
		float total = ccu.totalWritePerHour.getValue();
		return (total > maxWritePerHour);
	}

	protected void stopInternal() {
		for(SensorData sens: knownSensors.values()) {
			if(sens.setpointListener != null && sens.setpoint != null) {
				sens.setpoint.removeValueListener(sens.setpointListener);
				sens.setpointListener = null;
			}
		}
	}
	
	public static boolean stop() {
		if(instance != null) {
			instance.stopInternal();
			instance = null;
			return true;
		}
		return false;
	}
	
	protected void reportSetpointRequest(CCUInstance ccu) {
		long now = appMan.getFrameworkTime();
		if(now > nextEvalInterval) {
			long deltaT = nextEvalInterval - now;
			if(ccu.totalWritePerHour != null) {
				ValueResourceHelper.setCreate(ccu.totalWritePerHour, (float) (((double)(ccu.totalWriteCount*TimeProcUtil.HOUR_MILLIS))/deltaT));
			}
			ccu.totalWriteCount = 0;
			if(ccu.conditionalWritePerHour!= null) {
				ValueResourceHelper.setCreate(ccu.conditionalWritePerHour, (float) (((double)(ccu.conditionalWriteCount*TimeProcUtil.HOUR_MILLIS))/deltaT));
			}
			ccu.conditionalWriteCount = 0;
			intervalStart = nextEvalInterval;
			nextEvalInterval += DEFAULT_EVAL_INTERVAL;
		}
		ccu.totalWriteCount++;
	}

	long lastCCUListUpdate = -1;
	protected void updateCCUList() {
		long now = appMan.getFrameworkTime();
		if(now - lastCCUListUpdate > CCU_UPDATE_INTERVAL) {
			updateCCUListForced();
			lastCCUListUpdate = now;
		}
	}
	protected void updateCCUListForced() {
		Collection<InstallAppDevice> ccus = ChartsUtil.getCCUs(dpService);
		for(InstallAppDevice ccu: ccus) {
			if(knownCCUs.containsKey(ccu.getLocation()))
				continue;
			CCUInstance cd = new CCUInstance();
			cd.device = ccu;
			PhysicalElement dev = ccu.device();
			cd.dutyCycle = dev.getSubResource("dutyCycle", GenericFloatSensor.class).reading();
			cd.conditionalWritePerHour = ccu.getSubResource(conditionalWritePerHour, FloatResource.class);
			cd.totalWritePerHour = ccu.getSubResource(totalWritePerHour, FloatResource.class);
			cd.minimumAverageRequestDistance = ccu.getSubResource(minimumAverageRequestDistance, FloatResource.class);
			knownCCUs.put(ccu.getLocation(), cd);
		}
	}
	
	public CCUInstance getCCU(TemperatureSensor sensor) {
		PhysicalElement dev = LogHelper.getDeviceResource(sensor, false);
		return getCCU(dev);
	}
	
	public CCUInstance getCCU(PhysicalElement device) {
		updateCCUList();
		for(CCUInstance ccu: knownCCUs.values()) {
			Resource parent = ccu.device.device().getLocationResource().getParent();
			if(parent == null)
				continue;
			if(device.getLocation().startsWith(parent.getLocation()))
				return ccu;
		}
		return null;
	}
}
