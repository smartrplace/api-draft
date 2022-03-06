package org.smartrplace.util.virtualdevice;

import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;

/** Depends on HmSetpCtrlManagerTHSetp */
public class HmSetpCtrlManagerTHControlMode extends HmSetpCtrlManager<IntegerResource> {
	/** Overwrite for other implementations*/
	protected IntegerResource getFeeedback(IntegerResource setp) {
		String name = setp.getName()+"Feedback";
		return setp.getParent().getSubResource(name, IntegerResource.class);
	}
	
	protected final HmSetpCtrlManagerTHSetp mainHmMan;
	protected Map<String, CCUInstance> knownCCUs() {
		return mainHmMan.knownCCUs();
	}
	@Override
	protected void updateCCUListForced() {}
	@Override
	protected void writeDataReporting(RouterInstance ccu, long deltaT) {}
	
	public static class SensorDataHmInt extends SensorDataInt2Int {
		CCUInstance ccu = null;
		
		public SensorDataHmInt(Resource sensor, IntegerResource setpoint, IntegerResource feedback,
				HmSetpCtrlManagerTHControlMode ctrl) {
			super(sensor, setpoint, feedback, ctrl);
		}

		@Override
		public RouterInstance ccu() {
			if(ccu == null) {
				ccu = (CCUInstance) ctrl.getCCU(parentAsSensor);
				if(ccu == null) {
					System.out.println("WARNING: No CCU found for setpoint sensor:"+parentAsSensor.getLocation());
				}
			}
			return ccu;
		}
	}
	
	private HmSetpCtrlManagerTHControlMode(ApplicationManagerPlus appManPlus, HmSetpCtrlManagerTHSetp mainHmMan) {
		super(appManPlus, 30*TimeProcUtil.MINUTE_MILLIS);
		this.mainHmMan = mainHmMan;
	}
	
	private static HmSetpCtrlManagerTHControlMode instance = null;
	public static HmSetpCtrlManagerTHControlMode getInstance(ApplicationManagerPlus appManPlus) {
		if(instance == null) {
			HmSetpCtrlManagerTHSetp mainHmManLoc = HmSetpCtrlManagerTHSetp.getInstance(appManPlus);
			instance = new HmSetpCtrlManagerTHControlMode(appManPlus, mainHmManLoc);
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
	public SensorDataHmInt registerSensor(IntegerResource setp, Resource sensor,
			Map<String, SensorData> knownSensorsInner) {
		String loc = setp.getLocation();
		SensorDataHmInt result = (SensorDataHmInt) knownSensorsInner.get(loc);
		if(result != null)
			return result;
		IntegerResource fb = getFeeedback(setp);
		result = new SensorDataHmInt(sensor, setp, fb, this);
		knownSensorsInner.put(loc, result);
		return result;
	}
	
	@Override
	public boolean isSensorInOverload(SensorData data, float maxDC) {
		CCUInstance ccu = ((SensorDataHmInt)data).ccu;
		if(ccu == null)
			return false;
		return isRouterInOverload(ccu, maxDC);
	}
}
