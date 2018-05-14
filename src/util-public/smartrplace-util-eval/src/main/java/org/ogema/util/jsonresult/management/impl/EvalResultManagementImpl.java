package org.ogema.util.jsonresult.management.impl;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.model.jsonresult.JSONResultFileData;
import org.ogema.util.jsonresult.management.EvalResultManagement;

import de.iwes.timeseries.eval.api.extended.MultiResult;

public class EvalResultManagementImpl implements EvalResultManagement {

	@Override
	public void registerClass(Class<? extends MultiResult> resultStructure) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public JSONResultFileData createFileInfo(String fileNameWithPath) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public JSONResultFileData createFileInfo(int status, String workSpaceRelativePath) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceList<JSONResultFileData> getWorkspaceFileInfo() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean updateWorkspaceFileInfo(boolean referenceExperimental, boolean deleteExisting) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public <M extends MultiResult> M importFromJSON(JSONResultFileData fileData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <M extends MultiResult> M importFromJSON(String fileNameWithPath, String resultClassName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <M extends MultiResult> JSONResultFileData saveResult(M result, int status, String baseFileName,
			boolean overwriteIfExisting, List<JSONResultFileData> preEvaluationsUsed) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setWorkspace(String workspace) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean exportWorkspaceFileData(String destinationFilePath, boolean exportAsXML) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Resource importWorkspaceFileData(String sourceFilePath) {
		// TODO Auto-generated method stub
		return null;
	}
}
