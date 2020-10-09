package org.smartrplace.autoconfig.api;

import java.util.List;

public interface OSGiConfigAccessService {
	/** Get configuration by full pid including extension of factory pid if applicable.
	 * @return null if the pid is not registered. If the pid is a NOT a factory pid and it is
	 * registered, but no configuration is stored yet, then it shall be created. If it is a factory
	 * pid and no configuration for the specific factory pid has been set up then it shall be created.*/
	OSGiConfiguration getConfiguration(String pid);
	
	/**Get factory Pids for a mainPid
	 * 
	 * @param mainPid
	 * @return full factoryPIds in the format mainPid~factoryExtension
	 */
	List<String> getFactoryPids(String mainPid);
	
	/***********
	 * TODO: The following methods are not required for now
	 */
	
	/** Delete
	 * 
	 * @param config
	 * @return true if successful (the config existed and could be deleted)
	 */
	boolean deleteConfiguration(OSGiConfiguration config);
	
	/** Get all Pids
	 * 
	 * @param instantiatedOnly if false then all configuration as shown in the Apache Felix
	 * Web Console are returned, if true only those configuration that are really registered
	 * with values different from the default values are returned
	 * @return
	 */
	List<String> getMainPidsConfigured(boolean instantiatedOnly);
}
