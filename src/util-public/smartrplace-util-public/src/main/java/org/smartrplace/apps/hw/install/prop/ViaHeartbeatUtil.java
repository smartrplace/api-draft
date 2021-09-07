package org.smartrplace.apps.hw.install.prop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.smartrplace.gateway.device.KnownIssueDataGw;

import de.iwes.util.resource.ValueResourceHelper;

public class ViaHeartbeatUtil {
	public static final String VIA_HEARTBEAT_TYPE = "VIA_HEARTBT";
	public static final String VIA_HEARTBEAT_SEND = "##Transfer_via_Heartbeat_Send";
	public static final String VIA_HEARTBEAT_RECEIVE = "##Transfer_via_Heartbeat_Receive";
	
	/** See {@link #registerForTansferViaHeartbeatFromGateway(Datapoint, String, DatapointService)}
	 * The datapoint give here is registed for transfer from server to gateway, though.
	 * @param dp
	 * @param gwId
	 * @param dpService
	 * @return
	 */
	public static DatapointGroup registerForTansferViaHeartbeatToGateway(Datapoint dp, String gwId,
			DatapointService dpService) {
		return registerForTansferViaHeartbeatSend(dp, gwId, dpService);
	}
	@Deprecated
	/** Registration of datapoints on the Gateway currently is not supported. See HeartbeatServerGwData
	 * for details.
	 */
	public static DatapointGroup registerForTansferViaHeartbeatToServer(Datapoint dp, String serverUrl,
			DatapointService dpService) {
		return registerForTansferViaHeartbeatSend(dp, serverUrl, dpService);
	}
	public static DatapointGroup registerForTansferViaHeartbeatSend(Datapoint dp, String commPartnerId,
			DatapointService dpService) {
		String id = DatapointGroup.getGroupIdForGw(VIA_HEARTBEAT_SEND, commPartnerId);
		DatapointGroup grp = dpService.getGroup(id);
		grp.setType(VIA_HEARTBEAT_TYPE);
		grp.addDatapoint(dp);
		return grp;
	}
	
	/** Register a data point for transfer from gateway to server, to be used on server<br>
	 * Note that this does NOT automatically trigger a recalculation of
	 * {@link ViaHeartbeartOGEMAInstanceDpTransfer#updateByDpGroups(DatapointGroup, DatapointGroup)}.
	 * To get this you should make sure that {{@link #updateAllTransferRegistrations(DatapointService, ResourceAccess, boolean)}
	 * is called after each block/app init calling this method.
	 * 
	 * @param dp should be remote datapoint related to gateway specified by gwId. 
	 * @param gwId TODO: Check if this is really necessary as gatewayId is also given by the datapoint
	 * @param dpService
	 * @return
	 */
	public static DatapointGroup registerForTansferViaHeartbeatFromGateway(Datapoint dp, String gwId,
			DatapointService dpService) {
		return registerForTansferViaHeartbeatRecv(dp, gwId, dpService);
	}
	@Deprecated
	/** Registration of datapoints on the Gateway currently is not supported. See HeartbeatServerGwData
	 * for details.
	 */
	public static DatapointGroup registerForTansferViaHeartbeatFromServer(Datapoint dp, String serverUrl,
			DatapointService dpService) {
		return registerForTansferViaHeartbeatRecv(dp, serverUrl, dpService);
	}
	
	public static DatapointGroup registerForTansferViaHeartbeatRecv(Datapoint dp, String commPartnerId,
			DatapointService dpService) {
		String id = DatapointGroup.getGroupIdForGw(VIA_HEARTBEAT_RECEIVE, commPartnerId);
		DatapointGroup grp = dpService.getGroup(id);
		grp.setType(VIA_HEARTBEAT_TYPE);
		grp.addDatapoint(dp);
		return grp;
	}
	
	/*public static DatapointGroup getSendOrReceiveTransferGroup(String commPartnerId, DatapointService dpService) {
		String id = DatapointGroup.getGroupIdForGw(VIA_HEARTBEAT_RECEIVE, commPartnerId);
		if(dpService.hasGroup(id))
			return dpService.getGroup(id);
		String id2 = DatapointGroup.getGroupIdForGw(VIA_HEARTBEAT_SEND, commPartnerId);
		if(dpService.hasGroup(id2))
			return dpService.getGroup(id2);
		return null;
	}*/

	public static void updateAllTransferRegistrations(DatapointService dpService, ResourceAccess resAcc, boolean connectingAsClient) {
		List<String> gwsDone = new ArrayList<String>();
System.out.println("Update all Transfer from groups... ("+connectingAsClient+")");
		for(DatapointGroup dpg: dpService.getAllGroups()) {
			if(!"VIA_HEARTBT".equals(dpg.getType()))
				continue;
System.out.println("Use group:"+dpg.id());
			updateTransferRegistration(dpg, dpService, resAcc, connectingAsClient, gwsDone);
			/*String[] gwPlus = DatapointGroup.getGroupIdAndGw(dpg.id());
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
			gwsDone.add(commPartnerId);*/
		}
	}
	
