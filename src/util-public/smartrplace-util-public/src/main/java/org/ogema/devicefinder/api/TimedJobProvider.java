package org.ogema.devicefinder.api;

import org.smartrplace.apps.eval.timedjob.TimedEvalJobConfig;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.template.LabelledItem;

/** For now services are registered via the datapoint service. For this reason no init and stop is foreseen*/
public interface TimedJobProvider extends LabelledItem {
	void execute(long now);
	
	/** If this is true then the job cannot be started again*/
	boolean isRunning();
	
	/** If true the job implementation or the application registring the job can
	 * trigger the job directrly
	 */
	default boolean canJobTriggerItself() {
		return false;
	}
	
	/** Only relevant if {@link #canJobTriggerItself()} is true
	 */
	default long lastTimeJobWasStarted() {
		return -1;
	}
	
	/** 0: No evaluation job
	 *  1: standard evaluation job
	 */
	int evalJobType();
	
	/** Initialize resource. If evalJobType is positive then the resource is of type
	 * {@link TimedEvalJobConfig}
	 * @param config
	 * @return true if config was successful, otherwise the service shall not be used
	 */
	boolean initConfigResource(TimedJobConfig config);
	
	@Override
	default String description(OgemaLocale locale) {
		return label(locale);
	}
}
