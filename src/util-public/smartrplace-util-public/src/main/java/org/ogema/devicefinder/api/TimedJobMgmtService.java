package org.ogema.devicefinder.api;

import java.util.Collection;

import org.smartrplace.apps.eval.timedjob.TimedJobConfig;

public interface TimedJobMgmtService {
	/** Register provider. If the ID is already known, do nothing
	 * @param prov
	 * @return If the provider is already known, just return the existing data. Otherwise return new data for the provider.
	 */
	TimedJobMemoryData registerTimedJobProvider(TimedJobProvider prov);
	
	/** Unregister provider. Usaully this should not be necessary
	 * @return data for the service unregistered. Null if job is not registered or running (cannot be unregistered then)*/
	TimedJobMemoryData unregisterTimedJobProvider(TimedJobProvider prov);
	
	/** Delete persistent data for timed job. You have to make sure the job is not registered anymore.*/
	public TimedJobConfig deleteTimedJobProvider(String id);
	
	Collection<TimedJobMemoryData> getAllProviders();
	
	Collection<TimedJobMemoryData> getAllProviders(String type);
	
	Collection<String> getAllTypes();
	
	TimedJobMemoryData getProvider(String id);
}
