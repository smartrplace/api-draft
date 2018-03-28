package org.smartrplace.smarteff.admin.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.smartrplace.extensionservice.ExtensionResourceType;

/** TODO: This is not thread-safe yet
 * TODO: This has no clean-up mechanism yet
 * TODO: Generate configId randomly individually*/
public class ConfigIdAdministration {
	public static class ConfigInfo {
		public ConfigInfo(int entryIdx, List<ExtensionResourceType> entryResources) {
			this.entryIdx = entryIdx;
			this.entryResources = entryResources;
		}
		public int entryIdx;
		public List<ExtensionResourceType> entryResources;
	}
	Map<String, ConfigInfo> resourcesLocked = new HashMap<>();
	
	private int counter = 1000;
	
	public String getConfigId(int entryIdx, List<ExtensionResourceType> entryResources) {
		String result = ""+counter;
		counter++;
		resourcesLocked.put(result, new ConfigInfo(entryIdx, entryResources));
		return result;
	}
	
	public ConfigInfo getConfigInfo(String configId) {
		return resourcesLocked.get(configId);
	}
}
