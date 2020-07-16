package org.ogema.devicefinder.api;

import org.ogema.core.model.simple.SingleValueResource;

/** A listener object created via an AlarmingExtension for a certain {@link SingleValueResource}
 *
 */
public interface AlarmingExtensionListener {
	public static interface AlarmResult {
		String message();
		/** Value to put into alarming*/
		int alarmValue();
		/** Not really used yet. For now we issue releases by sending a suitable
		 * message and an alarmValue of zero*/
		boolean isRelease();
	}
	/** Check if a new value triggers an alarm. The method is called each time a new value
	 * is received on the triggerin SingleValueResource
	 * 
	 * @param <T>
	 * @param resource SingleValueResource changed
	 * @return null if no alarm applies, otherwise return the String for the alarming message
	 */
	<T extends SingleValueResource> AlarmResult resourceChanged(T resource, float value, long now);
}
