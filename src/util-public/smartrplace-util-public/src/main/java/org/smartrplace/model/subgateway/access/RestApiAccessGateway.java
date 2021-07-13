package org.smartrplace.model.subgateway.access;

import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Data;

public interface RestApiAccessGateway extends Data {
	/** ID of the gateway. Note that the URL relevant for connecting to the
	 * gateway is only stored in {@link #credentials()} */
	StringResource gatewayId();
	
	/** Encrypted JSON structure representing an object of {@link GatewayCredentials}
	 * The encryption key shall not be stored in the resource database and it must be
	 * made sure that the encryption key is NOT included into any backup file of the server.
	 * The encryption key shall only be backuped with the clear text password file of
	 * the superior instance.
	 */
	StringResource credentials();
	
	/** TODO: Is this relevant?*/
	StringResource saltKey();
}
