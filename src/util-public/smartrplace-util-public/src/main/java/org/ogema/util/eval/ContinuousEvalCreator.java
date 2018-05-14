package org.ogema.util.eval;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.model.eval.EvaluationConfig;

import de.iwes.tools.statistics.ComplexStatisticalAggregationProvider;
import de.iwes.tools.statistics.IntegratingStatProvider;
import de.iwes.tools.statistics.StatisticalAggregationProvider;
import de.iwes.tools.statistics.StatisticalProvider;
import de.iwes.tools.statistics.StatisticalProviderMeterCount;
import de.iwes.tools.statistics.StatisticsHelper;
import de.iwes.util.resource.ValueResourceHelper;

/**Class creates continuous evaluation.
 * TODO: Add support for review-evaluation like in de.iwes.tools.statisticsreview
 */
public abstract class ContinuousEvalCreator<T extends EvaluationConfig> {
	protected boolean actionActive = false;
	public BooleanResource stateControl;
	public ResourceValueListener<BooleanResource> controlListener = null;
	public OgemaLogger log = null;
	final protected ApplicationManager appMan;
	final protected T evalConfig;
	
	/** This field is used for the sample initStatProviders implementation and may be used as input field also
	 * for inherited implementations */
	final protected FloatResource dataSource;
	
	protected List<StatisticalProvider> providers = new ArrayList<>();
	
	/** override this if you need more/other {@link StatisticalAggregationProvider}s,
	 * {@link ComplexStatisticalAggregationProvider}s and/or {@link IntegratingStatProvider}s or even
	 * {@link StatisticalProviderMeterCount}
	 * @return true if evalConfig was changed and shall be activated
	 */
	protected boolean initStatProvidersResources(int[] intervalTypes) {
		return StatisticsHelper.initStatisticalAggregation(evalConfig.destinationStat(), intervalTypes);
	}
	protected void initStatProviders() {
		providers.add(new StatisticalAggregationProvider(dataSource, evalConfig.destinationStat(),
				appMan, null));
	}
	
	/**Find fitting {@link EvaluationConfig} in evalList or create it there*/
	public ContinuousEvalCreator(String identifier, ResourceList<T> evalList,
			Class<? extends T> evalClass,
			String controllingApplication, 
			int[] intervalTypes, boolean startOnCreate,
			FloatResource dataSource, ApplicationManager appMan) {
		T evalConfig = null;
		for(T ec: evalList.getAllElements()) {
			if(ec.id().getValue().equals(identifier)) {
				evalConfig = ec;
				break;
			}
		}
		boolean activate = false;
		if(evalConfig == null) {
			evalConfig = evalList.add(evalClass);
			evalConfig.id().<StringResource>create().setValue(identifier);
			activate = true;
		}
		this.appMan = appMan;
		this.evalConfig = evalConfig;
		this.dataSource = dataSource;
		init(evalConfig, controllingApplication, intervalTypes, startOnCreate, activate, dataSource, appMan);
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
	public ContinuousEvalCreator(T evalConfig, String controllingApplication, 
			int[] intervalTypes, boolean startOnCreate,
			FloatResource dataSource, ApplicationManager appMan) {
		this.appMan = appMan;
		this.evalConfig = evalConfig;
		this.dataSource = dataSource;
		init(evalConfig, controllingApplication, intervalTypes, startOnCreate, false, dataSource, appMan);
	}
	private void init(EvaluationConfig evalConfig, String controllingApplication, 
			int[] intervalTypes, boolean startOnCreate, boolean activateForSure,
			FloatResource dataSource, ApplicationManager appMan) {
		this.log = appMan.getLogger();
		boolean activate = initStatProvidersResources(intervalTypes);
		if((activateForSure || activate) | ValueResourceHelper.setIfNew(evalConfig.continuousOperationStateControl(), startOnCreate) |
				ValueResourceHelper.setIfNew(evalConfig.controllingApplication(), controllingApplication)) {
			evalConfig.activate(true);
		}
		stateControl = evalConfig.continuousOperationStateControl();
		if (stateControl.getValue()) startEvaluation();
		if (controlListener == null) {
			controlListener = new ResourceValueListener<BooleanResource>() {
				@Override
				public void resourceChanged(BooleanResource arg0) {
					if((!actionActive) && arg0.getValue()) {
						startEvaluation();
					} else {
						//stop evaluation not yet supported
					}
				}
			};
			stateControl.addValueListener(controlListener, true);
		}
	}
	protected void startEvaluation() {
		actionActive = true;
		initStatProviders();
	}
	
	public T getEvalResource() {
		return evalConfig;
	}
	
	public void close() {
		stateControl.removeValueListener(controlListener);
		for(StatisticalProvider sp: providers) {
			sp.close();
		}
	}
}
