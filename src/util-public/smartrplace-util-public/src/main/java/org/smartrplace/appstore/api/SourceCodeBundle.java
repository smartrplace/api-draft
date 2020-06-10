package org.smartrplace.appstore.api;

/** Representation of a bundle source code in a Git repository that is configured for the Appstore.
 * We assume here that the bundle is checked out from the master branch if several branches exist.
 * The information contains the Git repository path and the Maven coordinates as well as the
 * version in the current source code. */
public interface SourceCodeBundle {
	MavenBundleVersioned mavenCoordinates();
	
	GitRepository repository();
	String pathInRepository();
}
