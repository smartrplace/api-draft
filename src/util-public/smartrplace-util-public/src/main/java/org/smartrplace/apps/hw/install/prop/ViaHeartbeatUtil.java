package org.smartrplace.apps.hw.install.prop;

import java.util.ArrayList;
import java.util.Arrays;
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
		ViaHeartbeatLocalData hbData = ViaHeartbeatLocalData.getInstance(dpService);
		List<String> gwsDone = new ArrayList<String>();
System.out.println("Update all Transfer from groups... ("+connectingAsClient+")");
		for(DatapointGroup dpg: dpService.getAllGroups()) {
			if(!"VIA_HEARTBT".equals(dpg.getType()))
				continue;
System.out.println("Use group:"+dpg.id());
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
System.out.println("Use send group:"+sendGroup.id()+" recvGroup"+recvGroup.id());
			ViaHeartbeartOGEMAInstanceDpTransfer partnerData = hbData.getOrCreatePartnerData(
					commPartnerId, connectingAsClient);
			partnerData.updateByDpGroups(sendGroup, recvGroup);
			gwsDone.add(commPartnerId);
		}
	}
	
	public static List<String> getAlternativeGwIds(String gatewayId) {
		String baseId = getBaseGwId(gatewayId);
		return getAlternativeGwIdsForBase(baseId);
	}
	
	protected static List<String> getAlternativeGwIdsForBase(String baseId) {
		return Arrays.asList(new String[] {baseId, "_"+baseId, "gw"+baseId, "_gw"+baseId});
	}

	public static String getBaseGwId(String gatewayId) {
		if(gatewayId.startsWith("_gw"))
			return gatewayId.substring(3);
		if(gatewayId.startsWith("gw"))
			return gatewayId.substring(2);
		if(gatewayId.startsWith("_"))
			return gatewayId.substring(1);
		return gatewayId;
	}
}
