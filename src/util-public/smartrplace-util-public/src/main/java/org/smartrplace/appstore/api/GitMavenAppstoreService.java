package org.smartrplace.appstore.api;

import java.util.List;

/** 
 * 
 */
public interface GitMavenAppstoreService {
	/** To discuss: The service does not need to store the source bundles persistently, they shall all be
	 * added via addSourceBundles by the application connecting to the service after each system restart
	 * @return
	 */
	List<SourceCodeBundle> getSourceBundles();
	
	/** TODO: Add parameters to be able to provide full Git repository access based on the file path to the local
	 * clone of the repository
	 * @param filePath
	 * @return
	 */
	GitRepository getRepository(String filePath);
	
	/** Register a bundle with its source code for appstore management.
	 * To discuss: If the Git repository is not yet configured for Jenkins already, add a configuration for
	 * CI and deployment to the artifactory. Maybe this is not required initially? Exception when repository is
	 * not configured? Check if app is in Maven build path of the artifactory?
	 * 
	 * @param repo
	 * @param pathInRepo
	 * @return null if no fitting SourceBundle could be found
	 */
	SourceCodeBundle addSourceBundle(GitRepository repo, String pathInRepo);
	List<SourceCodeBundle> addSourceBundles(GitRepository repo, List<String> pathsInRepo);
	
	/** Get bundle versions available in the local artifactory. The list of all bundles may be requested via HTTPS?
	 * 
	 * @param mavenCoordinates if null then all bundles in the .m2/repositories structure shall be returned,
	 * 		otherwise only those for the mavenCoordinates specified
	 * @param update if true the the artifactories specified in the .m2/settings.xml shall be checked for new
	 * 		artifactories
	 * @return List of relevant bundles. Only bundles included into {@link #getSourceBundles()} shall be listed here
	 * 		with all versions available
	 */
	List<ArtifactoryBundle> getBundlesInArtifactory(String mavenCoordinates, boolean update);
	
	/** Deploy a build of the current source code to the Appstore artifactory. The result shall look like the result
	 * of the following procedure:<br>
	 * - Change the version of the bundle to the version specified<br>
	 * - Build the bundle and deploy<br>
	 * - Change back the version of the bundle to the previous version. There should be no commit with the version
	 * specified here.<br>
	 * - Details shall be discussed, for examle: How can the git commit used be included into the artifact so that
	 * this information can be used by the appstore so that {@link ArtifactoryBundle#gitCommit()} can be provided?
	 * 
	 * @param mavenCoordinates
	 * @param version
	 * @return
	 */
	ArtifactoryBundle deployToArtifactory(SourceCodeBundle sourceBundle, String version);
	
	
	/** Get effective bundles specified for a gateway in a Rundir. The effective setting for a rundir is determined
	 * by:<br>
	 * - The base configuration of bundles specified in the config/MM_*.xml files with MM being increasing numbers indicating
	 * the ordert in which the files shall be processed<br>
	 * - files provided in config_NNNNN/config/MM_*.xml with NNNNN being the gatewayID and MM_*.xml files that are
	 * used to overwrite the default files in the main config directory.
	 * Development note: This is described in [Mirror Rundirs, internally](https://gitlab.com/smartrplace/smartrplace-main/-/wikis/Development/MirrorRundirs#setting-up-and-management-of-instances) 
	 * 
	 * @param rundirPath path to local rundir directory to be used
	 * @param gatewayID usually in the form of a 5-digit number. If null then only the general xml files in the main
	 * 		config directory shall be taken into account
	 * @return Maven coordinates including groupId and artifactId of the bundles specified in the Rundir for
	 * 		the gateway
	 */
	List<AppstoreBundle> getRundirBundles(String rundirPath, String gatewayID);
	
	/** The skeleton rundir defines the standard xml files and further standard files. These are used for several
	 * update procedures
	 * @param rundirPath path to local clone of the general Appstore skeleton rundir
	 */
	void setDefaultSkeletonRundir(String rundirPath);
	
	/** The default SRC rundir defines the default 30_sp-roomcontrol.xml file and potentially further specific files
	 * for the Smartrplace standard product. This is also used for several update procedures
	 * @param rundirPath
	 */
	void setDefaultSSRCRundir(String rundirPath);
	
