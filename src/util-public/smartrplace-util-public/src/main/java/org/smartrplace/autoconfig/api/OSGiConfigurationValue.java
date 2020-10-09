package org.smartrplace.autoconfig.api;

import org.ogema.simpleresource.util.ResourceNonPersistentService;

/** This is an initial draft and not implemented yet
 * Note that this is a simpler approach than the ResourceBase / {@link ResourceNonPersistentService}
 * approach drafted before*/
public interface OSGiConfigurationValue {
	/** Name of variable as defined in the JSON and interface*/
	String getVariableName();
	
	/**Description for the variable
	 * TODO: Should we add a locale as argument?*/
	
	String getDescription();

	/**Type of the variable, usually a value type such as Float or String or an array of such variables
	 * TODO: Add support for arrays in setter und getter methods*/
	<T> Class<T> getType();
	
	/**If true then the value contains an array of the value type specified*/
	boolean isArray();
	
	/**The float value shall be converted to the type of the variable of getType() is not Float*/
	void setValue(float value);
	
	/**The String value shall be converted to the type of the variable of getType() is not String*/
	void setValue(String value);
	
	/** Get Float representation of the value content if possible
	 * 
	 * @return null if value is empty or if no Float representation is possible
	 */
	Float getValue();
	
	/** Get Float representation of the value
	 * 
	 * @return null if value is empty
	 */
	String getString();
	
	/**If true the value has not been set, but just uses the default value
	 * TODO: If not easy to implement, the method can be removed*/
	boolean isDefaultValue();
	Float getDefaultValue();
	String getDefaultString();
	
	/** Get the OSGiConfiguration to which the value belongs*/
	OSGiConfiguration getOSGIConfig();
}
