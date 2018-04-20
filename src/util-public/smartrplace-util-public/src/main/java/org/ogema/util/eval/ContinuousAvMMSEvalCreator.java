package org.ogema.util.eval;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.model.eval.EvaluationConfig;
import org.ogema.model.eval.base.EvaluationAvMMSConfig;

import de.iwes.tools.statistics.StatisticalAggregationProvider;
import de.iwes.tools.statistics.StatisticalMinMaxProvider;
import de.iwes.tools.statistics.StatisticsHelper;
import de.iwes.tools.statistics.StatisticalAggregationCallbackTemplate;

/**Class creates continuous evaluation.
 * Override initStatProvidersResources to create the output resources you actually need
 * TODO: std and rms not yet supported
 */
public class ContinuousAvMMSEvalCreator extends ContinuousEvalCreator<EvaluationAvMMSConfig> {
	@Override
	protected boolean initStatProvidersResources(int[] intervalTypes) {
		boolean activate = false;
		// Average
		if(super.initStatProvidersResources(intervalTypes)) activate = true;
		if(StatisticsHelper.initStatisticalAggregation(evalConfig.min(), intervalTypes))
			activate = true;
		if(StatisticsHelper.initStatisticalAggregation(evalConfig.max(), intervalTypes))
			activate = true;
		return activate;
	}
	
	StatisticalAggregationProvider avProvider;
	StatisticalMinMaxProvider minProvider;
	StatisticalMinMaxProvider maxProvider;
	@Override
	protected void initStatProviders() {
		minProvider = new StatisticalMinMaxProvider(null, evalConfig.min(),
				appMan, null, 1);
		providers.add(minProvider);
		maxProvider = new StatisticalMinMaxProvider(null, evalConfig.max(),
				appMan, null, 2);
		providers.add(maxProvider);
		//Average
		avProvider = new StatisticalAggregationProvider(dataSource, evalConfig.destinationStat(),
				appMan, new StatisticalAggregationCallbackTemplate() {
			@Override
			public float valueChanged(FloatResource value, FloatResource primaryAggregation, long valueEndInstant,
					long timeStep) {
				float val = value.getValue();
				if(val == 0) {
					System.out.println("Found value zero for pA:"+primaryAggregation.getValue()+" Loc:"+value.getLocation());
				}
				if(val < minProvider.primaryAggregation.getValue()) {
					minProvider.primaryAggregation.setValue(val);
				}
				if(val > maxProvider.primaryAggregation.getValue()) {
					maxProvider.primaryAggregation.setValue(val);
				}
				return value.getValue();
			}
		});
		providers.add(avProvider);
	}
	
	/**Find fitting {@link EvaluationConfig} in evalList or create it there*/
	public ContinuousAvMMSEvalCreator(String identifier, ResourceList<EvaluationAvMMSConfig> evalList,
			Class<? extends EvaluationAvMMSConfig> evalClass,
			String controllingApplication, 
			int[] intervalTypes, boolean startOnCreate,
			FloatResource dataSource, ApplicationManager appMan) {
		super(identifier, evalList, evalClass, controllingApplication, intervalTypes, startOnCreate, dataSource, appMan);
	}
	/**
	 * @param evalConfig
	 * @param controllingApplication
	 * @param intervalTypes
	 * @param startOnCreate if true the continuousOperationStateControl field will be set to true if it
	 * does not exist. If it exists already this parameter has no effect.
	 * @param dataSource TODO: the data source may not be a single FloatResource
	 * @param appMan
	 */
	public ContinuousAvMMSEvalCreator(EvaluationAvMMSConfig evalConfig, String controllingApplication, 
			int[] intervalTypes, boolean startOnCreate,
			FloatResource dataSource, ApplicationManager appMan) {
		super(evalConfig, controllingApplication, intervalTypes, startOnCreate, dataSource, appMan);
	}
}
