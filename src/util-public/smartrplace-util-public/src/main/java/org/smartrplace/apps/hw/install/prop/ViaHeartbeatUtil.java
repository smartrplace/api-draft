package org.smartrplace.apps.hw.install.prop;

import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;

public class ViaHeartbeatUtil {
	public static final String VIA_HEARTBEAT_TYPE = "VIA_HEARTBT";
	public static final String VIA_HEARTBEAT_SEND = "##Transfer_via_Heartbeat_Send";
	public static final String VIA_HEARTBEAT_RECEIVE = "##Transfer_via_Heartbeat_Receive";
	
	public static void registerForTansferViaHeartbeatToGateway(Datapoint dp, String gwId, DatapointService dpService) {
		String id = DatapointGroup.getGroupIdForGw(VIA_HEARTBEAT_SEND, gwId);
		DatapointGroup grp = dpService.getGroup(id);
		grp.setType(VIA_HEARTBEAT_TYPE);
		grp.addDatapoint(dp);
	}
	
	public static void registerForTansferViaHeartbeatFromGateway(Datapoint dp, String gwId, DatapointService dpService) {
		String id = DatapointGroup.getGroupIdForGw(VIA_HEARTBEAT_RECEIVE, gwId);
		DatapointGroup grp = dpService.getGroup(id);
		grp.setType(VIA_HEARTBEAT_TYPE);
		grp.addDatapoint(dp);
	}

	//TODO: We have to provide a similar API for the gateway or move the above API so that both gateway and server can
	//use it, then gateways set gwId = null
	
	//TODO: heartbeat gateway and server must find the datapoint groups and register InfoProviders on the datapoints
	//registered providing/consuming the information transmitted. Registration of receive/send must also be initiated via
	//heartbeat when the counter action is registered there.
}
