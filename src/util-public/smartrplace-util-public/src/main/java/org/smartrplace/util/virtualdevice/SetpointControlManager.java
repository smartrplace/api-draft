package org.smartrplace.util.virtualdevice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.model.devices.buildingtechnology.AirConditioner;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.Sensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.logconfig.LogHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;

public abstract class SetpointControlManager<T extends SingleValueResource> {
	public static final long CCU_UPDATE_INTERVAL = 10000;
	public static final long DEFAULT_EVAL_INTERVAL = 5*TimeProcUtil.MINUTE_MILLIS;
	private static final long RESEND_TIMER_INTERVAL = 60000;
	public static final int CONDITIONAL_PRIO = 60;
	public static final int PRIORITY_PRIO = 90;
	public static final int FORCE_PRIO = 1000;
	public static final long KEEPKNOWNTEMPERATURES_DURATION_DEFAULT = 30000;

	public static final String totalWritePerHour = "totalWritePerHour";
	public static final String conditionalWritePerHour = "conditionalWritePerHour";
	public static final String conditionalDropPerHour = "conditionalDropPerHour";
	public static final String priorityWritePerHour = "conditionalWritePerHour";
	public static final String priorityDropPerHour = "conditionalDropPerHour";
	
	public static enum SetpointControlType {
		/** Default types use the gateway as CCU/router*/
		ThermostatDefault,
		AirconditionerDefault,
		HmThermostat
	}
	
	public abstract boolean isSensorInOverload(SensorData data, int priority);
	public abstract boolean isRouterInOverload(RouterInstance ccu, int priority);
	public abstract RouterInstance getCCU(Sensor sensor);
	protected abstract void updateCCUListForced();
	protected abstract SensorData getSensorDataInstance(Sensor sensor);
	protected abstract Collection<RouterInstance> getRouters();
	public long knownSetpointValueOmitDuration(TemperatureResource temperatureSetpoint) {
		return KEEPKNOWNTEMPERATURES_DURATION_DEFAULT;
	}	
	
	protected final Timer resendTimer;
	protected Logger log = LoggerFactory.getLogger(this.getClass());

	public static class RouterInstance {
		volatile int totalWriteCount = 0;
		volatile int conditionalWriteCount = 0;
		volatile int conditionalDropCount = 0;
		volatile int priorityWriteCount = 0;
		volatile int priorityDropCount = 0;
		InstallAppDevice device;
		public FloatResource conditionalWritePerHour;
		public FloatResource conditionalDropPerHour;
		public FloatResource priorityWritePerHour;
		public FloatResource priorityDropPerHour;
		public FloatResource totalWritePerHour;
		
		//setpoint location -> data
		Map<String, SensorData> sensorResendMan = new HashMap<>();
	}
	public static abstract class SensorData {
		abstract Sensor sensor();
		abstract SingleValueResource setpoint();
		abstract SingleValueResource feedback();
		abstract RouterInstance ccu();
		abstract ResourceValueListener<?> setpointListener();
		abstract void stop();
		abstract void writeSetpoint(float value);
		
		//Resend data
		long lastSent = 1;
		SensorData sensor;
		Float valuePending;
		long valuePendingSince;
		
		//Check feedback
		volatile Float valueFeedbackPending = null;
	}
	protected final ApplicationManager appMan;
	protected final DatapointService dpService;
	protected final Map<String, SensorData> knownSensors = new HashMap<>();
	
	long nextEvalInterval;
	long intervalStart;
		
	protected SetpointControlManager(ApplicationManagerPlus appManPlus) {
		this.appMan = appManPlus.appMan();
		this.dpService = appManPlus.dpService();
		intervalStart = appMan.getFrameworkTime();
		nextEvalInterval = intervalStart + DEFAULT_EVAL_INTERVAL;
		resendTimer = appMan.createTimer(RESEND_TIMER_INTERVAL, new TimerListener() {
			
			@Override
			public void timerElapsed(Timer timer) {
				resendTimerUpdate();
			}
		});
		log.info("Started timer for SetpointControlManager:"+this.getClass().getSimpleName());
	}
	
