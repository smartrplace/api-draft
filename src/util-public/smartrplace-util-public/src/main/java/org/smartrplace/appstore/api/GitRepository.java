package org.smartrplace.appstore.api;

import java.util.List;

// TODO: Specify all data relevant to define Git repository data for access and management
public interface GitRepository {
	String sshUrl();
	
	/** Path to the local clone of the Git repository*/
	String localPath();
	
	/** Commit local changes, pull and push. If auto-merging is not successful make sure that
	 * local changes are reverted so that this works for sure as no manual merging is possible
	 * in the Appstore processes.*/
	void synchronize();
	
	/** Get all commits within a certain time range*/
	List<GitCommit> getCommits(long start, long end);
	
	/** Update repository via pull. This is mainly relevant for source code repositories*/
	boolean performPull();
	
	/** Commit all changes in the repository, pull any outside chanes and push the the new commit 
	 *  Note that this shall only be done on the rundir configuration repositories by the appstore,
	 *  not on the source code repositories.
	 * @param commitMessage if null a default message representing the updates shall be used
	 * @param forcePush if true then merging and pushing shall be enforced even if the standard merge
	 * 		of Git fails. In this case the local content may be reset to the head so that merging 
	 * 		works for sure.
	 * @return true if content was obtained via pull meaning the content of the repository was changed
	 * 		from outside the repository, which may require an update of the GUI content.
	 */
	boolean performCommitPush(String commitMessage, boolean forcePush);
}
