package org.smartrplace.apps.hw.install.prop;

import java.util.HashMap;
import java.util.Map;

import org.ogema.devicefinder.api.DatapointService;

/** All data required for communication with all partners*/
public class ViaHeartbeatLocalData {
	public ViaHeartbeatLocalData(DatapointService dpService) {
		this.dpService = dpService;
	}

	private static ViaHeartbeatLocalData instance = null;
	protected final DatapointService dpService;
	public static ViaHeartbeatLocalData getInstance(DatapointService dpService) {
		if(instance == null)
			instance = new ViaHeartbeatLocalData(dpService);
		return instance;
	}
	
	/** Communication partnerId -> List of datapoints*/
	Map<String, ViaHeartbeartOGEMAInstanceDpTransfer> partnerData = new HashMap<>();
	
	/**
	 * @param communicationPartnerId For partners the local instance is connecting to as heartbeat client
	 * 		the id is the server URL. For partners the local instance is connecting to as heartbeat server
	 * 		the id is the local machine user name used for the connection.
	 * @return
	 */
	public ViaHeartbeartOGEMAInstanceDpTransfer getOrCreatePartnerData(String communicationPartnerId,
			boolean connectingAsClient) {
		ViaHeartbeartOGEMAInstanceDpTransfer result = partnerData.get(communicationPartnerId);
		if(result != null)
			return result;
		result = new ViaHeartbeartOGEMAInstanceDpTransfer(communicationPartnerId, connectingAsClient, dpService);
		partnerData.put(communicationPartnerId, result);
		return result;
	}
}