	public enum SingleGatewayUpdateMode {
		/** This option means that just version of the {@link AppstoreBundle} specified shall be changed
		 * without any side effects. Upgrade and downdgrade shall be supported here.
		 */
		SPECIFIED_ENTRY_ONLY,
		/** In this mode the standard xml files for the gateway are updated to the skeleton and SRC default. If this
		 * is an update of just a single gateway then the files are provided in the config_NNNNN directory as the
		 * update shall not change the configuration of other gateways. Alle updates also of other bundles caused by
		 * this are just accepted. If this would cause a downgrade of any bundle (e.g. because an edited version of
		 * a standard xml file is set for the gateway before) then the downgrade shall be prevented by adding a
		 * suitable entry into the gateway-specific XML file. The standard XML files shall not be modified. The bundle
		 * itself shall also be updated, of course.<br>
		 * If the bundle is part of a version group then the bundle shall be taken out of the group and get a separate
		 * version, e.g. by setting a separate entry in the project/gateway-specific XML file (do not change the
		 * standard files!). If a downgrade is specified then this may not be possible - in this case an error code
		 * may be returned or an exception may be thrown (specify in the API when implementation is done).
		 */
		FULL_STANDARD_UDDATE,
		/** Like FULL_STANDARD_UPDATE, but always update group versions, do not take the bundle out of the group
		 * versioning
		 */
		FULL_GROUP_UPDATE
	}
	
	/** Update the version of a single bundle with potential side effects on other bundles based on the updateMode
	 * specified.
	 * It is not required to perform a check whether the new version is available in the artifactory. This shall be
	 * done by the using application.
	 * 
	 * @param rundirPath
	 * @param gatewayID if null then the general setting shall be updated. When one of the {@link SingleGatewayUpdateMode}s
	 * 		starting with FULL is selected, then any downgrades in any gateway shall be prevented. Note that only those
	 * 		gateways shall trigger a software update for which a change in effective special or general bundles is detected.
	 *      This can be implemented by only triggering an update an a gateway if an update on the respective config_NNNNN directory
	 *      is made, e.g. in a file containing just a number that is counted upwards with each update.
	 * @param mavenCoordinates of the bundle. May be null if only standard bundles shall be updated
	 * @param version
	 * @param updateMode
	 * @return list of bundle entries changed
	 */
	List<AppstoreBundle> updateVersion(String rundirPath, String gatewayID, String mavenCoordinates, String version,
			SingleGatewayUpdateMode updateMode);
	
	public enum MultiGatewayUpdateMode {
		/** Just update general version setting without caring about side effects on single gateways*/
		GENERAL_SETTING_ONLY,
		/** Like GENERAL_SETTING_ONLY, but make sure that any gateway-specific entries speicifiying versions between
		 * the general version before the update and after the update are removed. This effectively means that after
		 * the update all gateways are on the new version except for those who were already ahead of the new standard
		 * version before the update or who where behind the previous standard version before the update.
		 */
		STANDARD_TO_STANDARD,
		/** Like STANDARD_TO_STANDARD, but also gateways that where behind the previous version are not set to 
		 * the standard version ("No gateway-left-behind mode").
		 */
		ALL_TO_STANDARD_MINIMUM,
		/** Force that all gateways use the same version of the bundle specified even if that means a downgrade
		 * on some gateways*/
		ALL_TO_STANDARD_FORCED
	}
	
	/** Like {@link #updateVersion(String, String, String, String, SingleGatewayUpdateMode)}, but affects the
	 * general settings for a project. The bundles affected at all are determined via the
	 * {@link SingleGatewayUpdateMode} like in the other updateVersion method. The handling of different gateways
	 * with special configurations for those bundles is determined via the {@link MultiGatewayUpdateMode}. Not all
	 * combinations of the two modes may make sense, errors/exceptions may be defined during implementation
	 * 
	 * 
	 * @param rundirPath
	 * @param mavenCoordinates
	 * @param version
	 * @param updateMode
	 * @param multiGatewayModeSpec
	 * @return list of bundle entries changed
	 */
	List<AppstoreBundle> updateVersion(String rundirPath, String mavenCoordinates, String version,
			SingleGatewayUpdateMode updateMode,
			MultiGatewayUpdateMode multiGatewayModeSpec);
	
	GitRepository getRundirRepository(String rundirPath);

}
