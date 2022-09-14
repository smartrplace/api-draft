package org.smartrplace.appstore.api.versionmanagement;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.smartrplace.appstore.api.AppstoreBundle;
import org.smartrplace.appstore.api.GatewayUpdateService;
import org.smartrplace.appstore.api.GitCommit;
import org.smartrplace.appstore.api.GitRepository;
import org.smartrplace.appstore.api.MavenBundleUnversioned;
import org.smartrplace.system.guiappstore.config.AppstoreSystemUpdates;
import org.smartrplace.system.guiappstore.config.SystemUpdate;

import de.iwes.util.format.StringFormatHelper;

public class AppstoreUtils {
	public static String getRepositoryName(GitRepository repo) {
		return getRepositoryName(repo.remoteUrl());
	}
	public static String getRepositoryName(String repoUrl) {
		String[] els = repoUrl.split("/");
		String result = els[els.length-1];
		if(result.endsWith(".git"))
			return result.substring(0, result.length()-4);
		return result;
	}

	/** Get last update based on commitMaxTime*/
	public static SystemUpdate getLastUpdateGenerated(AppstoreSystemUpdates appConfigSysUpd) {
		List<SystemUpdate> all = appConfigSysUpd.systemUpdates().getAllElements();
		SystemUpdate result = null;
		long lastTime = -1;
		for(SystemUpdate upd: all) {
			if(upd.commitMaxTime().getValue() > lastTime) {
				lastTime = upd.commitMaxTime().getValue();
				result = upd;
			}
		}
		return result;
	}
	
	public static class LastVersionResult {
		public LastVersionResult(String version, SystemUpdate upd) {
			this.mostFrequentVersion = version;
			this.upd = upd;
		}
		public String mostFrequentVersion;
		public SystemUpdate upd;
	}
	
	/* Get version used most often in last SystemUpdate the repo was used*/
	public static LastVersionResult getLastVersion(GitRepository repo, AppstoreSystemUpdates appConfigSysUpd) {
		SystemUpdate last = getLastUpdateGenerated(appConfigSysUpd);
		if(last == null)
			return null;
		VersionsOccurences version = getRepoVersion(repo, last);
		if(!version.versions.isEmpty())
			return new LastVersionResult(version.versions.get(0), last);
		List<SystemUpdate> sorted = appConfigSysUpd.systemUpdates().getAllElements();
		sorted.sort(new Comparator<SystemUpdate>() {

			@Override
			public int compare(SystemUpdate o1, SystemUpdate o2) {
				return Long.compare(o2.commitMaxTime().getValue(), o1.commitMaxTime().getValue());
			}
		});
		for(SystemUpdate upd: sorted) {
			if(upd.equals(last))
				continue;
			version = getRepoVersion(repo, upd);
			if(version != null && (!version.versions.isEmpty()))
				return new LastVersionResult(version.versions.get(0), upd);
		}
		return null;
	}
	
	public static SystemUpdate getLastFullVersion(AppstoreSystemUpdates appConfigSysUpd) {
		List<SystemUpdate> sorted = appConfigSysUpd.systemUpdates().getAllElements();
		sorted.sort(new Comparator<SystemUpdate>() {

			@Override
			public int compare(SystemUpdate o1, SystemUpdate o2) {
				return Long.compare(o2.commitMaxTime().getValue(), o1.commitMaxTime().getValue());
			}
		});
		for(SystemUpdate upd: sorted) {
			VersionsOccurences repos = AppstoreUtils.getVersionsOccurences(upd.repoPath().getValues(), null, null);
			if(repos.versions.size() > 1)
				return upd;
		}
		return null;
	}
	
