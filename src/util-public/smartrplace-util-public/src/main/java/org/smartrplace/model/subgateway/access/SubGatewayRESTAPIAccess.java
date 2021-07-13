package org.smartrplace.model.subgateway.access;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

import org.json.JSONObject;

public interface SubGatewayRESTAPIAccess {
	/** Perform GET request. See {@link #performHTTPRequest(String, String, JSONObject)} for more details
	 * 
	 * @param gatewayId
	 * @param subUrl
	 * @return result of standard request converted to a JSON Object or null if this is not possible
	 */
	Future<JSONObject> performGETRequest(String gatewayId, String subUrl, String localUserName);
	
	/** Perform standard HTTP request to a gateway
	 * 
	 * @param gatewayId
	 * @param subUrl
	 * @param localUserName user name on the host system. The user name to be used for login to the remote gateway
	 * 		shall be determined by the fields superiorToGatewayUser and standardUserOnGateway of the respective
	 * 		gateway data.
	 * @param parameters
	 * @param method GET, POST, PUT, DELETE, OPTIONS (more?)
	 * @return reply of the gateway. Usually the 
	 */
	Future<String> performHTTPRequest(String gatewayId, String subUrl, String localUserName,
			JSONObject parameters, String method);
	
	/** Add user to gateway credentials. Update if these already existed. This shall trigger an update of the
	 * encrypted persistent data in {@link RestApiAccessGateway#credentials()}, but some time of caching in RAM
	 * may be foreseen to avoid too many encryption processes. Reading and decrypting may only take place on
	 * system startup.
	 * 
	 * @param userName user name on gateway
	 * @param password
	 * @param gatewayId
	 * @param isStandardUser if true then the user is made the standard user. If this is
	 * 		the first user registered for the gateway it shall be made the standard user
	 * 		independently of this parameter
	 * @return true if user has been added or udpated successfully
	 */
	boolean addGatewayUser(String userName, String password, String gatewayId,
			boolean isStandardUser);
	
	/** Add or update gateway data*/
	boolean addGatewayData(String url, String gatewayId);
	
	/** Add or update entry into {@link GatewayCredentials#superiorToGatewayUser}
	 * 
	 * @param localUserName
	 * @param remoteUserName
	 * @return
	 */
	boolean mapLocalUserName(String localUserName, String remoteUserName);
	
	/** Get known gateways (method may not be necessary as this can be accessed directly from
	 * not-encrypted resources
	 * 
	 * @return collection of known gateway ids
	 */
	Collection<String> getGateways();
	
	/** Get {@link GatewayCredentials#superiorToGatewayUser} for a gateway*/
	Map<String, String> getSuperiorToGatewayUser(String gatewayId);
}
