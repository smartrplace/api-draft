package org.smartrplace.appstore.api;

public interface AppstoreBundleInstall extends AppstoreBundle, ArtifactoryBundle {
	/** Time of installation of the bundle and/or transmission to the gateway
	 * 
	 * @return null if this time is not available
	 */
	Long installationTime();
}
