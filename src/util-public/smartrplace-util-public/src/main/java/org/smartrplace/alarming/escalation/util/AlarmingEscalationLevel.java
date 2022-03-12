package org.smartrplace.alarming.escalation.util;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.prototypes.Data;

public interface AlarmingEscalationLevel extends Data {
	/** Alarming app by which the configuration is sent. Together with {@link #alarmLevel()} this
	 * determines which receivers get high, medium and low priority alarms. Each alarming level must
	 * have a different combination of levelAppId and alarmLevel.
	 */
	StringResource levelAppId();
	/** If an alarm is detected and the alarmStatus resource is active then the value given
	 * here is written into the alarmStatus resource. For no-value alarms the level plus 1000 is used.<br>
	 * The alarm level is also used to determine the message priority:<br>
	 * 1: LOW<br>
	 * 2: MEDIUM<br>
	 * 3: HIGH
	 */
	IntegerResource alarmLevel();
	
	/** The higher the priority the later the level is informed. Standard priorities may be defined in the
	 * future.*/
	IntegerResource priority();
	
	/** The real delay is decided by the {@link EscalationProvider}, but a standard delay may be set here.*/
	TimeResource standardDelay();
	
	/** The following is based on TimedJobs: Each connected {@link EscalationProvider} can be executed
	 * on defined aligned times or at a defined interval after the alarm was detected. It can also
	 * be checked after each startup.
	 * See AbsoluteTiming for values. If positive and active then the job is executed at each aligned interval plus the
	 * the time defined by interval()*/
	IntegerResource alignedInterval();
	
	/** If alignedInterval is inactive or zero/negative then the job is executed after each interval without alignment
	 * provided in MINUTES. If the value is shorter than MINIMUM_MINUTES_FOR_TIMER_START then timer cannot be activated,
	 * but call on startup will be executed if configured.*/
	FloatResource interval();
	
	/** If active and zero or positive the operation shall be executed once after startup with a delay of milliseconds
	 * indicated by this element. The delay can be used to give other components time to start up.<br>
	 * Provided in MINUTES.
	 */
	FloatResource performOperationOnStartUpWithDelay();
}
