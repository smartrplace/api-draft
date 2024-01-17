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
import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.model.devices.buildingtechnology.AirConditioner;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.frontend.servlet.UserServlet;
import org.smartrplace.util.virtualdevice.HmSetpCtrlManager.WritePrioLevel;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;

public abstract class SetpointControlManager<T extends ValueResource> {
	public static final long CCU_UPDATE_INTERVAL = 10000;
	public static final long DEFAULT_EVAL_INTERVAL = Long.getLong("org.smartrplace.util.virtualdevice.evalinterval", 5*TimeProcUtil.MINUTE_MILLIS);
	private static final long RESEND_TIMER_INTERVAL = 60000;
	public static final float CONDITIONAL_PRIO_DEFAULT = 0.3f;
	public static final float PRIORITY_PRIO_DEFAULT = 1.0f;
	public static final int FORCE_PRIO = 1000;
	public static final long KEEPKNOWNTEMPERATURES_DURATION_DEFAULT = 8*TimeProcUtil.MINUTE_MILLIS; //30000;
	public static final long PENDING_TimeForMissingFeedback_DEFAULT = Long.getLong("org.smartrplace.util.virtualdevice.maxfeedbacktime", 8*TimeProcUtil.MINUTE_MILLIS);

	public static final String totalWritePerHour = "totalWritePerHour";
	public static final String conditionalWritePerHour = "conditionalWritePerHour";
	public static final String conditionalDropPerHour = "conditionalDropPerHour";
	public static final String priorityWritePerHour = "priorityWritePerHour";
	public static final String priorityDropPerHour = "priorityDropPerHour";
	public static final String resendMissingFbPerHour = "resendMissingFbPerHour";
	public static final String relativeLoadEff = "relativeLoadEff";
	private static final double RESEND_INCREASE_FACTOR = 1.1;
	private static final Long DEFAULT_MIN_INTERVAL_BETWEEN_WRITES = 5000l;
	
	public static enum SetpointControlType {
		/** Default types use the gateway as CCU/router*/
		ThermostatDefault,
		AirconditionerDefault,
		HmThermostat
	}
	
	public abstract boolean isSensorInOverload(SensorData data, float maxDC);
	protected abstract boolean isRouterInOverloadForced(RouterInstance ccu, float priority);
	public abstract RouterInstance getCCU(Resource sensor);
	protected abstract void updateCCUListForced();
	//protected abstract SensorData getSensorDataInstance(Resource sensor);
	protected abstract Collection<RouterInstance> getRouters();
	
	final TimeResource knownSetpointValBlockTime;
	public long knownSetpointValueOmitDuration(T temperatureSetpoint) {
		if(knownSetpointValBlockTime != null && knownSetpointValBlockTime.isActive())
			return knownSetpointValBlockTime.getValue();
		return KEEPKNOWNTEMPERATURES_DURATION_DEFAULT;
	}
	public long knownSetpointValueOmitDuration(SensorData sd) {
		return knownSetpointValueOmitDuration((T)null);
	}
	
	protected boolean resendIfNoFeedback() {
		return true;
	}
	
	protected final Timer resendTimer;
	protected Logger log = LoggerFactory.getLogger(this.getClass());
	
	private static Logger log2;
	static Logger log() {
		if(log2 != null)
			return log2;
		return LoggerFactory.getLogger(SetpointControlManager.class);
	}

	/** Resend if no first sending was successful, but no fitting feedback is available
	 * Initial resend interval*/
	protected final long pendingTimeForMissingFeedbackDefault;
	/** Retry sending after this time if feedback does not fit request */
	protected final long pendingTimeForRetry;
	/** If a request has actually been sent then wait this time. After pendingTimeForRetry we check again if router is not in overload 
	 * TODO: May be settable in constructor also in the future*/
	protected final long lastSentAgoForRetry = 15*TimeProcUtil.MINUTE_MILLIS;
	

	public static class RouterInstance {
		volatile int totalWriteCount = 0;
		volatile int conditionalWriteCount = 0;
		volatile int conditionalDropCount = 0;
		volatile int priorityWriteCount = 0;
		volatile int priorityDropCount = 0;
		volatile int resendMissingFbCount = 0;
		/** During an evaluation interval this value should only go up and shall be limited below
		 * 100%, usually below the level of PRIORITY_PRIO<br>
		 * Note: Logged into {@link #relativeLoadEff} as RouterLoadEff*/
		volatile float relativeLoadMax = 0;
		
