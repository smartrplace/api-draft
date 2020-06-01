package org.smartrplace.appstore.api;

public interface ArtifactoryBundle {
	/** Maven coordinates including groupId and artifactId of the bundles specified in the Rundir for
	 * 		the gateway */
	String mavenCoordinates();
	
	/** Full version String*/
	String version();
	
	/** Perform version comparison.
	 * @return see {@link String#compareTo(String)} for general comparison definition*/
	int compareToVersion(String version);
	
	/** Git commit ID of the source code used to build the artifact. For SourceCodeBundles this is the latest
	 * commit ID changing the bundle
	 * @return
	 */
	String gitCommit();
}	