	public SensorData registerSensor(T setp) {
		String loc = setp.getLocation();
		SensorData result = knownSensors.get(loc);
		if(result != null)
			return result;
		TemperatureSensor sensor = ResourceHelper.getFirstParentOfType(setp, TemperatureSensor.class);
		result = getSensorDataInstance(sensor); //new SensorData(sensor, this);
		knownSensors.put(loc, result);
		return result;
	}
	
	/** Request to send new setpoint value to device
	 * 
	 * @param setp
	 * @param setpoint
	 * @param priority
	 * @return true if resource is written that triggers sending setpoint to device. False if request is dropped or postponed
	 */
	public boolean requestSetpointWrite(T setp, float setpoint, int priority) {
		SensorData sens = registerSensor(setp);
		boolean isOverload = isSensorInOverload(sens, priority);
		if(!isOverload) {
			long now = appMan.getFrameworkTime();
			if(sens.ccu() != null) {
				if(priority <= CONDITIONAL_PRIO)
					sens.ccu().conditionalWriteCount++;
				else
					sens.ccu().priorityWriteCount++;
				sens.lastSent = now;
			}
			sens.writeSetpoint(setpoint);
			if(sens.valueFeedbackPending == null || (sens.valueFeedbackPending != setpoint))
				sens.valuePendingSince = now;
			sens.valueFeedbackPending = setpoint;
			sens.valuePending = null;
			return true;
		}
		if(sens.ccu() != null) {
			sens.valueFeedbackPending = null;
			if(priority <= CONDITIONAL_PRIO)
				sens.ccu().conditionalDropCount++;
			else {
				long now = appMan.getFrameworkTime();
				if(sens.valuePending == null) {
					sens.valuePending = setpoint;
					sens.valuePendingSince = now;
				} else if(setpoint != sens.valuePending) {
					sens.valuePendingSince = now;					
				}
				sens.ccu().priorityDropCount++;
			}
		}
		return false;
	}
	
	protected void stopInternal() {
		for(SensorData sens: knownSensors.values()) {
			if(sens.setpointListener() != null && sens.setpoint() != null) {
				sens.setpoint().removeValueListener(sens.setpointListener());
				sens.stop();
			}
		}
		if(resendTimer != null) {
			resendTimer.destroy();
		};
	}
	
	protected void writeDataReporting(RouterInstance ccu, long deltaT) {
		if(ccu.totalWritePerHour != null) {
			ValueResourceHelper.setCreate(ccu.totalWritePerHour, (float) (((double)(ccu.totalWriteCount*TimeProcUtil.HOUR_MILLIS))/deltaT));
		}
		ccu.totalWriteCount = 0;

		if(ccu.conditionalWritePerHour!= null) {
			ValueResourceHelper.setCreate(ccu.conditionalWritePerHour, (float) (((double)(ccu.conditionalWriteCount*TimeProcUtil.HOUR_MILLIS))/deltaT));
		}
		ccu.conditionalWriteCount = 0;
		if(ccu.conditionalDropPerHour!= null) {
			ValueResourceHelper.setCreate(ccu.conditionalDropPerHour, (float) (((double)(ccu.conditionalDropCount*TimeProcUtil.HOUR_MILLIS))/deltaT));
		}
		ccu.conditionalDropCount = 0;
		
		if(ccu.priorityWritePerHour!= null) {
			ValueResourceHelper.setCreate(ccu.priorityWritePerHour, (float) (((double)(ccu.priorityWriteCount*TimeProcUtil.HOUR_MILLIS))/deltaT));
		}
		ccu.priorityWriteCount = 0;
		if(ccu.priorityDropPerHour!= null) {
			ValueResourceHelper.setCreate(ccu.priorityDropPerHour, (float) (((double)(ccu.priorityDropCount*TimeProcUtil.HOUR_MILLIS))/deltaT));
		}
		ccu.priorityDropCount = 0;		
	}
	/*protected void checkForDataReporting(RouterInstance ccu) {
		long now = appMan.getFrameworkTime();
		if(now > nextEvalInterval)  synchronized (this) {
			long deltaT = now - intervalStart;
			
			log.info("In "+this.getClass().getSimpleName()+" write report resources for "+deltaT+" msec");
			writeDataReporting(ccu, deltaT);

			intervalStart = nextEvalInterval;
			nextEvalInterval += DEFAULT_EVAL_INTERVAL;
		}		
	}*/
	
