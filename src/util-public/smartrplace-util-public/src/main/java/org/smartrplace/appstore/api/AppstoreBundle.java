package org.smartrplace.appstore.api;

/** Representation of an entry in an xml configuration file for a gateway rundir*/
public interface AppstoreBundle {
	MavenBundleVersioned mavenCoordinates();
	
	/** Location for installation inside the bin directory*/
	String installationLocation();
	
	/** Start level declared in the xml file*/
	int startLevel();
	
	/** If the version is not specified explicitly for the bundle, but via a version placeholder for which the
	 * version is specified on top of the xml file then this placeholder String is returned here. The version
	 * specified by the placehoder shall still be included in {@link #mavenCoordinates()}.
	 * @return null if the version is specified directly for the bundle entry
	 */
	String versionGroupID();
	
	/** If true then the bundle declaration will not be used by the launcher because another bundle is declared
	 * with an effectively higher priority
	 * @return null in situations where no such evaluation can be done
	 */
	Boolean isOverridden();
	
	/** File in which the bundle entry is provided as relative path compared to the rundir root*/
	String sourceFile();
}	
