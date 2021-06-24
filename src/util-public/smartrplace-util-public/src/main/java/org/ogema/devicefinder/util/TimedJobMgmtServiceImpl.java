package org.ogema.devicefinder.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.TimedJobMgmtService;
import org.ogema.devicefinder.api.TimedJobProvider;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.smartrplace.apps.eval.timedjob.TimedEvalJobConfig;
import org.smartrplace.apps.eval.timedjob.TimedJobConfig;
import org.smartrplace.autoconfig.api.InitialConfig;
import org.smartrplace.gateway.device.MemoryTimeseriesPST;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;

public class TimedJobMgmtServiceImpl implements TimedJobMgmtService {
	Map<String, TimedJobMemoryData> knownJobs = new HashMap<>();
	protected final ApplicationManager appMan;
	protected final TimedJobMgmtData jobData;
	
	public static class TimedJobMgmtData {
		public final MemoryTimeseriesPST logResource;

		public TimedJobMgmtData(MemoryTimeseriesPST logResource) {
			this.logResource = logResource;
		}
	}
	
	public TimedJobMgmtServiceImpl(ApplicationManager appMan) {
		this.appMan = appMan;
		MemoryTimeseriesPST logResource = ResourceHelper.getEvalCollection(appMan).getSubResource("memoryTimeseriesPST", MemoryTimeseriesPST.class);
		if(!logResource.exists()) {
			logResource.create();
			logResource.activate(false);
		}
		this.jobData = new TimedJobMgmtData(logResource);
	}

	@Override
	public TimedJobMemoryData registerTimedJobProvider(TimedJobProvider prov) {
		String id = prov.id();
		TimedJobMemoryData result = knownJobs.get(id);
		if(result == null) {
			result = new TimedJobMemoryData(appMan, jobData);
			result.prov = prov;
			result.res = getOrCreateConfiguration(id, prov.evalJobType()>0);
			createPersistentIndex(result.res);
			if(!result.res.isActive()) {
				prov.initConfigResource(result.res);
				result.res.activate(true);
			}
			knownJobs.put(id, result);
		} else {
			final String provVersion = prov.getInitVersion();
			final String shortID = provVersion.isEmpty()?id:(id+"_"+provVersion);
			LocalGatewayInformation gw = ResourceHelper.getLocalGwInfo(appMan);
			if((!InitialConfig.isInitDone(shortID, gw.initDoneStatus()))) {	
				prov.initConfigResource(result.res);				
			}
		}
		result.startTimerIfNotStarted();
		return result;
	}

	@Override
	public TimedJobMemoryData unregisterTimedJobProvider(TimedJobProvider prov) {
		TimedJobMemoryData data = getProvider(prov.id());
		if(data != null && data.isRunning()) {
			return null;
		}
		return knownJobs.remove(prov.id());
	}

	@Override
	public Collection<TimedJobMemoryData> getAllProviders() {
		return knownJobs.values();
	}

	@Override
	public TimedJobMemoryData getProvider(String id) {
		return knownJobs.get(id);
	}

	protected TimedJobConfig getOrCreateConfiguration(String id, boolean isEval) {
		LocalGatewayInformation gw = ResourceHelper.getLocalGwInfo(appMan);
		if(isEval)
			return ResourceListHelper.getOrCreateNamedElementFlex(id, gw.timedJobs(), TimedEvalJobConfig.class, false);
		else
			return ResourceListHelper.getOrCreateNamedElementFlex(id, gw.timedJobs(), TimedJobConfig.class, false);
	}

	public void stop() {
		// TODO Auto-generated method stub
		
	}
	
	protected int maxPIdx = -1;
	
	/** Create persistent index if it does not exist yet*/
	protected void createPersistentIndex(TimedJobConfig config) {
		if(config.persistentIndex().exists())
			return;
		if(maxPIdx < 0) {
			LocalGatewayInformation gw = ResourceHelper.getLocalGwInfo(appMan);
			for(TimedJobConfig tj: gw.timedJobs().getAllElements()) {
				if(tj.persistentIndex().getValue() > maxPIdx)
					maxPIdx = tj.persistentIndex().getValue();
			}
		}
		maxPIdx++;
		ValueResourceHelper.setCreate(config.persistentIndex(), maxPIdx);
	}
}
