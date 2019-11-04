package org.smartrplace.rexometer.driver.emoncms.task;

import java.util.concurrent.Callable;

import org.smartrplace.rexometer.driver.emoncms.EmonCMSDriver;

public abstract class EmonCMSTask implements Callable<EmonCMSTask>, Comparable<EmonCMSTask> {

	private volatile long nextExec;
	
	public long getNextExec() {
		return nextExec;
	}
	
	public void setNextExec(long t) {
		nextExec = t;
	}
	
	public long getInterval() {
		return EmonCMSDriver.MIN_STEP;
	};
	
	public int getFieldId() {
		return Integer.MAX_VALUE;
	}

	@Override
	public int compareTo(EmonCMSTask o) {
		return Long.compare(nextExec, o.nextExec);
	}

	
}