	public static void updateTransferRegistration(DatapointGroup dpg,
			DatapointService dpService, ResourceAccess resAcc, boolean connectingAsClient) {
		updateTransferRegistration(dpg, dpService, resAcc, connectingAsClient, null);
	}
	public static void updateTransferRegistration(DatapointGroup dpg, 
			DatapointService dpService, ResourceAccess resAcc,
			boolean connectingAsClient,
			List<String> gwsDone) {
		ViaHeartbeatLocalData hbData = ViaHeartbeatLocalData.getInstance(dpService, resAcc);

		String[] gwPlus = DatapointGroup.getGroupIdAndGw(dpg.id());
		DatapointGroup recvGroup;
		DatapointGroup sendGroup;
		String commPartnerId = gwPlus[1];
		if(gwsDone != null && gwsDone.contains(commPartnerId))
			return;
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
		if(gwsDone != null)
			gwsDone.add(commPartnerId);
		
	}
	
	public static List<String> getAlternativeGwIds(String gatewayId) {
		String baseId = getBaseGwId(gatewayId);
		return getAlternativeGwIdsForBase(baseId);
	}
	
	protected static List<String> getAlternativeGwIdsForBase(String baseId) {
		return Arrays.asList(new String[] {baseId, "_"+baseId, "gw"+baseId, "_gw"+baseId, "config_"+baseId, "_config_"+baseId});
	}

	public static String getBaseGwId(String gatewayId) {
		if(gatewayId.startsWith("_config_"))
			return gatewayId.substring(8);
		if(gatewayId.startsWith("config_"))
			return gatewayId.substring(7);
		if(gatewayId.startsWith("_gw"))
			return gatewayId.substring(3);
		if(gatewayId.startsWith("gw"))
			return gatewayId.substring(2);
		if(gatewayId.startsWith("_"))
			return gatewayId.substring(1);
		return gatewayId;
	}

	public static void updateEvalResources(KnownIssueDataGw kniData, ApplicationManager appMan) {
		if(kniData != null) {
			int[] alarmNum = AlarmingConfigUtil.getActiveAlarms(appMan.getResourceAccess());
			ValueResourceHelper.setCreateIfChanged(kniData.datapointsInAlarmState(), alarmNum[0]);
			ValueResourceHelper.setCreateIfChanged(kniData.activeAlarmSupervision(), alarmNum[1]);
			ValueResourceHelper.setCreateIfChanged(kniData.datapointsTotal(), alarmNum[2]);
			ValueResourceHelper.setCreateIfChanged(kniData.devicesTotal(), alarmNum[3]);
			
			int[] knis = AlarmingConfigUtil.getKnownIssues(appMan.getResourceAccess());
			ValueResourceHelper.setCreateIfChanged(kniData.knownIssuesUnassigned(), knis[0]);
			ValueResourceHelper.setCreateIfChanged(kniData.knownIssuesAssignedBattery(), knis[AlarmingConfigUtil.MAIN_ASSIGNEMENT_ROLE_NUM+2]);
			ValueResourceHelper.setCreateIfChanged(kniData.knownIssuesAssignedDevNotReacheable(), knis[AlarmingConfigUtil.MAIN_ASSIGNEMENT_ROLE_NUM+3]);
			ValueResourceHelper.setCreateIfChanged(kniData.knownIssuesAssignedSignalStrength(), knis[AlarmingConfigUtil.MAIN_ASSIGNEMENT_ROLE_NUM+4]);
			ValueResourceHelper.setCreateIfChanged(kniData.knownIssuesAssignedOther(), knis[1]+knis[5]+knis[6]+knis[7]);
			ValueResourceHelper.setCreateIfChanged(kniData.knownIssuesAssignedOperationOwn(), knis[2]);
			ValueResourceHelper.setCreateIfChanged(kniData.knownIssuesAssignedDevOwn(), knis[3]);
			ValueResourceHelper.setCreateIfChanged(kniData.knownIssuesAssignedCustomer(), knis[4]);
			ValueResourceHelper.setCreateIfChanged(kniData.knownIssuesOpExternal(), knis[AlarmingConfigUtil.MAIN_ASSIGNEMENT_ROLE_NUM]);
			ValueResourceHelper.setCreateIfChanged(kniData.knownIssuesDevExternal(), knis[AlarmingConfigUtil.MAIN_ASSIGNEMENT_ROLE_NUM+1]);
		}		
	}
}
