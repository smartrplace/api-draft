package org.smartrplace.remotesupervision.transfer.util;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;

public class DataLogManagement {
	public static String getLastDateConfirmedForAll(StringResource allDataLogsTransferredUpToHereDateName,
			ResourceList<TimeResource> datesSentCompletelyAllLogs) {
		if(datesSentCompletelyAllLogs == null) return null;
		String maxString = null;
		if(allDataLogsTransferredUpToHereDateName.isActive()) {
			String s = allDataLogsTransferredUpToHereDateName.getValue();
			if(!((s == null) || s.equals(""))) {
				maxString = s;
			}
		}
		for(TimeResource confirmed: datesSentCompletelyAllLogs.getAllElements()) {
			maxString = getMaxString(confirmed.getName(), maxString);
		}
		return maxString;
	}
	private static String getMaxString(String arg1, String currentMaxString) {
		if(currentMaxString == null) return arg1;
		if(arg1.compareTo(currentMaxString) > 0) return arg1;
		return currentMaxString;
	}
}
