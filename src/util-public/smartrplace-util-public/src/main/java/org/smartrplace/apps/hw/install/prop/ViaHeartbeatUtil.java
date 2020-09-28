package org.smartrplace.apps.hw.install.prop;

import java.util.ArrayList;
import java.util.List;

import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;

public class ViaHeartbeatUtil {
	public static final String VIA_HEARTBEAT_TYPE = "VIA_HEARTBT";
	public static final String VIA_HEARTBEAT_SEND = "##Transfer_via_Heartbeat_Send";
	public static final String VIA_HEARTBEAT_RECEIVE = "##Transfer_via_Heartbeat_Receive";
	
	public static void registerForTansferViaHeartbeatToGateway(Datapoint dp, String gwId,
			DatapointService dpService) {
		registerForTansferViaHeartbeatSend(dp, gwId, dpService);
	}
	public static void registerForTansferViaHeartbeatToServer(Datapoint dp, String serverUrl,
			DatapointService dpService) {
		registerForTansferViaHeartbeatSend(dp, serverUrl, dpService);
	}
	public static void registerForTansferViaHeartbeatSend(Datapoint dp, String commPartnerId,
			DatapointService dpService) {
		String id = DatapointGroup.getGroupIdForGw(VIA_HEARTBEAT_SEND, commPartnerId);
		DatapointGroup grp = dpService.getGroup(id);
		grp.setType(VIA_HEARTBEAT_TYPE);
		grp.addDatapoint(dp);
	}
	
	public static void registerForTansferViaHeartbeatFromGateway(Datapoint dp, String gwId,
			DatapointService dpService) {
		registerForTansferViaHeartbeatRecv(dp, gwId, dpService);
	}
	public static void registerForTansferViaHeartbeatFromServer(Datapoint dp, String serverUrl,
			DatapointService dpService) {
		registerForTansferViaHeartbeatRecv(dp, serverUrl, dpService);
	}
	public static void registerForTansferViaHeartbeatRecv(Datapoint dp, String commPartnerId,
			DatapointService dpService) {
		String id = DatapointGroup.getGroupIdForGw(VIA_HEARTBEAT_RECEIVE, commPartnerId);
		DatapointGroup grp = dpService.getGroup(id);
		grp.setType(VIA_HEARTBEAT_TYPE);
		grp.addDatapoint(dp);
	}

	public static void updateAllTransferRegistrations(DatapointService dpService, boolean connectingAsClient) {
		ViaHeartbeatLocalData hbData = ViaHeartbeatLocalData.getInstance();
		List<String> gwsDone = new ArrayList<String>();
		for(DatapointGroup dpg: dpService.getAllGroups()) {
			if(!"VIA_HEARTBT".equals(dpg.getType()))
				continue;
			String[] gwPlus = DatapointGroup.getGroupIdAndGw(dpg.id());
			DatapointGroup recvGroup;
			DatapointGroup sendGroup;
			String commPartnerId = gwPlus[1];
			if(gwsDone.contains(commPartnerId))
				continue;
			if(gwPlus[0].equals(VIA_HEARTBEAT_RECEIVE)) {
				recvGroup = dpg;
				String id = DatapointGroup.getGroupIdForGw(VIA_HEARTBEAT_SEND, commPartnerId);
				sendGroup = dpService.getGroup(id);
			} else {
				sendGroup = dpg;
				String id = DatapointGroup.getGroupIdForGw(VIA_HEARTBEAT_RECEIVE, commPartnerId);
				recvGroup = dpService.getGroup(id);				
			}
			ViaHeartbeartOGEMAInstanceDpTransfer partnerData = hbData.getOrCreatePartnerData(
					commPartnerId, connectingAsClient);
			partnerData.updateByDpGroups(sendGroup, recvGroup);
			gwsDone.add(commPartnerId);
		}
		//for(ViaHeartbeartOGEMAInstanceDpTransfer partnerData: hbData.partnerData.values()) {
		//	updateTransferRegistration(partnerData.commPartnerId, hbData, dpService, connectingAsClient);
		//}
	}

	/*public static void updateTransferRegistration(String commPartnerId,
			DatapointService dpService, boolean connectingAsClient) {
		updateTransferRegistration(commPartnerId, ViaHeartbeatLocalData.getInstance(),
				dpService, connectingAsClient);
	}
	public static void updateTransferRegistration(String commPartnerId,
			ViaHeartbeatLocalData hbData,
			DatapointService dpService, boolean connectingAsClient) {
		ViaHeartbeartOGEMAInstanceDpTransfer partnerData = hbData.getOrCreatePartnerData(
				commPartnerId, connectingAsClient);
		updateTransferRegistration(commPartnerId, partnerData, dpService, connectingAsClient);
	}*/
	public static void updateTransferRegistration(String commPartnerId,
				ViaHeartbeartOGEMAInstanceDpTransfer partnerData,
				DatapointService dpService, boolean connectingAsClient) {
		String id = DatapointGroup.getGroupIdForGw(VIA_HEARTBEAT_RECEIVE, commPartnerId);
		DatapointGroup recvGroup = dpService.getGroup(id);
		
		id = DatapointGroup.getGroupIdForGw(VIA_HEARTBEAT_SEND, commPartnerId);
		DatapointGroup sendGroup = dpService.getGroup(id);
		
		partnerData.updateByDpGroups(sendGroup, recvGroup);
	}
	
	//TODO: We have to provide a similar API for the gateway or move the above API so that both gateway and server can
	//use it, then gateways set gwId = null
	
	//TODO: heartbeat gateway and server must find the datapoint groups and register InfoProviders on the datapoints
	//registered providing/consuming the information transmitted. Registration of receive/send must also be initiated via
	//heartbeat when the counter action is registered there.
}
