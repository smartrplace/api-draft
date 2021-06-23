package org.ogema.devicefinder.util;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.ogema.devicefinder.util.TimedJobMgmtServiceImpl.TimedJobMgmtData;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resourcemanipulator.timer.CountDownAbsoluteTimer;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsolutePersistentTimer;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTimerListener;

/** Data for a TimedJob that is not stored persistently*/
public class TimedJobMemoryData {
	public static final float MINIMUM_MINUTES_FOR_TIMER_START = 2.5f;
	public static final long LOAD_REPORT_INTERVAL = 5*TimeProcUtil.MINUTE_MILLIS;
	
	public long lastRunStart() {
		return lastRunStart;
	}

	public long lastRunDuration() {
		return lastRunDuration;
	}

	public long maxRunDuration() {
		return maxRunDuration;
	}

	public long nextScheduledStart() {
		return nextScheduledStart;
	}

	public boolean isRunning() {
		return isRunning;
	}

	public boolean triggeredForExecutionOnceOutsideTime() {
		return myThreadForRunningOnceFromOutside != null;
	}
	public TimedJobConfig res() {
		return res;
	}

	public TimedJobProvider prov() {
		return prov;
	}

	protected volatile long lastRunStart = -1;
	protected volatile long lastRunEnd;
	
	protected volatile long lastRunDuration = 0;
	protected volatile long maxRunDuration = 0;
	protected volatile long nextScheduledStart = 0;
	
	protected volatile long executionTimeCounter = 0;
	protected volatile long freeTimeCounter = 0;
	protected volatile long lastLoadReport = -1;
	
	protected volatile boolean isRunning;
	
	protected TimedJobConfig res;
	protected TimedJobProvider prov;
	protected final TimedJobMgmtData jobData;
	
	protected Timer timerUnaligned = null;
	protected AbsolutePersistentTimer timerAligned = null;
	
	private final ApplicationManager appMan;
	
	public TimedJobMemoryData(ApplicationManager appMan, TimedJobMgmtData jobData) {
		this.appMan = appMan;
		this.jobData = jobData;
		long now = appMan.getFrameworkTime();
		lastRunEnd = now;
		lastLoadReport = now;
	}

	boolean isAligned;
	int align;
	long interval;
	protected boolean executeBlockingOnceFromTimer() {
		nextScheduledStart = getNextScheduledStartIfExecutingNow(isAligned, align, interval);
		return executeBlockingOnce();
	}
	//Should not called directly from the outside
	protected boolean executeBlockingOnce() {
		if(isRunning())
			return false;
		lastRunStart = appMan.getFrameworkTime();
		long lastFreeTime = lastRunStart - lastRunEnd;
		freeTimeCounter += lastFreeTime;
		isRunning = true;
		prov.execute(lastRunStart, this);
		isRunning = false;
		ValueResourceHelper.setCreate(jobData.logResource.jobIdxStarted(), res.persistentIndex().getValue());
		lastRunEnd = appMan.getFrameworkTime();
		lastRunDuration = lastRunEnd - lastRunStart;
		if(lastRunDuration > maxRunDuration)
			maxRunDuration = lastRunDuration;
		executionTimeCounter += lastRunDuration;
		ValueResourceHelper.setCreate(jobData.logResource.jobDuration(), lastRunDuration);
		if(lastRunEnd - lastLoadReport > LOAD_REPORT_INTERVAL) {
			float load = (float) (((double)executionTimeCounter)/(executionTimeCounter+freeTimeCounter));
			ValueResourceHelper.setCreate(jobData.logResource.jobLoad(), load);
			lastLoadReport = lastRunEnd;
		}
		return true;
	}
	
	//protected volatile Thread myThread = null;
	protected volatile CountDownDelayedExecutionTimer myThreadForRunningOnceFromOutside = null;
	public boolean executeNonBlockingOnce() {
		
		if(isRunning())
			return false;
		if(myThreadForRunningOnceFromOutside != null)
			return false;
		myThreadForRunningOnceFromOutside = new CountDownDelayedExecutionTimer(appMan, 1) {
			
			@Override
			public void delayedExecution() {
		        try {
		        	executeBlockingOnce();
		        } catch(Exception e) {
		            e.printStackTrace();
		        } finally {
					myThreadForRunningOnceFromOutside = null;
				}
		    }  
		};
		return true;

		/*synchronized(this) {
			if(myThread != null)
				return false;
			myThread = new Thread() {
			    public void run() {
			        try {
			        	executeBlockingOnce();
			        } catch(Exception e) {
			            e.printStackTrace();
			        } finally {
						myThread = null;
					}
			    }  
			};
			myThread.start();
			return true;
		}*/
	}
	
