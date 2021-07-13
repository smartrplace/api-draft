package org.smartrplace.model.subgateway.access;

import java.util.Map;

/** Access data for a single data. Note that this data shall be stored as JSON encrypted
 * in a StringResource
 */
public class GatewayCredentials {
	/** Same id as in {@link RestApiAccessGateway#gatewayId()}
	 */
	String gatewayId;
	
	/** Base URL of the gateway, e.g. https://mygateway.smartrplace.de */
	String url;
	
	/** user logged in on the superior gateway -> user to be used for login on the sub-gateway
	 * Note that all values of this list must exist as key in the {@link #passwords} map
	 */
	Map<String, String> superiorToGatewayUser;
	
	/** If set then the standard user shall be used for login to the gateway if no
	 * fitting entry in {@link #superiorToGatewayUser} is available
	 */
	String standardUserOnGateway;
	
	/** User name -> password (for sub gateway)*/
	Map<String, String> passwords;
}
