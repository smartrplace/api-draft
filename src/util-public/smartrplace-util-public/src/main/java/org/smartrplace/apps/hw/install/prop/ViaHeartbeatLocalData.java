package org.smartrplace.apps.hw.install.prop;

import java.util.HashMap;
import java.util.Map;

import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.devicefinder.api.DatapointService;

/** All data required for communication with all partners*/
public class ViaHeartbeatLocalData {
	public ViaHeartbeatLocalData(DatapointService dpService, ResourceAccess resAcc) {
		this.dpService = dpService;
		this.resAcc = resAcc;
	}

	private static ViaHeartbeatLocalData instance = null;
	protected final DatapointService dpService;
	protected final ResourceAccess resAcc;
	public static ViaHeartbeatLocalData getInstance(DatapointService dpService, ResourceAccess resAcc) {
		if(instance == null)
			instance = new ViaHeartbeatLocalData(dpService, resAcc);
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
		ViaHeartbeartOGEMAInstanceDpTransfer result = getPartnerData(communicationPartnerId);
		/*for(String gwOpt: ViaHeartbeatUtil.getAlternativeGwIds(communicationPartnerId)) {
			result = partnerData.get(gwOpt);
			if(result != null)
				return result;
		}*/
		if(result != null)
			return result;
		result = new ViaHeartbeartOGEMAInstanceDpTransfer(communicationPartnerId, connectingAsClient, dpService, resAcc);
		partnerData.put(communicationPartnerId, result);
		return result;
	}

	public ViaHeartbeartOGEMAInstanceDpTransfer getPartnerData(String communicationPartnerId) {
		ViaHeartbeartOGEMAInstanceDpTransfer result = null;
		for(String gwOpt: ViaHeartbeatUtil.getAlternativeGwIds(communicationPartnerId)) {
			result = partnerData.get(gwOpt);
			if(result != null)
				return result;
		}
		return null;
	}
}
