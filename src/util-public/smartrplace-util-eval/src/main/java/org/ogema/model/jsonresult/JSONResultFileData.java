package org.ogema.model.jsonresult;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.prototypes.Data;

/** Information on JSON result files to be stored in the OGEMA system. Each Workspace shall be a set
 * of data with dependencies that works usually without outside dependencies. So it is is possible
 * to copy an entire workspace like an Excel file with several tables inside. Usually each workspace
 * contains three major subdirectories:<br>
 * - Temporary files that usually shall be clean up on every clean start or on every evaluation run.
 * These files also may be deleted by the Multi-Evaluation itself as they are just used to transfer
 * information between evaluation steps
 * - Exmperimental files that usually shall be cleaned up when a development process or
 *   an "experiment" is finished
 * - Major result files that shall be kept for further evaluations, documentation etc.*/
public interface JSONResultFileData extends Data {
	/** Absolute file path on the system. This information is only provided
	 * as an exception for performance optimization. Usually the path
	 * should be determined based on a root directory system property or
	 * a specific setting of the management instance, the workspace,
	 * the status (possibly) and
	 * the path relative to the workspace. In this way files and the
	 * respective resources (as OGEMA-JSON/XML-Export) can be transferred
	 * between systems easily.
	 */
	StringResource filePath();
	StringResource workSpace();
	/**  1: Temporary result to be deleted soon<br>
	 *  10: Experimental result, can also be deleted after experiment finished
	 *     if not transferred/marked as relevant
	 * 100: Result that can be used by further evaluations in the future and
	 *     shall not be deleted
	 */
	IntegerResource status();
	/** For smaller workspaces without a further sub-structure except
	 * separation in temporary/experimental/major results this is just the
	 * file name
	 */
	StringResource workSpaceRelativePath();
	StringArrayResource gatewaysIncluded();
	
	StringResource evaluationProviderId();
	StringResource evaluationProviderClassName();
	StringResource resultClassName();
	
	/** TODO: Offer both options here ?*/
	/** The list shall contain references on the data of the files used for
	 * pre-evaluation. Providing file names may be much more ambigious, but it
	 * should not be necessary to create a JSONResultFileData-resource for every
	 * file referenced here.
	 */
	ResourceList<JSONResultFileData> preEvaluationsUsed();
	StringArrayResource preEvaluationFilesUsed();
	
	TimeResource startTime();
	TimeResource endTime();
	TimeResource stepSize();
	/** If no gaps occur timeIntervalNum = (endTime - startTime)/stepSize*/
	IntegerResource timeIntervalNum();
}
