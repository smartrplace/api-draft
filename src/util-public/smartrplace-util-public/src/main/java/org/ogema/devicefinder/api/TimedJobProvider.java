package org.ogema.devicefinder.api;

import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;
import org.smartrplace.apps.eval.timedjob.TimedEvalJobConfig;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.template.LabelledItem;

/** For now services are registered via the datapoint service. For this reason no init and stop is foreseen*/
public interface TimedJobProvider extends LabelledItem {
	void execute(long now, TimedJobMemoryData data);
	
	/** 0: No evaluation job
	 *  1: standard evaluation job
	 *  2: mass automation job (automation job type for which may be registered a lot of instances, mostly processed like an evaluation job)
	 */
	int evalJobType();
	
	/** Initialize resource. If evalJobType is positive then the resource is of type
	 * {@link TimedEvalJobConfig}
	 * @param config
	 * @return should return true. A return value of false is reserved for future use
	 */
	boolean initConfigResource(TimedJobConfig config);
	
	/** Change the result of this to trigger another call to {@link #initConfigResource(TimedJobConfig)}
	 * If "XXX" is returned then initConfigResource is called on every startup and no initversion control is used
	 * @return may be null or empty if only initial call is requested
	 */
	public String getInitVersion();

	@Override
	default String description(OgemaLocale locale) {
		return label(locale);
	}
	
	default void timerStartedNotification(TimedJobMemoryData data) {}
	default void timerStoppedNotification(TimedJobMemoryData data) {}
	
	/** Overwrite this for evaluation jobs that may register several providers for various datapoints*/
	default String getType() {
		if(evalJobType() == 0)
			return "BASE_DEFAULT";
		else if(evalJobType() == 1)
			return "EVAL_DEFAULT";
		return "TIMED_"+evalJobType()+"_DEFAULT";
	}
	
	default ProcessedReadOnlyTimeSeries3 getEvaluationContext() {return null;}
}
