package org.smartrplace.appstore.api;

/** Respresentation of a Git commit*/
public interface GitCommit {
	/** ID of the commit
	 */
	String gitCommit();

	/** Time stamp of the commit*/
	long timeStamp();
	
	/** User name of the committer
	 * TODO: Check if the email address should be used or be provided additionally
	 */
	String committerUserName();
	
	/** Reference to the repository to which the commit belongs*/
	GitRepository repository();
}
