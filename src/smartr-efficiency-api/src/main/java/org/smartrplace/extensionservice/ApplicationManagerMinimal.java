package org.smartrplace.extensionservice;

/**This is a minimal implementation of the ApplicationManager that normal OGEMA applications receive.
 * Extension modules that shall not get access to ResourceManagement / ResourceAccess can get this
 * minimal framework access.
 * 
 */
public interface ApplicationManagerMinimal {
	long getFrameworkTime();
}