	/** Start timed operation
	 * @param appMan
	 * @return error code: 0:no error, 10: disabled, 20: failed
	 */
	public int startTimerIfNotStarted() {
		if(isTimerActive())
			return 0;
		return restartTimer();
	}
	
	public int restartTimer() {
		stopTimerIfRunning();
		
		if(res.disable().getValue()) {
			return 10;
		}
		
		//If settings are not suitable for startup, we just perform initial run if configured
		final long nextScheduledStartWithoutStart;
		if(canTimerBeActivated()) {
			interval = (long) (res.interval().getValue()*TimeProcUtil.MINUTE_MILLIS);
			align = res.alignedInterval().getValue();
			isAligned = (align > 0);
			if(!isAligned) {
				timerUnaligned = appMan.createTimer(interval, new TimerListener() {
					
					@Override
					public void timerElapsed(Timer timer) {
						executeBlockingOnceFromTimer();
					}
				});
			} else {
				timerAligned = new AbsolutePersistentTimer(res.lastStartStorage(), align,
						new AbsoluteTimerListener() {
							
							@Override
							public void timerElapsed(CountDownAbsoluteTimer myTimer, long absoluteTime, long timeStep) {
								if(interval > 0) {
									new CountDownDelayedExecutionTimer(appMan, interval) {
										@Override
										public void delayedExecution() {
											executeBlockingOnceFromTimer();
										}
									};
								} else
									executeBlockingOnceFromTimer();
								
							}
						}, appMan);
			}
			nextScheduledStartWithoutStart = getNextScheduledStartIfExecutingNow(isAligned, align, interval);
			
			prov.timerStartedNotification(this);
		} else
			nextScheduledStartWithoutStart = -1;
		
		float startup = res.performOperationOnStartUpWithDelay().getValue();
		if(startup >= 0) {
			long delay = (long) (startup*TimeProcUtil.MINUTE_MILLIS);
			if(delay < 1)
				delay = 1;
			nextScheduledStart = appMan.getFrameworkTime() + delay;
			new CountDownDelayedExecutionTimer(appMan, delay) {
				@Override
				public void delayedExecution() {
					nextScheduledStart = nextScheduledStartWithoutStart;
					executeBlockingOnce();
				}
			};
		} else
			nextScheduledStart = nextScheduledStartWithoutStart;

		return 0;
	}
	
	public void stopTimerIfRunning() {
		if(timerAligned != null) {
			timerAligned.stop();
			prov.timerStoppedNotification(this);
			timerAligned = null;
		}
		if(timerUnaligned != null) {
			timerUnaligned.stop();
			timerUnaligned.destroy();
			prov.timerStoppedNotification(this);
			timerUnaligned = null;
		}		
	}
	
	public boolean isTimerActive() {
		return (timerAligned != null || timerUnaligned != null);
	}
	
	/** Check timer settings*/
	public boolean canTimerBeActivated() {
		int alignLoc = res.alignedInterval().getValue();
		if(alignLoc > 0)
			return true;
		float intervalLoc = res.interval().getValue();
		if(intervalLoc >= MINIMUM_MINUTES_FOR_TIMER_START)
			return true;
		return false;
	}
	
	protected long getNextScheduledStartIfExecutingNow(boolean isAligned, int align, long interval) {
		long now = appMan.getFrameworkTime();
		if(isAligned) {
			return AbsoluteTimeHelper.getNextStepTime(now, align)+interval;
		} else
			return now+interval;
	}
	
	@Override
	public String toString() {
		try {
			return prov.id()+":"+prov.label(null);
		} catch(Exception e) {
			if(res != null)
				return "WOjob:"+res.getLocation();
			else
				return "WOres:"+super.toString();
		}
	}
}
