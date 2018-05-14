package org.ogema.util.jsonresult.management;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.model.jsonresult.JSONResultFileData;

import de.iwes.timeseries.eval.api.extended.MultiResult;
import de.iwes.util.resource.ResourceHelper;

public interface EvalResultManagement {
	
	/** Register a result class so that files that store a result of this class can be opened
	 * by the management
	 */
	void registerClass(Class<? extends MultiResult> resultStructure);
	
	/** Read file to get all the data in the resource*/
	JSONResultFileData createFileInfo(String fileNameWithPath);
	JSONResultFileData createFileInfo(int status, String workSpaceRelativePath);
	
	/** Usually a management implementation will setup a ResourceList for each workspace
	 * where all result file descriptors are collected. This list can be obtained here. If
	 * for some reason a different structure is required the method may return null and
	 * a another custom method would have to be used. 
	 */
	ResourceList<JSONResultFileData> getWorkspaceFileInfo();
	
	/** Create or update list of file results in a workspace
	 * 
	 * @param referenceExperimental if true also entries for experimental files will be made,
	 * 		otherwise only major results are referenced. Temporary results are never
	 *      referenced (this could be done for single files via {@link #createFileInfo(String)}.
	 * @param deleteExisting only relevant if an existing ResourceList is provided. If true
	 * 		all entries for files not found anymore or that would not be included based on the
	 *      parameters of the call will be deleted.
	 * @return true if the resource structure was newly created or changed, false if no changes
	 * 		were made
	 */
	boolean updateWorkspaceFileInfo(boolean referenceExperimental, boolean deleteExisting);
	
	/** Read from JSON assuming that the respective result class has been registered before*/
	<M extends MultiResult> M importFromJSON(JSONResultFileData fileData);
	
	/** 
	 *  
	 * @param fileNameWithPath full system file path
	 * @param structure
	 * @return
	 */
	<M extends MultiResult> M importFromJSON(String fileNameWithPath, String resultClassName);
	
	/** Save a result into a file and create a file descriptor resource.
	 * 
	 * @param result result to save. The Class of the result will be registered automatically if
	 * 		not yet known
	 * @param status
	 * @param baseFileName
	 * @param overwriteIfExisting if true the baseFileName will be used to append .json if not yet there,
	 *  		but no further changes will be made. If false (default) a number will be appended to
	 *  		make the file name unique and avoid overwriting. If overwriting takes places an
	 *  		existing JSONResultFileData object shall be deleted for the
	 *  		file overwritten
	 * @param fileDataList
	 * @return if status is not temporary the result resource will be added to the fileDataList,
	 * 			otherwise it is added as general temporary resource (see {@link ResourceHelper#getSampleResource(Class)})
	 */
	<M extends MultiResult> JSONResultFileData saveResult(M result, int status,
			String baseFileName, boolean overwriteIfExisting,
			List<JSONResultFileData> preEvaluationsUsed);
	
	/**If workspace is set the workspace argument in all methods can be left null and this 
	 * workspace will be used as default. Custom implementations of the EvalResultManagement may
	 * also be fixed to a certain workspace, here this method would not have any effect.<br>
	 * If the ResourceList for the result descriptors for the workspace is not available yet and/or
	 * the system file folder structure is not available yet it shall be created.*/
	void setWorkspace(String workspace);
	
	/** Export workspace resource data, especially ResourceList with JSONResultFileData,
	 * into JSON or XML file
	 * @param destinationFilePath if null the base workspace folder for JSON files will be used
	 * @return true if success
	 */
	boolean exportWorkspaceFileData(String destinationFilePath, boolean exportAsXML);
	Resource importWorkspaceFileData(String sourceFilePath);
}
