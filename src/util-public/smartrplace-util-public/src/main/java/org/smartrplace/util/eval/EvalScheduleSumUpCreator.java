package org.smartrplace.util.eval;

import java.io.Closeable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.tools.resourcemanipulator.timer.CountDownAbsoluteTimer;

import de.iwes.util.timer.AbsolutePersistentTimer;
import de.iwes.util.timer.AbsoluteTimerListener;

/**Aggregate a list of evaluation results into a new result.
  */
public class EvalScheduleSumUpCreator implements AbsoluteTimerListener, Closeable {
	final private int mode;
	final private Schedule destination;
	
	private final AbsolutePersistentTimer timer;
	private final ApplicationManager appMan;
	
	public class SchedUpdateInfo {
		//boolean currentDataFound;
		long lastTimestampFound = 0;
	}
	protected final Map<Schedule, SchedUpdateInfo> sources = new HashMap<>();
	long currentAlignedTime;
	
	/**
	 * @param mode aggregation mode:
	 * 1: minimum<br>
	 * 2: maximum<br>
	 * 3: average<br>
	 * 4: integral<br>
	 * 5: derivation (not supported yet)
	 * @param destination schedule to write into
	 * @param sources initial sources, if null initialized without sources
	 * @param intervalType
	 * @param lastCallback time resource provided to store last callback information for
	 * 		AbsoluteTimer
	 * @param appMan
	 * @param instantInterval if positive the scheduleHolder Resource is updated with this interval
	 */
	public EvalScheduleSumUpCreator(int mode, Schedule destination, List<Schedule> sources, 
			int intervalType, TimeResource lastCallback, ApplicationManager appMan) {
		super();
		this.mode = mode;
		this.destination = destination;
		this.appMan = appMan;
		if(sources != null) {
			for(Schedule s: sources) {
				addSchedule(s);
			}
		}
		timer = new AbsolutePersistentTimer(lastCallback, intervalType, this, appMan);
	}

	public class ScheduleListener implements ResourceValueListener<Schedule> {
		@Override
		public void resourceChanged(Schedule resource) {
			SchedUpdateInfo sui = sources.get(resource);
			if(sui != null) sui.lastTimestampFound = resource.getLastUpdateTime();
			
			checkInputData();
		}
	}
	ScheduleListener schedListener = new ScheduleListener();
	
	private boolean checkInputData() {
		for(SchedUpdateInfo sui2:sources.values()) {
			if(sui2.lastTimestampFound < (currentAlignedTime-500)) return false;
		}
		//we got all values, we can transfer now
		float sumUp = (new SimpleAggregation<Schedule>() {
			@Override
			protected float getValue(Schedule element) {
				SampledValue rawVal = element.getValue(currentAlignedTime);
				if(rawVal != null)
					return element.getValue(currentAlignedTime).getValue().getFloatValue();
				else return 0;
			}
		}).getAggregatedValue(sources.keySet(), mode);
		destination.addValue(currentAlignedTime, new FloatValue(sumUp));
		return true;
	}
	
	public boolean addSchedule(Schedule schedule) {
		schedule.addValueListener(schedListener, false);
		if(sources.containsKey(schedule)) return false;
		sources.put(schedule, new SchedUpdateInfo());
		return true;
	}
	public boolean removeSchedule(Schedule schedule) {
		schedule.removeValueListener(schedListener);
		return (sources.remove(schedule)!=null);
	}
	@Override
	public void timerElapsed(CountDownAbsoluteTimer myTimer, long absoluteTime, long timeStep) {
		currentAlignedTime = absoluteTime;
		final long maxEnd = System.currentTimeMillis()+30000;
		
		appMan.createTimer(1000, new TimerListener() {
			@Override
			public void timerElapsed(Timer timer) {
				if(checkInputData() || (System.currentTimeMillis() >= maxEnd)) timer.destroy();
			}
		});
		// FIXME blocks the app thread
		//while(System.currentTimeMillis() < maxEnd) {
		//	try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
		//}
	}
	
	@Override
	public void close() {
		timer.stop();
	}

	public FloatResource getDestinationParent() {
		return destination.getParent();
	}
}
