package org.smartrplace.intern.backup;

public class Constants {

	/**
	 * boolean value
	 */
	public static final String PROPERTY_SIMULATE_ID = "org.ogema.sim.setRemoteSupervisionId";
	
	/**
	 * Gateway id, only evaluated if not set yet or {@link #PROPERTY_SIMULATE_ID} is true
	 */
	public static final String PROPERTY_SIMULATED_ID = "org.smartrplace.remotesupervision.gateway.id";
	
	/*
	public static final String PROPERTY_SCP_HOST = "org.smartrplace.remotesupervision.master.host";
	
	public static final String DEFAULT_SCP_HOST = "sema.iee.fraunhofer.de"; 
			// "88.198.127.166"; 
	
	public static final String PROPERTY_SCP_PORT = "org.smartrplace.remotesupervision.master.port";
	
	public static final int DEFAULT_SCP_PORT = 8443;
	*/
}
