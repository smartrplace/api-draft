package org.smartrplace.autoconfig.api;

import java.util.List;

import org.ogema.simpleresource.util.ResourceNonPersistentService;

/** This is an initial draft and not implemented yet
 * Note that this is a simpler approach than the ResourceBase / {@link ResourceNonPersistentService}
 * approach drafted before*/
public interface OSGiConfiguration {
	/**Full pid*/
	String getPid();
	
	/** Name as shown in the OSGi Config Admin. The name usually only is unique for the mainPid*/
	String name();
	
	List<OSGiConfigurationValue> getValues();
	
	/** Get variable
	 * 
	 * @param variableName
	 * @return null if not defined
	 */
	OSGiConfigurationValue getValue(String variableName);
	
	/** Add new value
	 * 
	 * @param <T>
	 * @param variableName
	 * @param description
	 * @param type
	 * @return newly created value
	 */
	<T> OSGiConfigurationValue addValue(String variableName, String description, Class<T> type, boolean isArray);
	
	boolean deleteValue(OSGiConfigurationValue value);
}
