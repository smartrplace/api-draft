package org.ogema.devicefinder.api;

import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;

/** Data for a TimedJob that is not stored persistently*/
public interface TimedJobMemoryData {
	public static final float MINIMUM_MINUTES_FOR_TIMER_START = 2.5f;
	public static final long LOAD_REPORT_INTERVAL = 1*TimeProcUtil.MINUTE_MILLIS;
	public static final long MIN_FREE_MEMORY_MB = Long.getLong("org.ogema.devicefinder.util.minfreemb", 200);
	
	public long lastRunStart();

	public long lastRunDuration();

	public long lastRunEnd();

	public long maxRunDuration();

	public long nextScheduledStart();

	/** The {@link TimedJobMgmtService} makes sure that each job cannot be triggered again while it is running.
	 * It will also not be queued then. If minimum gaps are required between starting a job
	 * (e.g. for jobs performing bundle restarts) then the job has to implement such a
	 * supervision itself. See BundleRestartButton as an example.*/
	public boolean isRunning();

	public boolean triggeredForExecutionOnceOutsideTime();
	public TimedJobConfig res();

	public TimedJobProvider prov();

	public boolean executeBlockingOnceOnYourOwnRisk();
	public boolean executeNonBlockingOnce();
	public int startTimerIfNotStarted();
	
	public int restartTimer();
	public void stopTimerIfRunning();
	public boolean isTimerActive();
	
	/** Check timer settings*/
	public boolean canTimerBeActivated();
}