	/** DEPRECATED: Get version of bundles in repository assuming that all bundles of the repo are used with the same version*/
	/*public static String getRepoVersion(GitRepository repo, SystemUpdate upd) {
		List<String> repoPaths = Arrays.asList(upd.repoPath().getValues());
		int idx = repoPaths.indexOf(repo.remoteUrl());
		if(idx < 0)
			return null;
		return upd.version().getElementValue(idx);
	}*/
	/** Version for Repo in a certain SystemUpdate*/
	public static class VersionsOccurences {
		public List<String> versions = new ArrayList<>();
		public List<Integer> occurences = new ArrayList<>();
	}
	public static VersionsOccurences getRepoVersion(GitRepository repo, SystemUpdate upd) {
		//VersionsOccurences result = new VersionsOccurences();
		String[] repoPaths = upd.repoPath().getValues();
		String[] versions = upd.version().getValues();
		return getVersionsOccurences(versions, repo.remoteUrl(), repoPaths);
		/*int maxOcc = 0;
		int maxOccIdx = -1;
		for(int idx=0; idx<repoPaths.length; idx++) {
			if(repo.remoteUrl().equals(repoPaths[idx])) {
				String ver = versions[idx];
				int resIdx = result.versions.indexOf(ver);
				if(resIdx < 0) {
					result.versions.add(ver);
					result.occurences.add(1);
				} else {
					int newOcc = result.occurences.get(resIdx)+1;
					result.occurences.set(resIdx, newOcc);
					if(newOcc > maxOcc) {
						maxOcc = newOcc;
						maxOccIdx = resIdx;
					}
				}
			}
				
		}
		if(maxOccIdx > 0) {
			//Set version with most occurences in front
			String keeps = result.versions.get(0);
			result.versions.set(0, result.versions.get(maxOccIdx));
			result.versions.set(maxOccIdx, keeps);
			Integer keepi = result.occurences.get(0);
			result.occurences.set(0, result.occurences.get(maxOccIdx));
			result.occurences.set(maxOccIdx, keepi);
		}
		return result;*/
	}

	/** Return all Strings that can be found in input summarizing all equal Strings and the number of
	 * occurences.
	 * @param input
	 * @param filterDest
	 * @param filterStrings
	 * @return put one of the Strings that occure most on first place, no more sorting
	 */
	public static VersionsOccurences getVersionsOccurences(String[] input, String filterDest, String[] filterStrings) {
		VersionsOccurences result = new VersionsOccurences();
		int maxOcc = 0;
		int maxOccIdx = -1;
		for(int idx=0; idx<input.length; idx++) {
			if(filterDest != null && (!filterDest.equals(filterStrings[idx])))
				continue;
			String ver = input[idx];
			int resIdx = result.versions.indexOf(ver);
			if(resIdx < 0) {
				result.versions.add(ver);
				result.occurences.add(1);
			} else {
				int newOcc = result.occurences.get(resIdx)+1;
				result.occurences.set(resIdx, newOcc);
				if(newOcc > maxOcc) {
					maxOcc = newOcc;
					maxOccIdx = resIdx;
				}
			}
		}
		if(maxOccIdx > 0) {
			//Set version with most occurences in front
			String keeps = result.versions.get(0);
			result.versions.set(0, result.versions.get(maxOccIdx));
			result.versions.set(maxOccIdx, keeps);
			Integer keepi = result.occurences.get(0);
			result.occurences.set(0, result.occurences.get(maxOccIdx));
			result.occurences.set(maxOccIdx, keepi);
		}
		return result;
	}
	
	public static String getVersionsOccSummary(VersionsOccurences allVersions) {
		if(allVersions == null)
			return "!!null!!";
		if(allVersions.versions.isEmpty())
			return "--";
		else if(allVersions.versions.size() == 1)
			return allVersions.versions.get(0);
		else
			return allVersions.versions.get(0) + "("+allVersions.versions.size()+")";
	}
	
	public static Set<String> getLastVersions(List<String> versions) {
		Set<String> result = new HashSet<>();
		for(String v: versions) {
			if(v == null)
				throw new IllegalStateException("Version is null:"+StringFormatHelper.getListToPrint(versions));
			result.add(getLastVersion(v));
		}
		return result;
	}
	
	public static String getLastVersion(String fullVersion) {
		int idxdot = fullVersion.lastIndexOf('.');
		int idxminus = fullVersion.lastIndexOf('-');
		int idx = Math.max(idxdot, idxminus);
		if(idx < 0)
			return fullVersion;
		if(idx >= (fullVersion.length()-1))
			throw new IllegalStateException("Version ending on dot or minus:"+fullVersion);
		return fullVersion.substring(idx+1);
	}
	