		InstallAppDevice device;
		public FloatResource conditionalWritePerHour;
		public FloatResource conditionalDropPerHour;
		public FloatResource priorityWritePerHour;
		public FloatResource priorityDropPerHour;
		public FloatResource totalWritePerHour;
		public FloatResource resendMissingFbPerHour;
		
		/** Logged in RouterLoadEff via relativeLoadMax*/
		public FloatResource relativeLoadEff;
		
		/** Overwrite for real CCU*/
		float dutyCycleWarningYellow() {
			return CONDITIONAL_PRIO_DEFAULT;
		}
		float dutyCycleWarningRed() {
			return PRIORITY_PRIO_DEFAULT;
		}
	}
	protected final ApplicationManager appMan;
	protected final ApplicationManagerPlus appManPlus;
	protected final DatapointService dpService;
	
	protected final RouterInstance nullRouterInstance = new RouterInstance();
	protected boolean nullRouterInstanceUsed = false;
	protected final Map<RouterInstance, Map<String, SensorData>> knownSensors = new HashMap<>();
	protected List<SensorData> knownSensorsAll() {
		List<SensorData> result = new ArrayList<>();
		for(Map<String, SensorData> mapInner: knownSensors.values()) {
			result.addAll(mapInner.values());
		}
		return result ;
	}
	
	long nextEvalInterval;
	long intervalStart;

	public static long lastOvervloadEventRed = -1;
	public static long lastOvervloadEventYellow = -1;
	
