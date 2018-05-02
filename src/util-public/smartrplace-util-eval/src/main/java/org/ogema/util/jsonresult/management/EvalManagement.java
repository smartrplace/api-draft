package org.ogema.util.jsonresult.management;

import java.util.List;

import org.ogema.model.jsonresult.JSONResultFileData;

import de.iwes.timeseries.eval.api.extended.MultiResult;

public interface EvalManagement extends EvalResultManagement {
	/** Should we better return the JSONResultFileData?*/
	<M extends MultiResult> M performEvaluationAndSave(int status,
			String baseFileName, boolean overwriteIfExisting,
			List<JSONResultFileData> preEvaluationsToUse);
	
	/**Perform/update also PreEvaluations*/
}