	public static String getUnversioned(MavenBundleUnversioned coords) {
		return coords.groupId()+"."+coords.artifactId();
	}
	
	public static GitCommit getLastCommit(GitRepository repo, long now) {
		long interval = TimeProcUtil.DAY_MILLIS;
		do {
			long startTime = now - interval;
			List<? extends GitCommit> commits = repo.getCommits(startTime, now);
			if(!commits.isEmpty()) {
				GitCommit result = null;
				long maxTime = -1;
				for(GitCommit com: commits) {
					if(com.timeStamp() > maxTime) {
						maxTime = com.timeStamp();
						result = com;
					}
				}
				return result;
			}
			interval = interval*2;
		} while(interval <= 512*TimeProcUtil.DAY_MILLIS);
		return null;
	}
	
	public static String[] getGroupArtifactId(String mavenCoords) {
		int idx = mavenCoords.lastIndexOf('.');
		return new String[] {mavenCoords.substring(0, idx), mavenCoords.substring(idx+1)};
	}

	//MavenCoord->Data
	public static Map<String, RundirBundleData> getRundirBundleData(String gwId, String rundirRepoLocalPath, GatewayUpdateService gwUpdateService) {
		Map<String, RundirBundleData> result = new HashMap<>(); //new ArrayList<>();
		try {
			Map<String, List<AppstoreBundle>> allGeneral = gwUpdateService.getRundirBundles(rundirRepoLocalPath,
					null);
			for(Entry<String, List<AppstoreBundle>> ab: allGeneral.entrySet()) {
				for(AppstoreBundle bundle: ab.getValue()) {
					result.put(AppstoreUtils.getUnversioned(bundle.mavenCoordinates()), new RundirBundleData(ab.getKey(), bundle));					
				}
			}
			if(gwId == null)
				return result;
			Map<String, List<AppstoreBundle>> all = gwUpdateService.getRundirBundles(rundirRepoLocalPath,
					gwId);
			for(Entry<String, List<AppstoreBundle>> ab: all.entrySet()) {
				for(AppstoreBundle bundle: ab.getValue()) {
					result.put(AppstoreUtils.getUnversioned(bundle.mavenCoordinates()), new RundirBundleData(gwId+"/"+ab.getKey(), bundle));					
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
    public static String getVersion(SystemUpdate update, String mavenToFind) {
		String[] versions = update.version().getValues();
		
		int idx = 0;
		for(String mavenCoord: update.mavenCoordUnmversioned().getValues()) {
			String version = versions[idx];
			idx++;
			if(mavenToFind.equals(mavenCoord)) {
				return version;
			}
		}
		if(update.parentUpdate().exists())
			return getVersion(update.parentUpdate(), mavenToFind);
		return null;
	}
    
    public static class SystemUpdateSummary {
    	public long lastChangeTime = -1;
    	public int lastBundleChangeNum = 0;
    	public long lastBuildTime = -1;
    }
    private static SystemUpdateSummary susummary = null;
    public static SystemUpdateSummary getSystemUpdateSummary(BundleContext bc) {
    	if(susummary == null) {
    		if(bc == null)
    			return null;
    		susummary = new SystemUpdateSummary();
    		Bundle[] allBundles = bc.getBundles();
    		for(Bundle bundle: allBundles) {
    			long modTime = bundle.getLastModified();
    			if(modTime > susummary.lastChangeTime) {
    				if(modTime > (susummary.lastChangeTime + TimeProcUtil.MINUTE_MILLIS))
    					susummary.lastBundleChangeNum = 0;
    				susummary.lastChangeTime = modTime;
    			}
    			if(modTime > (susummary.lastChangeTime - TimeProcUtil.MINUTE_MILLIS))
    				susummary.lastBundleChangeNum++;
    			String lastMod = bundle.getHeaders().get("Bnd-LastModified");
    			if(lastMod != null) try {
    				long lastBuild = Long.parseLong(lastMod);
    				if(lastBuild > susummary.lastBuildTime)
    					susummary.lastBuildTime = lastBuild;
    			} catch(NumberFormatException e) {}
    		}
    	}
    	return susummary;
    }
}