	protected SetpointControlManager(ApplicationManagerPlus appManPlus) {
		this(appManPlus, PENDING_TimeForMissingFeedback_DEFAULT);
	}
	protected SetpointControlManager(ApplicationManagerPlus appManPlus, long pendingTimeForRetry) {
		this.appManPlus = appManPlus;
		this.appMan = appManPlus.appMan();
		this.log2 = log;
		this.dpService = appManPlus.dpService();
		this.pendingTimeForRetry = pendingTimeForRetry;
		this.pendingTimeForMissingFeedbackDefault = pendingTimeForRetry;
		this.knownSetpointValBlockTime = appMan.getResourceAccess().getResource("smartrplaceHeatcontrolConfig/knownSetpointValBlockTime");
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
	
	public abstract Resource getSensor(T setp);
	
	public abstract SensorData registerSensor(T setp, Resource sensor, Map<String, SensorData> knownSensorsInner);
	
	public SensorData registerSensor(T setp) {
		Resource sensor = getSensor(setp);
		RouterInstance router = getCCU(sensor);
		if(router == null) {
			nullRouterInstanceUsed = true;
			router = nullRouterInstance;
		}
		synchronized (knownSensors) {
			return registerSensorUnsynchronized(setp, sensor, router);
		}
	}
	private SensorData registerSensorUnsynchronized(T setp, Resource sensor, RouterInstance router) {
		Map<String, SensorData> mapInner = knownSensors.get(router);
		if(mapInner == null) {
			mapInner = new HashMap<>();
			knownSensors.put(router, mapInner);
		}
		SensorData sens = registerSensor(setp, sensor, mapInner);
		if(sens == null) {
			sens = registerSensor(setp, sensor, mapInner);
			throw new IllegalStateException("Registered as null for "+setp.getLocation());		
		}
		return sens;
	}
	
	public SensorData getSensorData(T setp) {
		Resource sensor = getSensor(setp);
		RouterInstance router = getCCU(sensor);
		if(router == null) {
			nullRouterInstanceUsed = true;
			router = nullRouterInstance;
		}
		//if(router == null)
		//	return null;
		synchronized (knownSensors) {
			Map<String, SensorData> mapInner = knownSensors.get(router);
			if(mapInner == null)
				return registerSensorUnsynchronized(setp, sensor, router);
			SensorData result = mapInner.get(setp.getLocation());
			if(result == null) {
				return registerSensorUnsynchronized(setp, sensor, router);
				//throw new IllegalStateException(" Null entry for "+setp.getLocation()+" !!(gSD)");
			}
			return result;
		}
	}
	/** Request to send new setpoint value to device
	 * 
	 * @param setp
	 * @param setpoint
	 * @param priority
	 * @return true if resource is written that triggers sending setpoint to device. False if request is dropped or postponed.
	 * 		If request is dropped because setpoint and feedback already have the value requested true is returned (success)
	 */
	public boolean requestSetpointWrite(T setp, float setpoint, WritePrioLevel prioLevel, boolean resendEvenIfConditional) {
		return requestSetpointWrite(setp, setpoint, null, prioLevel, resendEvenIfConditional, false, false);
	}
	public boolean requestSetpointWrite(T setp, float setpoint, WritePrioLevel prioLevel, boolean resendEvenIfConditional, boolean writeEvenIfNochChangeForFeedbackAndSetpoint) {
		return requestSetpointWrite(setp, setpoint, null, prioLevel, resendEvenIfConditional,
				writeEvenIfNochChangeForFeedbackAndSetpoint, false);
	}
	/** Method to be called to write to String and ArrayResources and other cases that cannot be covered with a float argument.*/
	public boolean requestSetpointWrite(T setp, Object setpointData, WritePrioLevel prioLevel, boolean resendEvenIfConditional, boolean writeEvenIfNochChangeForFeedbackAndSetpoint) {
		return requestSetpointWrite(setp, Float.NaN, setpointData, prioLevel, resendEvenIfConditional,
				writeEvenIfNochChangeForFeedbackAndSetpoint, false);
	}
	/** Call this if the setpoint should be set by the device itself or via transaction, just request a feedback and resend if feedback is not received*/
	public boolean requestSetpointFeedback(T setp, float setpoint, WritePrioLevel prioLevel, boolean resendEvenIfConditional) {
		return requestSetpointWrite(setp, setpoint, null, prioLevel, resendEvenIfConditional, false, true);
	}
	
	protected boolean requestSetpointWrite(T setp, float setpoint, Object setpointData, WritePrioLevel prioLevel,
			boolean resendEvenIfConditional, boolean writeEvenIfNochChangeForFeedbackAndSetpoint,
			boolean requestConfirmationOnly) {
		return requestSetpointWrite(setp, setpoint, setpointData, prioLevel, resendEvenIfConditional, writeEvenIfNochChangeForFeedbackAndSetpoint,
				requestConfirmationOnly, DEFAULT_MIN_INTERVAL_BETWEEN_WRITES);
	}
	/**
	 * 
	 * @param setp
	 * @param setpoint
	 * @param setpointData
	 * @param prioLevel
	 * @param resendEvenIfConditional
	 * @param writeEvenIfNochChangeForFeedbackAndSetpoint
	 * @param requestConfirmationOnly if set true no real write operation is performed initially, but the manager only checks if the
	 * 		requested value is confirmed by feedback. This can be used is the value setting is expected to be made by auto-settings
	 * @return false if the request is still pending e.g. as request could not be written due to high router load
	 */
	protected boolean requestSetpointWrite(T setp, float setpoint, Object setpointData, WritePrioLevel prioLevel,
			boolean resendEvenIfConditional, boolean writeEvenIfNochChangeForFeedbackAndSetpoint,
			boolean requestConfirmationOnly, Long minRewriteTime) {
		SensorData sens = registerSensor(setp);
		sens.pendingTimeForMissingFeedback = pendingTimeForMissingFeedbackDefault;
		
		float maxDC;
		switch(prioLevel) {
		case MUST_WRITE:
			maxDC = 1.1f;
			break;
		case PRIORITY:
			if(sens.ccu() != null)
				maxDC = sens.ccu().dutyCycleWarningRed();
			else
				maxDC = PRIORITY_PRIO_DEFAULT;
			break;
		case CONDITIONAL:
			if(sens.ccu() != null)
				maxDC = sens.ccu().dutyCycleWarningYellow();
			else
				maxDC = CONDITIONAL_PRIO_DEFAULT;
			break;
		default:
			throw new IllegalStateException("Unknown prio level:"+prioLevel);
		}
		return requestSetpointWrite(setp, setpoint, setpointData, sens, maxDC,
				resendEvenIfConditional, writeEvenIfNochChangeForFeedbackAndSetpoint, requestConfirmationOnly, minRewriteTime);
	}
	protected boolean requestSetpointWrite(T setp, float setpoint, Object setpointData,
			SensorData sens, float maxDC,
			boolean resendEvenIfConditional, boolean writeEvenIfNochChangeForFeedbackAndSetpoint,
			boolean requestConfirmationOnly, Long minRewriteTime) {
		if((!writeEvenIfNochChangeForFeedbackAndSetpoint) && (setpointData == null)) {
			//we do not support this for array resources etc. yet
			boolean done = true;
			if(sens.setpoint() instanceof SingleValueResource) {
				float curSetp = ValueResourceUtils.getFloatValue((SingleValueResource) sens.setpoint());
				if(!ValueResourceHelper.isAlmostEqual(curSetp, setpoint))
					done = false;
				if(done && (sens.feedback() instanceof SingleValueResource)) {
					float curFb = ValueResourceUtils.getFloatValue((SingleValueResource) sens.feedback());
					if(!ValueResourceHelper.isAlmostEqual(curFb, setpoint))
						done = false;					
				}
				if(done) {
					sens.valuePending = null;
					sens.valueFeedbackPending = null;
					if(Boolean.getBoolean("setpointcontrolmanager.logdetails"))
						log.debug("In DONE1 For "+sens.setpoint().getLocation()+" valueFeedback:"+sens.valueFeedbackPending);
					return true;
				}
			}
		}
		boolean isOverload;
		if(minRewriteTime != null) {
			long now = appMan.getFrameworkTime();
			long sentAgo = now - sens.lastSent;
			if(sentAgo < minRewriteTime) {
				if(Boolean.getBoolean("setpointcontrolmanager.logdetails"))
					log.debug("Retarding write due to MIN_REWRITE_TIME for "+sens.setpoint().getLocation()+" minRewriteTime:"+minRewriteTime+" valueFeedback:"+sens.valueFeedbackPending);
				sens.pendingTimeForMissingFeedback = Math.max(10000l,  DEFAULT_MIN_INTERVAL_BETWEEN_WRITES);
				isOverload = true;
			}
			isOverload = isSensorInOverload(sens, maxDC);
		} else
			isOverload = isSensorInOverload(sens, maxDC);
		if((!isOverload) || requestConfirmationOnly) {
			//We do not actively reset overload, but it just expires
			//sens.valuePendingDueToOverloadSince = -1;
			long now = appMan.getFrameworkTime();
			if(!requestConfirmationOnly) {
				if(sens.ccu() != null) {
					if(maxDC <= sens.ccu().dutyCycleWarningYellow())
						sens.ccu().conditionalWriteCount++;
					else
						sens.ccu().priorityWriteCount++;
					sens.lastSent = now;
				}
				if(setpointData != null)
					sens.writeSetpointData(setpointData);
				else {
					sens.writeSetpoint(setpoint);
					sens.reportSetpoint(setpoint);
				}
				//if(Boolean.getBoolean("org.smartrplace.util.virtualdevice.noresend"))
				//	return true;
			}
			if(!resendIfNoFeedback()) {
				sens.valuePending = null;
				sens.valueFeedbackPending = null;
				if(Boolean.getBoolean("setpointcontrolmanager.logdetails"))
					log.debug("In RESEND_IF_NO_FB For "+sens.setpoint().getLocation()+" valueFeedback:"+sens.valueFeedbackPending);
				return true;
			}

			if((!writeEvenIfNochChangeForFeedbackAndSetpoint) && (setpointData == null)) {
				//we do not support this for array resources etc. yet
				boolean done = true;
				if(sens.feedback() instanceof SingleValueResource) {
					float curFb = ValueResourceUtils.getFloatValue((SingleValueResource) sens.feedback());
					if(!ValueResourceHelper.isAlmostEqual(curFb, setpoint))
						done = false;					
				}
				if(done) {
					sens.valuePending = null;
					sens.valueFeedbackPending = null;
					if(Boolean.getBoolean("setpointcontrolmanager.logdetails"))
						log.debug("In DONE2 For "+sens.setpoint().getLocation()+" valueFeedback:"+sens.valueFeedbackPending);
					return true;
				}
			}
						
			if(sens.valueFeedbackPending == null || (sens.valueFeedbackPending != setpoint)) {
				sens.valuePendingSince = now;
				sens.maxDC = maxDC;
				if(isOverload)
					setLastOverloadEvent(now, maxDC);
			}
			if(setpointData != null)
				sens.valueFeedbackPendingObject = setpointData;
			sens.valueFeedbackPending = setpoint;
			if(Boolean.getBoolean("setpointcontrolmanager.logdetails"))
				log.debug("For "+sens.setpoint().getLocation()+" valueFeedback:"+sens.valueFeedbackPending);
			sens.valuePending = null;
			return true;
		} //if((!isOverload) || requestConfirmationOnly) {
		if(sens.ccu() != null) {
			if(maxDC <= sens.ccu().dutyCycleWarningYellow()) {
				sens.ccu().conditionalDropCount++;
				if(!resendEvenIfConditional)
					return false;
			} else {
				sens.ccu().priorityDropCount++;
			}

			if(sens.valueFeedbackPending != null) {
				if(sens.valueFeedbackPending == setpoint) {
					//we stay in feedback mode
					return false;
				} else
					sens.valueFeedbackPending = null;
			}
			
			long now = appMan.getFrameworkTime();
			if(sens.valuePending == null) {
				sens.valuePendingSince = now;
				sens.maxDC = maxDC;
				if(isOverload)
					setLastOverloadEvent(now, maxDC);
			} else if(setpoint != sens.valuePending) {
				sens.valuePendingSince = now;					
				sens.maxDC = maxDC;
				if(isOverload)
					setLastOverloadEvent(now, maxDC);
			}

			if(setpointData != null)
				sens.valuePendingObject = setpointData;
			else
				sens.valuePending = setpoint;
		}
		return false;
	}
	
	private void setLastOverloadEvent(long now, float maxDC) {
		if(maxDC > 0.5f)
			lastOvervloadEventRed = now;
		else
			lastOvervloadEventYellow = now;
		
	}
	protected void stopInternal() {
		for(SensorData sens: knownSensorsAll()) {
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
		if(!ccu.device.isActive())
			return;
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
		
		if(ccu.resendMissingFbPerHour!= null) {
			ValueResourceHelper.setCreate(ccu.resendMissingFbPerHour, (float) (((double)(ccu.resendMissingFbCount*TimeProcUtil.HOUR_MILLIS))/deltaT));
		}
		ccu.resendMissingFbCount = 0;

		if(ccu.relativeLoadEff != null) {
			ValueResourceHelper.setCreate(ccu.relativeLoadEff, ccu.relativeLoadMax);
		}
		ccu.relativeLoadMax = 0;		

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
	
	@SuppressWarnings("unchecked")
	protected void resendTimerUpdate() {
		Collection<RouterInstance> routers = getRouters();
		if(nullRouterInstanceUsed) {
			routers = new ArrayList<>(routers);
			routers.add(nullRouterInstance);
		}
		long now = appMan.getFrameworkTime();
		log.debug("In timer of "+this.getClass().getSimpleName()+" found "+routers.size()+" routers to update.");
		Long deltaT;
		if(now > nextEvalInterval) {
			deltaT = now - intervalStart;
			
			log.debug("In "+this.getClass().getSimpleName()+" write report resources for "+deltaT+" msec");
			

			intervalStart = nextEvalInterval;
			nextEvalInterval += DEFAULT_EVAL_INTERVAL;
		} else
			deltaT = null;
		for(RouterInstance cd: routers) {
			if(deltaT != null && (cd != nullRouterInstance))
				writeDataReporting(cd, deltaT);
			//checkForDataReporting(cd);

			float retryAfterOverloadLimit = Math.min(0.9f*cd.dutyCycleWarningRed(), 2.5f*cd.dutyCycleWarningYellow());
			if((cd != nullRouterInstance) && isRouterInOverload(cd, retryAfterOverloadLimit))
				continue;
			Map<String, SensorData> subMap = knownSensors.get(cd);
			if(subMap == null)
				continue;
			List<SensorData> allresend = new ArrayList<>(subMap.values());
			for(SensorData resenddata: allresend) {
				if(resenddata.valueFeedbackPending != null) {
					long timePendingFb = now - resenddata.valuePendingSince;
					//long sentAgoFb = now - resenddata.lastSent;
					if((timePendingFb > resenddata.pendingTimeForMissingFeedback)) { // && (sentAgoFb > lastSentAgoForRetry)) {
						if(resenddata.valueFeedbackPending != null)
							log.warn("Feedback missing for "+resenddata.setpoint().getLocation()+" for "+timePendingFb+" msec. Resending, value:"+(float)resenddata.valueFeedbackPending+".");
						else
							log.warn("Feedback missing for "+resenddata.setpoint().getLocation()+" for "+timePendingFb+" msec. Resending.");
						cd.resendMissingFbCount++;
						boolean success;
						if(resenddata.valueFeedbackPendingObject != null) {
							success = requestSetpointWrite((T) resenddata.setpoint(), resenddata.valueFeedbackPendingObject,
									WritePrioLevel.CONDITIONAL, false, false);							
							log.debug("Resending object for "+resenddata.setpoint().getLocation()+
									" with interval sec:"+(resenddata.pendingTimeForMissingFeedback/1000));
						} else try {
							success = requestSetpointWrite((T) resenddata.setpoint(), (float)resenddata.valueFeedbackPending,
								WritePrioLevel.CONDITIONAL, false, false);
							log.debug("Resending val "+String.format("%.1f", (float)resenddata.valuePending)+" for "+resenddata.setpoint().getLocation()+
									" with interval sec:"+(resenddata.pendingTimeForMissingFeedback/1000));
						} catch(NullPointerException e) {
							String text = "Resend failed";
							if(resenddata.setpoint() != null)
								text += "  Setpoint:"+resenddata.setpoint().getLocation();
							else
								text += "  Setpoint null!  ";
							if(resenddata.feedback() != null)
								text += "  Feedback:"+resenddata.feedback().getLocation();
							else
								text += "  Feedback null!  ";
							text += "  Value:"+resenddata.valueFeedbackPending;
							UserServlet.logException(e, null, -11, appManPlus);
							throw new IllegalStateException(text, e);
						}
						if(!success) {
							//CHANGED (not anymore): we stop the chain due to overload
							//resenddata.valueFeedbackPending = null;
							resenddata.lastSent = now;
						} else { 
							resenddata.valuePendingSince = now;
							//	break;
						}
					}
					continue;
				}
				//valuePending is for processing requests postponed due to overload
				if(resenddata.valuePending == null)
					continue;
				long timePending = now - resenddata.valuePendingSince;
				long sentAgo = now - resenddata.lastSent;
				if((timePending > pendingTimeForRetry) && (sentAgo > lastSentAgoForRetry)) {
					//NOTE: valuePending is set already so we do not have to indicate resend, will take place anyways if no success.
					resenddata.pendingTimeForMissingFeedback = (long) (RESEND_INCREASE_FACTOR * (double)resenddata.pendingTimeForMissingFeedback);
					boolean success;
					if(resenddata.valueFeedbackPendingObject != null) {
						success = requestSetpointWrite((T) resenddata.setpoint(), Float.NaN, resenddata.valuePendingObject,
								resenddata, Math.min(retryAfterOverloadLimit, resenddata.maxDC), false, false, false, DEFAULT_MIN_INTERVAL_BETWEEN_WRITES);
						log.debug("Retry to send of object for "+resenddata.setpoint().getLocation()+
								" with interval sec:"+(pendingTimeForRetry/1000));
					} else {
						success = requestSetpointWrite((T) resenddata.setpoint(), (float)resenddata.valuePending, null,
								resenddata, Math.min(retryAfterOverloadLimit, resenddata.maxDC), false, false, false, 5000l);
						log.debug("Retry to send of val "+String.format("%.1f", (float)resenddata.valuePending)+" for "+resenddata.setpoint().getLocation()+
								" with interval sec:"+(pendingTimeForRetry/1000));
					}
					if(success) {
						resenddata.valuePending = null;
						resenddata.lastSent = now;
					} /*else
					break;*/
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
		cd.resendMissingFbPerHour = ccu.getSubResource(resendMissingFbPerHour, FloatResource.class);
		cd.relativeLoadEff = ccu.getSubResource(relativeLoadEff, FloatResource.class);
	}
	
	public boolean isRouterInOverload(RouterInstance ccu, float priority) {
		//TODO: implement caching per ccu and priority
		return isRouterInOverloadForced(ccu, priority);
	}
	
	public static SetpointControlType getControlType(SingleValueResource setpoint) {
		if(DeviceTableBase.isHomematic(setpoint.getLocation()))
			return SetpointControlType.HmThermostat;
		PhysicalElement device = ResourceHelper.getFirstParentOfType(setpoint, Thermostat.class); //LogHelper.getDeviceResource(setpoint, false, true);
		if(device != null)
			return SetpointControlType.ThermostatDefault;
		device = ResourceHelper.getFirstParentOfType(setpoint, AirConditioner.class); //LogHelper.getDeviceResource(setpoint, false, true);		
		if(device != null)
			return SetpointControlType.AirconditionerDefault;
		//TODO: Maybe we should just return null, but this is for fail-fast
		throw new IllegalStateException("Unknown heat control type : "+setpoint.getLocation());
	}
	
	/** Overwrite if implementation is used*/
	public boolean isFeedbackFullySet(float t, T setPoint) {
		return false;
	}
	
	boolean finalLoggerFound = false;
	protected void setHeatcontrolLogger(ApplicationManager appLoc) {
		if(finalLoggerFound)
			return;
		Bundle bundle = appLoc.getAppID().getBundle();
		if(bundle != null && bundle.getSymbolicName().contains("-heatcontrol-v")) {
			log = appLoc.getLogger();
			finalLoggerFound = true;
		}
	}
}
