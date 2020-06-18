package org.smartrplace.appstore.api;

import java.io.IOException;
import java.util.List;

/** This service can be used to generate an artifact in the Appstore Maven Artifactory from a source code version committed to a
 * Git Source code repository configured for the Appstore CI server. Configuration of the Git repositories is not part of
 * this API yet.
 */
public interface AppstoreVersionGenerationService {
	/** Get Git repository information on a repository configured on the CI server and that is available locally. For now
	 * we assume that the repository is configured on the CI server, no automated configuration on the CI is required at this step.
	 * TODO: Add parameters if necessary to be able to provide full Git repository access based on the file path to the local
	 * clone of the repository
	 * @param filePath path to the local clone of the repository
	 * @return
     * @throws IOException if the given path could not be accessed or does not contain a git repository.
	 */
	GitRepository getRepository(String filePath) throws IOException;

	/** Register a bundle with its source code for appstore management. The service does not need to store the source
	 * bundles persistently, they shall all be
	 * added via this method by the application connecting to the service after each system restart
	 * To discuss: If the Git repository is not yet configured for Jenkins already, add a configuration for
	 * CI and deployment to the artifactory. Maybe this is not required initially? Exception when repository is
	 * not configured? Check if app is in Maven build path of the artifactory?
	 * 
	 * @param repo
	 * @param pathInRepo
	 * @return null if no fitting SourceBundle could be found
	 */
	SourceCodeBundle addSourceBundle(GitRepository repo, String pathInRepo);
	
	//Not really required 
	//List<SourceCodeBundle> addSourceBundles(GitRepository repo, List<String> pathsInRepo);

	/** Get all source bundles registered so far with up-to-date version information 
	 * @return
	 */
	List<SourceCodeBundle> getSourceBundles();
	
	/** Get bundle versions available in the local artifactory. It is not required that the source code file path
	 * has been registered before as artifacts may still exist for bundles for which the source code is not
	 * available anymore or has moved.
	 * TODO: The list of all bundles may be requested via HTTPS?
	 * 
	 * @param mavenCoordinates if null then all bundles in the .m2/repositories structure shall be returned,
	 * 		otherwise only those for the mavenCoordinates specified
	 * @param update if true the the artifactories specified in the .m2/settings.xml shall be checked for new
	 * 		artifactories
	 * @return List of relevant bundles. Only bundles included into {@link #getSourceBundles()} shall be listed here
	 * 		with all versions available
	 */
	List<ArtifactoryBundle> getBundlesInArtifactory(MavenBundleUnversioned mavenCoordinates, boolean update);
	
	/** Deploy a build of the current source code to the Appstore artifactory. The result shall look like the result
	 * of the following procedure:<br>
	 * - Change the version of the bundle to the version specified<br>
	 * - Build the bundle and deploy<br>
	 * - Change back the version of the bundle to the previous version. There should be no commit with the version
	 * specified here.<br>
	 * - Details shall be discussed, for examle: How can the git commit used be included into the artifact so that
	 * this information can be used by the appstore so that {@link ArtifactoryBundle#gitCommit()} can be provided?
	 * 
	 * TODO: Update this based on the real implementation. The current plan is to generate the versioned bundle
	 * via a special configured Jenkins build
	 * 
	 * @param sourceBundle reference to the source code information obtained via {@link #addSourceBundle(GitRepository, String)}
	 * 		or {@link #getSourceBundles()}. So the source bundle has to be registered before calling this method.
	 * @param commit commit to be used
	 * @param version
	 * @return
	 */
	ArtifactoryBundle deployArtifact(SourceCodeBundle sourceBundle, String version, GitCommit commit);

}
