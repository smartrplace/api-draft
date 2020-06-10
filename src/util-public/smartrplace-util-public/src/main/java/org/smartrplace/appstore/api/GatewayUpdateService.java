package org.smartrplace.appstore.api;

import java.util.List;
import java.util.Map;

/** This service configures which artifacts from the Appstore Maven Artifactory are actually used on which
 * gateway connected to the Appstore.
 * TODO: The definition below assumes that all gateways of a project share a common Git configuration repository
 * defining the xml configuration files actually used on a Gateway. It further assumes that the definition of the
 * specific gateway configuration is NOT made via separate branches for each gateway, but via a separate directory for
 * each gateway in the master branch holding the xml files that need to be overwritten or added to the general project
 * settings. This requires an adaptation of the update script that copies the content of this directory to the config
 * directory after each git pull that brought changes. The advantage of this approach is that handling the gateway-specific
 * information is probably much easier on the Appstore server side then managing hundreds of branches and their synchronization.
 */
public interface GatewayUpdateService {
	/** Get Git repository information on a rundir repository. We assume here that each project configuration
	 * repository contains exactly one project rundir configuring all gateways of the project.
	 * TODO: The implementation can be very similar to {@link AppstoreVersionGenerationService#getRepository(String)}
	 * @param filePath path to the local clone of the repository
	 * @return
	 */
	GitRepository getRepository(String filePath);
	
	/** Get bundles specified for a gateway in a Rundir.
	 * 
	 * @param rundirPath repository of the local rundir directory to be used this can also be the skeleton or the default SRC rundir,
	 * 		which are not managed via Git by the Appstore.
	 * @param gatewayID usually in the form of a 5-digit number. If null then only the general xml files in the main
	 * 		config directory shall be taken into account, otherwise only the gateway-specific files in the directory config_<gatewayID>
	 * @return Map xml file name (without path) -> list of all appstore bundle representations in the file
	 */
	Map<String, List<AppstoreBundle>> getRundirBundles(String rundirPath, String gatewayID);
	
	/** Update the version of a single bundle entry. If the specified file does not exist it shall be created as valid
	 * xml configuration file.
	 * 
	 * It is not required to perform a check whether the new version is available in the artifactory. This shall be
	 * done by the using application.
	 * 
	 * @param rundirPath repository of the local clone of the project rundir holding the xml configuration files. This method only
	 * 		writes into directories managed via Git, so a {@link GitRepository} has to be given as argument here.
	 * @param fileName name of the xml file to be adapted. The name is given without path as the path shall be determined by the gatewayID
	 * @param gatewayID if null then the general setting shall be updated, otherwise only the version in the gateway directory config_<gatewayID>
	 * @param mavenBundle coordinates and version of the bundle. If the same group and artifact ID already exists in the
	 * 		file then the version shall be overriden, otherwise a new entry shall be created. If the version is null then the entry
	 * 		shall be deleted if it exists.
	 * @param updateMode
	 * 
	 * @return list of bundle entries changed
	 */
	List<AppstoreBundle> updateOrSetVersion(GitRepository rundirPath, String fileName, String gatewayID, MavenBundleVersioned mavenBundle);

}
