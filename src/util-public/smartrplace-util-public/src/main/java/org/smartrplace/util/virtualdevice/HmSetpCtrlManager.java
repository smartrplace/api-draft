package org.smartrplace.util.virtualdevice;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.units.PercentageResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.logconfig.LogHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;

public abstract class HmSetpCtrlManager<T extends ValueResource> extends SetpointControlManager<T> {
	public static final float DEFAULT_MAX_WRITE_PER_HOUR = 720;

	//public static final String paramMaxDutyCycle = "maxDutyCycle";
	public static final String paramMaxWritePerCCUperHour = "maxWritePerCCUperHour";
	public static final String dutyCycleMax = "dutyCycleMax";
	
	public enum WritePrioLevel {
		MUST_WRITE,
		CONDITIONAL,
		PRIORITY
	}
	public static final String dutyCycleYellowMin = "dutyCycleYellowMin";
	public static final String dutyCycleRedMin = "dutyCycleRedMin";

	private final Map<String, CCUInstance> knownCCUsInternal = new HashMap<>();
	/** CCU location -> CCUInstnace */
	protected Map<String, CCUInstance> knownCCUs() {
		return knownCCUsInternal;
	}

	//For now we cannot use HAP info as device - HAP relation is not easy to obtain. Also Controller relation is not used yet.
	public static class CCUInstance extends RouterInstance {
		public FloatResource dutyCycle = null;
		/** Maximum for each reporting period (default: approx. 5 minutes)
		 * Logged in dutyCycleEff*/
		public FloatResource dutyCycleMax = null;
		
		public PercentageResource dutyCycleWarningYello;
		public PercentageResource dutyCycleWarningRed;
		@Override
		float dutyCycleWarningYellow() {
			if(dutyCycleWarningYello == null)
				return super.dutyCycleWarningYellow();
			return dutyCycleWarningYello.getValue();
		}
		@Override
		float dutyCycleWarningRed() {
			if(dutyCycleWarningRed == null)
				return super.dutyCycleWarningRed();
			return dutyCycleWarningRed.getValue();
		}
		
		public ResourceValueListener<FloatResource> dutyCycleListener;
		public volatile float dutyCycleValueMax = 0;
		public volatile float dutyCycleValueMaxNew = 0;
		
		public CCUInstance() {};
	}
	
	/** Parameter defining alternative limit for conditional writes. Note very relevant most likely*/
	//protected final FloatResource maxDutyCycleParam;
	
	/** Parameter overwriting DEFAULT_MAX_WRITE_PER_HOUR. This could be very relevant*/
	protected final FloatResource maxWritePerCCUperHourParam;
	
	protected HmSetpCtrlManager(ApplicationManagerPlus appManPlus, long pendingTimeForRetry) {
		super(appManPlus, pendingTimeForRetry);
		//maxDutyCycleParam = ResourceHelper.getEvalCollection(appMan).getSubResource(paramMaxDutyCycle, FloatResource.class);
		maxWritePerCCUperHourParam = ResourceHelper.getEvalCollection(appMan).getSubResource(paramMaxWritePerCCUperHour, FloatResource.class);
	}
	
	@Override
	public abstract boolean isSensorInOverload(SensorData data, float maxDC);
	
	/** The current interval is limited based on the number of write operations. Also the dutyCycleMax is evaluated for the previous interval.
	 * Both must be ok to allow for writing
	 * 
	 */
	@Override
	public boolean isRouterInOverloadForced(RouterInstance router, float maxDC) {
		CCUInstance ccu = (CCUInstance) router;

		/*float maxDC;
		if(maxDutyCycleParam != null && maxDutyCycleParam.isActive() && priority <= ccu.dutyCycleWarningYello.getValue())
			maxDC = maxDutyCycleParam.getValue();
		else
			maxDC = priority; // 0.97f;*/

		if(maxDC <= 1.0f) {
			//For MUST_WRITE we do not test this
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
			//usually max is available and this not used, so for MUST_WRITE the condition below should always be false
			if(maxWritePerCCUperHourParam != null && maxWritePerCCUperHourParam.isActive()) {
				prevDC = ccu.totalWritePerHour.getValue() / maxWritePerCCUperHourParam.getValue();
			} else {
				prevDC = ccu.totalWritePerHour.getValue() / DEFAULT_MAX_WRITE_PER_HOUR;
			}
		}
		if(prevDC > ccu.relativeLoadMax)
			ccu.relativeLoadMax = prevDC;
if(prevDC > maxDC)
System.out.println("Overload in "+(ccu.device!=null?ccu.device.deviceId().getValue():"??"));	
		return (prevDC > maxDC);
	}

