package org.smartrplace.backup.api;

import java.nio.file.Path;
import java.util.concurrent.Future;

public interface OSGiConfigBackupService {
	/** Draft: Create backup of all OSGi configuations into a file in the format of gateways.yaml
	 * 
	 * @param destinationLocation
	 * @return
	 */
	Future<Path> createBackup(Path destinationLocation);
}
