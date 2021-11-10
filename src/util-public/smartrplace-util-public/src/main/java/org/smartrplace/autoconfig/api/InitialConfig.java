package org.smartrplace.autoconfig.api;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;

public class InitialConfig {

	public static boolean isInitDone(String initID, StringResource res) {
		String status = res.getValue();
		if(status != null && status.contains(initID+","))
			return true;
		return false;
	}
	public static boolean checkInitAndMarkAsDone(String initID, StringResource res, String deviceId) {
		if(isInitDone(initID, res))
			return true;
		addString(initID, res, deviceId);
		return false;
	}
	/** Check in EvalCollection.initDoneStatus
	 * 
	 * @param initID
	 * @param appMan
	 * @param deviceId
	 * @return true if init is already done, false if the operation still has to be performed
	 */
	public static boolean checkInitAndMarkAsDone(String initID, ApplicationManager appMan, String deviceId) {
		ResourceList<StringResource> resList = ResourceHelper.getEvalCollection(appMan).initDoneStatus();
		StringResource res;
		if(resList.size() == 0) {
			res = resList.add();
		} else
			res = resList.getAllElements().get(0);
		return checkInitAndMarkAsDone(initID, res, deviceId);
	}
	public static void addString(String initID, StringResource res) {
		addString(initID, res, null);
	}
	public static void addString(String initID, StringResource res, String deviceId) {
		if(!res.exists()) {
			ValueResourceHelper.setCreate(res, initID);
		} else {
			String exist = res.getValue();
			if(exist.contains(initID+","))
				return;
			String newExist = "";
			if(deviceId != null) {
				//remove existing entries for deviceId
				String[] entries = exist.split(",");
				for(String e: entries) {
					if(e.startsWith(deviceId))
						continue;
					newExist += e+",";
				}
			} else
				newExist = exist;
			res.setValue(newExist+initID+",");
		}
	}

}