	protected void reportSetpointRequest(RouterInstance ccu) {
		//checkForDataReporting(ccu);
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
	
	//TODO: Shall be flexible in the future
	long pendingTimeForRetry = 8*TimeProcUtil.MINUTE_MILLIS;
	long lastSentAgoForRetry = 15*TimeProcUtil.MINUTE_MILLIS;
	@SuppressWarnings("unchecked")
	protected void resendTimerUpdate() {
		Collection<RouterInstance> routers = getRouters();
		long now = appMan.getFrameworkTime();
		log.info("In timer of "+this.getClass().getSimpleName()+" found "+routers.size()+" routers to update.");
		Long deltaT;
		if(now > nextEvalInterval) {
			deltaT = now - intervalStart;
			
			log.info("In "+this.getClass().getSimpleName()+" write report resources for "+deltaT+" msec");
			

			intervalStart = nextEvalInterval;
			nextEvalInterval += DEFAULT_EVAL_INTERVAL;
		} else
			deltaT = null;
		for(RouterInstance cd: routers) {
			if(deltaT != null)
				writeDataReporting(cd, deltaT);
			//checkForDataReporting(cd);
			
			if(isRouterInOverload(cd, CONDITIONAL_PRIO))
				continue;
			List<SensorData> allresend = new ArrayList<>(cd.sensorResendMan.values());
			for(SensorData resenddata: allresend) {
				if(resenddata.valueFeedbackPending != null) {
					long timePendingFb = now - resenddata.valuePendingSince;
					long sentAgoFb = now - resenddata.lastSent;
					if((timePendingFb > pendingTimeForRetry) && (sentAgoFb > lastSentAgoForRetry)) {
						boolean success = requestSetpointWrite((T) resenddata.sensor.setpoint(), resenddata.valueFeedbackPending, CONDITIONAL_PRIO);
						if(success) {
							resenddata.valueFeedbackPending = null;
							resenddata.lastSent = now;
						} else
							break;
					}
					continue;
				}
				if(resenddata.valuePending == null)
					continue;
				long timePending = now - resenddata.valuePendingSince;
				long sentAgo = now - resenddata.lastSent;
				if((timePending > pendingTimeForRetry) && (sentAgo > lastSentAgoForRetry)) {
					boolean success = requestSetpointWrite((T) resenddata.sensor.setpoint(), resenddata.valuePending, CONDITIONAL_PRIO);
					if(success) {
						resenddata.valuePending = null;
						resenddata.lastSent = now;
					} else
						break;
				}
			}
		}
	}

	/** Base implementation that usually has to be overwritten by copying and extension*/
	protected void initRouterStandardValues(RouterInstance cd, InstallAppDevice ccu) {
		cd.device = ccu;
		cd.totalWritePerHour = ccu.getSubResource(totalWritePerHour, FloatResource.class);
		cd.conditionalWritePerHour = ccu.getSubResource(conditionalWritePerHour, FloatResource.class);
		cd.conditionalDropPerHour = ccu.getSubResource(conditionalDropPerHour, FloatResource.class);
		cd.priorityWritePerHour = ccu.getSubResource(priorityWritePerHour, FloatResource.class);
		cd.priorityDropPerHour = ccu.getSubResource(priorityDropPerHour, FloatResource.class);
	}
	
	public static SetpointControlType getControlType(SingleValueResource setpoint) {
		if(DeviceTableBase.isHomematic(setpoint.getLocation()))
			return SetpointControlType.HmThermostat;
		PhysicalElement device = LogHelper.getDeviceResource(setpoint, false, true);
		if(device instanceof AirConditioner)
			return SetpointControlType.AirconditionerDefault;
		if(device instanceof Thermostat)
			return SetpointControlType.ThermostatDefault;
		//TODO: Maybe we should just return null, but this is for fail-fast
		throw new IllegalStateException("Unknown heat control type : "+setpoint.getLocation());
	}
}