package org.smartrplace.tissue.util.logconfig;

import java.util.HashMap;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.gateway.device.MemoryTimeseriesPST;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;

public abstract class PerformanceLog {
	public static final int ZERO_COUNTER_REPORT_INTERVAL = 1000; //should be 10000
	public static final long MINIUM_DURATION_TO_CONSOLE = 10;
	
	protected FloatResource counter;
	protected FloatResource singleEvent;
	final boolean useCounter;
	final ApplicationManager appMan;
	protected int zeroCounter = 0;
	protected int zeroCounterSinceReport = 0;
	protected final String id;
	
	protected abstract FloatResource getEventResource(MemoryTimeseriesPST device);
	protected FloatResource getCounterResource(MemoryTimeseriesPST device) {return null;}
	
	public PerformanceLog(boolean useCounter, ApplicationManager appMan, String id) {
		this.useCounter = useCounter;
		this.appMan = appMan;
		this.id = id;
	}
	
	/** Write message to text log file and write to slotsDB single duration and 
	 * incremented duration counter
	 *  Note that event counter only will be written if duration is greater zero
	 *  or the org.smartrplace.tissue.util.logconfig.logzero=true property is set
	 * @param msec
	 * @param consoleMessageHeader
	 */
	public void logEvent(long msec, String consoleMessageHeader) {
		if(Boolean.getBoolean("evaldebug0")||Boolean.getBoolean("evaldebug")) {
			if(msec > MINIUM_DURATION_TO_CONSOLE)
				System.out.println(consoleMessageHeader+" "+msec+" msec");
		}
		logEvent(msec*0.001f);
	}
	public void logEvent(float value) {
		if(value == 0) {
			zeroCounter++;
			zeroCounterSinceReport++;
			if(zeroCounterSinceReport > ZERO_COUNTER_REPORT_INTERVAL) {
				if(Boolean.getBoolean("evaldebug0")||Boolean.getBoolean("evaldebug"))
					System.out.println("   ZERO-DURATIONS for "+id+":"+zeroCounter);
				zeroCounterSinceReport = 0;
			}
			if(!Boolean.getBoolean("org.smartrplace.tissue.util.logconfig.logzero"))
				return;
		}
		MemoryTimeseriesPST gw = null;
		if(singleEvent == null) {
			gw = ResourceHelper.getEvalCollection(appMan).getSubResource("memoryTimeseriesPST", MemoryTimeseriesPST.class);
			if(!gw.exists()) {
				gw.create();
				gw.activate(false);
			}
			singleEvent = getEventResource(gw);
		}
		ValueResourceHelper.setCreate(singleEvent, value);
		
		if(useCounter) {
			if(counter == null) {
				counter = getCounterResource(gw);
			}
			if(!counter.exists()) {
				ValueResourceHelper.setCreate(counter, value);							
			} else
				counter.getAndAdd(value);
		}
	}
	
	protected final static Map<String, PerformanceLog> knownLogs = new HashMap<>();
	public static interface GwSubResProvider {
		FloatResource getEventResource(MemoryTimeseriesPST device);
		default FloatResource getCounterResource(MemoryTimeseriesPST device) {
			throw new IllegalStateException("If useCounter=true, then getCounterResource must be overwritten!");
		}
	}
	public static PerformanceLog getInstance(boolean useCounter, ApplicationManager appMan, String key, GwSubResProvider prov) {
		PerformanceLog result = knownLogs.get(key);
		if(result != null)
			return result;
		result = new PerformanceLog(useCounter, appMan, key) {
			
			@Override
			protected FloatResource getEventResource(MemoryTimeseriesPST device) {
				return prov.getEventResource(device);
			}
			
			@Override
			protected FloatResource getCounterResource(MemoryTimeseriesPST device) {
				return prov.getCounterResource(device);
			}
		};
		return result;
	}
}
