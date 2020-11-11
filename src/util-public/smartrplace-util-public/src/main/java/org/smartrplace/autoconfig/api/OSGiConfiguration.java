package org.smartrplace.autoconfig.api;

import org.ogema.simpleresource.util.ResourceNonPersistentService;

/** This is an initial draft and not implemented yet
 * Note that this is a simpler approach than the ResourceBase / {@link ResourceNonPersistentService}
 * approach drafted before*/
public interface OSGiConfiguration {
	/**Full pid*/
	String getPid();
	
	/** Name as shown in the OSGi Config Admin. The name usually only is unique for the mainPid*/
	String name();
	
	/** Read float value from the configuration
	 * 
	 * @param key
	 * @return null if a value with the key does not exist or if the value cannot be converted to float
	 */
	Float getFloat(String key);
	
	/** Write float value into the configuration
	 * 
	 * @param key
	 * @param value
	 * @param commitImmediately if true then the value and other values written since last commit are
	 * 		written into the persistent storage. This will lead to a restart of any bundle that registered
	 * 		the configuration. Otherwise the change is stored just for the next commit.
	 * @return true if the value could be written successfully
	 */
	boolean setFloat(String key, float value, boolean commitImmediately);
	
	Long getLong(String key);
	boolean setLong(String key, long value, boolean commitImmediately);
	
	Boolean getBoolean(String key);
	boolean setBoolean(String key, boolean value, boolean commitImmediately);

	String getString(String key);
	boolean setString(String key, String value, boolean commitImmediately);

	Float[] getFloatArray(String key);
	boolean setFloatArray(String key, Float[] value, boolean commitImmediately);

	Long[] getLongArray(String key);
	boolean setLongArray(String key, Long[] value, boolean commitImmediately);

	Boolean[] getBooleanArray(String key);
	boolean setBooleanArray(String key, Boolean[] value, boolean commitImmediately);

	String[] getStringArray(String key);
	boolean setStringArray(String key, String[] value, boolean commitImmediately);

	boolean commitChanges();
	
	boolean deleteValue(String key, boolean commitImmediately);
	//List<OSGiConfigurationValue> getValues();
	
	/** Get variable
	 * 
	 * @param variableName
	 * @return null if not defined
	 */
	//OSGiConfigurationValue getValue(String variableName);
	
	/** Add new value
	 * 
	 * @param <T>
	 * @param variableName
	 * @param description
	 * @param type
	 * @return newly created value
	 */
	//<T> OSGiConfigurationValue addValue(String variableName, String description, Class<T> type, boolean isArray);
}
