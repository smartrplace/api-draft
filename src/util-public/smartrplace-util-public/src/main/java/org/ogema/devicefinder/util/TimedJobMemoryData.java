package org.ogema.devicefinder.util;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;

/** Data for a TimedJob that is not stored persistently*/
public class TimedJobMemoryData {
	public long lastRunStart = -1;
	public long lastRunDuration = 0;
	
	public TimedJobConfig res;
	public TimedJobProvider prov;
	
	public boolean executeBlocking(ApplicationManager appMan) {
		if(prov.isRunning())
			return false;
		lastRunStart = appMan.getFrameworkTime();
		prov.execute(lastRunStart);
		long finish = appMan.getFrameworkTime();
		lastRunDuration = finish - lastRunStart;
		return true;
	}
	
	protected volatile Thread myThread = null;
	public boolean executeNonBlocking(ApplicationManager appMan) {
		synchronized(this) {
			if(myThread != null)
				return false;
			myThread = new Thread() {
			    public void run() {
			        try {
			        	executeBlocking(appMan);
			        } catch(Exception e) {
			            e.printStackTrace();
			        } finally {
						myThread = null;
					}
			    }  
			};
			myThread.start();
			return true;
		}
	}
}
