package org.smartrplace.appstore.api;

/** Representation of a bundle built available in the Appstore Maven Artifactory*/
public interface ArtifactoryBundle {
	MavenBundleVersioned mavenCoordinates();
	
	/** Source bundle used to build the artifact. May be null if the source is not available / known
	 * anymore
	 * @return
	 */
	SourceCodeBundle sourceBundle();
	
	/** Git commit information of the source code used to build the artifact. For SourceCodeBundles this is the latest
	 * commit ID changing the bundle
	 * @return
	 */
	GitCommit gitCommit();
}	
