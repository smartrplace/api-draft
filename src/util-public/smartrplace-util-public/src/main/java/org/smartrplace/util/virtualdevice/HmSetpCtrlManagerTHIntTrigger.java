package org.smartrplace.util.virtualdevice;

import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;

/** Depends on HmSetpCtrlManagerTHSetp */
public class HmSetpCtrlManagerTHIntTrigger extends HmSetpCtrlManager<IntegerResource> {
	
	protected final HmSetpCtrlManagerTHSetp mainHmMan;
	protected Map<String, CCUInstance> knownCCUs() {
		return mainHmMan.knownCCUs();
	}
	@Override
	protected void updateCCUListForced() {}
	@Override
	protected void writeDataReporting(RouterInstance ccu, long deltaT) {}
	
	@Override
	protected boolean resendIfNoFeedback() {
		return false;
	}
	
	public static class SensorDataHmIntTrigger extends SensorDataWithoutFeedback {
		CCUInstance ccu = null;
		
		public SensorDataHmIntTrigger(IntegerResource setpoint,	HmSetpCtrlManagerTHIntTrigger ctrl) {
			super(setpoint, ctrl);
		}

		@Override
		public RouterInstance ccu() {
			if(ccu == null) {
				ccu = (CCUInstance) ctrl.getCCU(setpoint);
				if(ccu == null) {
					System.out.println("WARNING: No CCU found for setpoint sensor:"+setpoint.getLocation());
				}
			}
			return ccu;
		}
	}
	
	private HmSetpCtrlManagerTHIntTrigger(ApplicationManagerPlus appManPlus, HmSetpCtrlManagerTHSetp mainHmMan) {
		super(appManPlus, 30*TimeProcUtil.MINUTE_MILLIS);
		this.mainHmMan = mainHmMan;
	}
	
	private static HmSetpCtrlManagerTHIntTrigger instance = null;
	public static HmSetpCtrlManagerTHIntTrigger getInstance(ApplicationManagerPlus appManPlus) {
		if(instance == null) {
			HmSetpCtrlManagerTHSetp mainHmManLoc = HmSetpCtrlManagerTHSetp.getInstance(appManPlus);
			instance = new HmSetpCtrlManagerTHIntTrigger(appManPlus, mainHmManLoc);
		}
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
	public Resource getSensor(IntegerResource setp) {
		return setp.getParent();
	}
	
	@Override
	public SensorDataHmIntTrigger registerSensor(IntegerResource setp, Resource sensor,
			Map<String, SensorData> knownSensorsInner) {
		String loc = setp.getLocation();
		SensorDataHmIntTrigger result = (SensorDataHmIntTrigger) knownSensorsInner.get(loc);
		if(result != null)
			return result;
		appMan.getLogger().debug("Create and register SensorDataHmIntTrigger for "+loc);
		result = new SensorDataHmIntTrigger(setp, this);
		knownSensorsInner.put(loc, result);
		return result;
	}
	
	@Override
	public boolean isSensorInOverload(SensorData data, float maxDC) {
		CCUInstance ccu = ((SensorDataHmIntTrigger)data).ccu;
		if(ccu == null)
			return false;
		return isRouterInOverload(ccu, maxDC);
	}
}
