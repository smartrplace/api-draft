package org.smartrplace.appstore.api;

public interface MavenBundleVersioned extends MavenBundleUnversioned {
	String version();
	
	/** Perform version comparison.
	 * @return see {@link String#compareTo(String)} for general comparison definition*/
	int compareToVersion(String version);
}
