package org.smartrplace.appstore.api;

// TODO: Specify all data relevant to define Git repository data for access and management
public interface GitRepository {
	String sshUrl();
	
	/** Commit local changes, pull and push. If auto-merging is not successful make sure that
	 * local changes are reverted so that this works for sure as no manual merging is possible
	 * in the Appstore processes.*/
	void synchronize();
}
