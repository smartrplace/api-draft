package org.smartrplace.smarteff.access.api;

import java.util.List;

/** Via the EvalButtonConfigService buttons can be defined that shall be placed on 
 * evaluation overview pages. For now only buttons opening the timeseries viewer expert
 * are foreseen.
 */
public interface EvalButtonConfigService {
	List<EvalButtonConfig> configurations();
}
