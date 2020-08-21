package org.ogema.simpleresource.util;

import java.util.List;

/** This is an initial draft and not implemented yet
 * Note that this is a simpler approach than the ResourceBase / {@link ResourceNonPersistentService}
 * approach drafted before*/
public interface OSGiConfigurationAccess {
	public static interface OSGiConfigValue {
		String getName();
		<T> Class<T> getType();
		void setValue(float value);
		void setValue(String value);
		Float getValue();
		String getString();
		
		//TODO: Does this exist?
		List<OSGiConfigValue> subValues();
	}
	List<String> getMainPids();
	List<String> getFactoryPids(String mainPid);
	List<OSGiConfigValue> getValues(String pid);
	
	//Required?
	void registerPid(String Pid, List<OSGiConfigValue> values);
}
