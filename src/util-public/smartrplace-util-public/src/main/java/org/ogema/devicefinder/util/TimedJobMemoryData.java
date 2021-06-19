package org.ogema.devicefinder.util;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resourcemanipulator.timer.CountDownAbsoluteTimer;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;

import de.iwes.util.timer.AbsolutePersistentTimer;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTimerListener;

/** Data for a TimedJob that is not stored persistently*/
public class TimedJobMemoryData {
	public static final float MINIMUM_MINUTES_FOR_TIMER_START = 5;
	
	public long lastRunStart() {
		return lastRunStart;
	}

	public long lastRunDuration() {
		return lastRunDuration;
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
	protected volatile long lastRunDuration = 0;
	protected volatile long nextScheduledStart = 0;
	protected volatile boolean isRunning;
	
	protected TimedJobConfig res;
	protected TimedJobProvider prov;
	
	protected Timer timerUnaligned = null;
	protected AbsolutePersistentTimer timerAligned = null;
	
	private final ApplicationManager appMan;
	
	public TimedJobMemoryData(ApplicationManager appMan) {
		this.appMan = appMan;
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
		isRunning = true;
		lastRunStart = appMan.getFrameworkTime();
		prov.execute(lastRunStart, this);
		isRunning = false;
		long finish = appMan.getFrameworkTime();
		lastRunDuration = finish - lastRunStart;
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
