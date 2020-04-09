/**
 * ﻿Copyright 2014-2018 Smartrplace and Fraunhofer-Gesellschaft zur Förderung der angewandten Wissenschaften e.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.timeseries.luthmon.quality;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.widgets.configuration.service.OGEMAConfigurations;
import org.smartrplace.timeseries.tsquality.QualityEvalProviderBase;

import de.iwes.timeseries.eval.api.EvaluationInput;
import de.iwes.timeseries.eval.api.EvaluationInstance.EvaluationListener;
import de.iwes.timeseries.eval.api.EvaluationProvider;
import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.SingleEvaluationResult;
import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance;
import de.iwes.timeseries.eval.base.provider.utils.SingleValueResultImpl;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoEvaluationCore;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoResultType;

/**
 * Evaluate basic time series qualities per gateway including gap evaluation
 */
@Service(EvaluationProvider.class)
@Component
public class DefaultQualityEvalProvider extends QualityEvalProviderBase {
	/** Adapt these values to your provider*/
    public final static String ID = "adefault-quality_eval_provider";
    public final static String LABEL = "Default Quality: Gap evaluation provider";
    public final static String DESCRIPTION = "Default Quality: Provides gap evaluation, additional information in log file";
	public static final String TS_TOTAL_PROP = "%TS_TOTAL";
	public static final String SENSORS_TO_FILTEROUT_PROP = "%SENS_FILTER";
    
	@SuppressWarnings("unchecked")
	@Override
	protected List<String> getSensorsToFilerOut(String currentGwId) {
		Object dataTouse = OGEMAConfigurations.getObject(DefaultQualityEvalProvider.class.getName(), SENSORS_TO_FILTEROUT_PROP, currentGwId);
		if(dataTouse != null && dataTouse instanceof List)
			return (List<String>)dataTouse;
		return super.getSensorsToFilerOut(currentGwId);
	}
	
    public DefaultQualityEvalProvider() {
        super(ID, LABEL, DESCRIPTION);
    }
	
	/**
 	 * Define the results of the evaluation here including the final calculation
 	*/

    public final static GenericGaRoResultType TS_TOTAL = new GenericGaRoResultType("TS_TOTAL",
    		"Number of standard time series", IntegerResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			int num;
			Object dataTouse = OGEMAConfigurations.getObject(DefaultQualityEvalProvider.class.getName(), TS_TOTAL_PROP, cec.currentGwId);
			if(dataTouse != null && dataTouse instanceof Integer) {
				num = (Integer)dataTouse;
			} else {
				num = cec.tsNum;
			}
			return new SingleValueResultImpl<Integer>(rt, num, inputData);
		}
    };

   private static final List<GenericGaRoResultType> RESULTS = Arrays.asList(TS_TOTAL, TS_WITH_DATA, TS_GOOD, TS_GOLD,
		  OVERALL_GAP_REL,
		  GAP_TIME_REL, ONLY_GAP_REL, TOTAL_TIME, DURATION_TIME, GAP_SENSORS); //EVAL_RESULT
    
	@Override
	protected List<GenericGaRoResultType> resultTypesGaRo() {
		return RESULTS;
	}

	@Override
	protected GenericGaRoEvaluationCore initEval(List<EvaluationInput> input, List<ResultType> requestedResults,
			Collection<ConfigurationInstance> configurations, EvaluationListener listener, long time, int size,
			int[] nrInput, int[] idxSumOfPrevious, long[] startEnd) {
		return new EvalCore(input, requestedResults, configurations, listener, time, size, nrInput, idxSumOfPrevious, startEnd);
	}
}
