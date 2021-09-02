package org.ogema.devicefinder.api;

import java.util.Collection;

public interface TimedJobMgmtService {
	/** Register provider. If the ID is already known, do nothing
	 * @param prov
	 * @return If the provider is already known, just return the existing data. Otherwise return new data for the provider.
	 */
	TimedJobMemoryData registerTimedJobProvider(TimedJobProvider prov);
	
	/** Unregister provider. Usaully this should not be necessary
	 * @return data for the service unregistered. Null if job is not registered or running (cannot be unregistered then)*/
	TimedJobMemoryData unregisterTimedJobProvider(TimedJobProvider prov);
	
	Collection<TimedJobMemoryData> getAllProviders();
	
	TimedJobMemoryData getProvider(String id);
}