	@Override
	protected void updateCCUListForced() {
		Collection<InstallAppDevice> ccus = ChartsUtil.getCCUs(dpService);
		Map<String, CCUInstance> knownCCUs = knownCCUs();
		for(InstallAppDevice ccu: ccus) {
			if(knownCCUs.containsKey(ccu.getLocation()))
				continue;
			CCUInstance cd = new CCUInstance();
			initRouterStandardValues(cd, ccu);
			knownCCUs().put(ccu.getLocation(), cd);
			
			//application specific
			PhysicalElement dev = ccu.device();
			cd.dutyCycle = dev.getSubResource("dutyCycle", GenericFloatSensor.class).reading();
			if(cd.dutyCycle != null && cd.dutyCycle.isActive()) {
				cd.dutyCycleMax = ccu.getSubResource(dutyCycleMax, FloatResource.class);
				cd.dutyCycleWarningYello = ccu.getSubResource(dutyCycleYellowMin, PercentageResource.class);
				if(ValueResourceHelper.setIfNew(cd.dutyCycleWarningYello, CONDITIONAL_PRIO_DEFAULT))
					cd.dutyCycleWarningYello.activate(false);
				cd.dutyCycleWarningRed = ccu.getSubResource(dutyCycleRedMin, PercentageResource.class);
				if(ValueResourceHelper.setIfNew(cd.dutyCycleWarningRed, PRIORITY_PRIO_DEFAULT))
					cd.dutyCycleWarningRed.activate(false);
				cd.dutyCycleListener = new ResourceValueListener<FloatResource>() {
	
					@Override
					public void resourceChanged(FloatResource resource) {
						if(!cd.device.isActive())
							return;
						float val = Boolean.getBoolean("org.smartrplace.util.virtualdevice.dutycycle100")?(cd.dutyCycle.getValue()*0.01f):cd.dutyCycle.getValue();
						if(val > cd.dutyCycleValueMaxNew)
							cd.dutyCycleValueMaxNew = val;
						if(val > cd.dutyCycleValueMax) {
							cd.dutyCycleValueMax = val;
							if(cd.dutyCycleMax != null)
								ValueResourceHelper.setCreate(cd.dutyCycleMax, cd.dutyCycleValueMax);							
						}
					}
				};
				cd.dutyCycle.addValueListener(cd.dutyCycleListener, true);
			}
		}
	}
	
	public RouterInstance getCCU(T setpoint) {
		PhysicalElement dev = LogHelper.getDeviceResource(setpoint, false);
		return getCCU(dev);
	}

	@Override
	public RouterInstance getCCU(Resource sensor) {
		PhysicalElement dev = LogHelper.getDeviceResource(sensor, false);
		return getCCU(dev);
	}
	
	public RouterInstance getCCU(PhysicalElement device) {
		updateCCUList();
		Map<String, CCUInstance> kccus = knownCCUs();
		for(RouterInstance ccu: kccus.values()) {
			Resource parent = ccu.device.device().getLocationResource().getParent();
			if(parent == null)
				continue;
			if(device.getLocation().startsWith(parent.getLocation()))
				return ccu;
		}
		return null;		
	}
	public static InstallAppDevice getCCU(PhysicalElement device, DatapointService dpService) {
		Collection<InstallAppDevice> ccus = ChartsUtil.getCCUs(dpService);
		for(InstallAppDevice ccu: ccus) {
			Resource parent = ccu.device().getLocationResource().getParent();
			if(parent == null)
				continue;
			if(device.getLocation().startsWith(parent.getLocation()))
				return ccu;
		}
		return null;
	}

	//@Override
	//protected abstract SensorData getSensorDataInstance(Resource sensor);

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected Collection<RouterInstance> getRouters() {
		updateCCUList();
		return (Collection)knownCCUs().values();
	}
	
	@Override
	protected void writeDataReporting(RouterInstance ccu, long deltaT) {
		super.writeDataReporting(ccu, deltaT);
		
		
		CCUInstance ccuMy = (CCUInstance)ccu;
		if(!ccuMy.device.isActive())
			return;
		
		ccuMy.dutyCycleValueMax = ccuMy.dutyCycleValueMaxNew;		
		if(ccuMy.dutyCycleMax != null) {
			ValueResourceHelper.setCreate(ccuMy.dutyCycleMax, ccuMy.dutyCycleValueMax);
		}
		ccuMy.dutyCycleValueMaxNew = 0;		
	}
	
	/*@Override
	public long knownSetpointValueOmitDuration(T temperatureSetpoint) {
		RouterInstance ccu = getCCU(temperatureSetpoint);
		if(ccu != null && isRouterInOverload(ccu, ccu.dutyCycleWarningYellow())) {
			return 5*TimeProcUtil.MINUTE_MILLIS;
		}
		return super.knownSetpointValueOmitDuration(temperatureSetpoint);
	}*/
}
