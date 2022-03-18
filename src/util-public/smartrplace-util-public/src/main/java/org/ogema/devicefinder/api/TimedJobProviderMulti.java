package org.ogema.devicefinder.api;

import java.util.List;

/** For a {@link TimedJobProviderMulti} all persistent settings are the same for all
 * {@link TimedJobMemoryData} instances running on their own. So in the basic overview
 * where all settings are made only the Multi-provider is listed. On a special page
 * all multi-instances shall be listed.
 * 
 * T type of parameter to create a new instance */
@Deprecated // not implemented, but may be relevant for evaluation in the future
public interface TimedJobProviderMulti<T> extends TimedJobProvider { 
	@Override
	default void execute(long now, TimedJobMemoryData data) {
		throw new IllegalStateException("For a multi-provider argument is required to start a running instance!");
	}
	
	/** Called by framework*/
	void execute(long now, TimedJobMemoryData data, T instanceArgument);
	
	/** Called by application
	 * TODO: Framework needs method to start a provider by ID and instanceArgument*/
	public void startForInstance(T instanceArgument);
	
	/** Init provider and get all instanceArguments to be started initially
	 * 
	 * @return all instanceArguments to be started initially
	 */
	List<T> start();
}
