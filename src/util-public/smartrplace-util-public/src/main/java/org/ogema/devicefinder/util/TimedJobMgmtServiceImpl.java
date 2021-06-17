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

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;

public class TimedJobMgmtServiceImpl implements TimedJobMgmtService {
	Map<String, TimedJobMemoryData> knownJobs = new HashMap<>();
	protected final ApplicationManager appMan;
	
	public TimedJobMgmtServiceImpl(ApplicationManager appMan) {
		this.appMan = appMan;
	}

	@Override
	public TimedJobMemoryData registerTimedJobProvider(TimedJobProvider prov) {
		String id = prov.id();
		TimedJobMemoryData result = knownJobs.get(id);
		if(result == null) {
			result = new TimedJobMemoryData(appMan);
			result.prov = prov;
			result.res = getOrCreateConfiguration(id, prov.evalJobType()>0);
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
}
