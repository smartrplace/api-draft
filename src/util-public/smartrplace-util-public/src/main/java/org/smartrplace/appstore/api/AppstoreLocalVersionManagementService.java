package org.smartrplace.appstore.api;

import java.util.List;

/** In contrast to the {@link AppstoreVersionGenerationService} this service works on gateway instances
 * using the SNAPSHOT versions of the bundles. Versioning is only done locally based on the
 * date when an update is created. Versions do not only contain the source code, but also configurations
 * and potentially database content.
 */
public interface AppstoreLocalVersionManagementService {
	/** Create copy or zip file of current /bin, /config, /security and gateway directory.
	 * Also last generalBackup file shall be stored.<br>
	 * Note: The generalBackup file already contains the config directory. When also the gateway
	 * directory is added, then only the /bin file needs to be added in addition.
	 * @return time stamp of the version created. Each version shall be accessible by its
	 * 		time stamp.
	 */
	Long generateLocalVersion();
	
	/** Get all versions available on the system
	 * 
	 * @return
	 */
	List<Long> getLocalVersions();
	
	/** Delete version from the system
	 * 
	 * @return true if version was deleted successfully
	 */
	boolean deleteLocalVersion();
	
	/** Get number of versions that can be created without deleting a version based
	 * on the disk space reserved for appstore versions 
	 * 
	 * @return
	 */
	int getEstimatedNumberOfVersionsFree();
	
	/** Restore a previously created version
	 * 
	 * @param versionTimestamp version to be restored. The bin directory shall be restored in any case.
	 * @param restoreConfigurations if true restore also the /config directory and the gateway directory
	 * @param restoreDatabase if true restore also the resource database from the zip file
	 * @param restoreUsers if true restore user administration from the security directory.
	 * @return
	 */
	boolean restoreLocalVersion(long versionTimestamp, boolean restoreConfigurations,
			boolean restoreDatabase, boolean restoreUsers);
	
	/** Trigger update from current Maven/Artifactory version like an update of the
	 * gateway directory via Git
	 * 
	 * @return true if trigger was performed successfully
	 */
	boolean triggerUpdate();
}
