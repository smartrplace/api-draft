package org.ogema.devicefinder.api;

import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.model.extended.alarming.AlarmConfiguration;

/** A listener object created via an AlarmingExtension for a certain {@link SingleValueResource}.
 * Note that the alarming must implement the information whether the alarm is active or not itself
 * including the generation of the information of a release.<br>
 * The standard value limit alarming is implemented in AlarmValueListenerBasic#resourceChanged, which
 * can be used as a reference. A retard for alarm generation is implemented there by the framework and
 * does not need to be implemented by the alarming itself.<br>
 * A release timer is especially helpful to avoid quick switching between
 * release and alarm status, but should not be necessary in most cases.
 *
 */
public interface AlarmingExtensionListener {
	public static interface AlarmResult {
		String message();
		/** Value to put into alarming*/
		int alarmValue();
		/** A release must contain a suitable
		 * message and an alarmValue of zero. This information is used to avoid sending out alarms withing the
		 * retard period when a release is coming first.*/
		boolean isRelease();
		
		/** duration between first alarm detection and sending the alarm. If null then the
		 * {@link AlarmConfiguration#maxViolationTimeWithoutAlarm()} value is used.*/
		default Long retard() {return null;}
	}
	/** Check if a new value triggers an alarm. The method is called each time a new value
	 * is received on the triggerin SingleValueResource
	 * 
	 * @param <T>
	 * @param resource SingleValueResource changed
	 * @return null if no alarm applies, otherwise return the String for the alarming message
	 */
	<T extends SingleValueResource> AlarmResult resourceChanged(T resource, float value, long now);
	
	default String id() {return this.getClass().getName();}

	AlarmingExtension sourceExtension();
}
