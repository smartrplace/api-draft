package org.ogema.devicefinder.api;

import org.ogema.devicefinder.util.TimedJobMemoryData;
import org.smartrplace.apps.eval.timedjob.TimedEvalJobConfig;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.template.LabelledItem;

/** For now services are registered via the datapoint service. For this reason no init and stop is foreseen*/
public interface TimedJobProvider extends LabelledItem {
	void execute(long now, TimedJobMemoryData data);
	
	/** If this is true then the job cannot be started again*/
	//Moved into TimedJobMemoryData
	//boolean isExecuting();
	
	/** If true the job implementation or the application registring the job can
	 * trigger the job directrly
	 */
	//default boolean canJobTriggerItself() {
	//	return false;
	//}
	
	/** Only relevant if {@link #canJobTriggerItself()} is true
	 */
	//default long lastTimeJobWasStarted() {
	//	return -1;
	//}
	
	/** 0: No evaluation job
	 *  1: standard evaluation job
	 */
	int evalJobType();
	
	/** Initialize resource. If evalJobType is positive then the resource is of type
	 * {@link TimedEvalJobConfig}
	 * @param config
	 * @return should return true. A return value of false is reserved for future use
	 */
	boolean initConfigResource(TimedJobConfig config);
	
	/** Change the result of this to trigger another call to {@link #initConfigResource(TimedJobConfig)}
	 * @return
	 */
	public String getInitVersion();

	@Override
	default String description(OgemaLocale locale) {
		return label(locale);
	}
	
	default void timerStartedNotification(TimedJobMemoryData data) {}
	default void timerStoppedNotification(TimedJobMemoryData data) {}
	
}
