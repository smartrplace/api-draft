package org.smartrplace.apps.hw.install.prop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatInfoProvider.StringProvider;

public class ViaHeartbeartOGEMAInstanceDpTransfer {
	/** Short transfer Id -> Local Datapoint*/
	private Map<String, Datapoint> datapointsToRecvM = new HashMap<>();
	private Map<Datapoint, String> datapointsToSendM = new HashMap<>();
	Map<Datapoint, ViaHeartbeatInfoProvider> infoProvidersM = new HashMap<>();
	protected int lastTransferId = 0;
	protected boolean structureUpdateToRemotePending = true;
	protected boolean allValueUpdatePending = true;
	protected long lastStructureUpdate = -1;
	protected boolean initStructureRequestSent = false;
	
	public long lastStructUpdateReceived = -1;
	
	protected Long autoStructureUpdateRate = Long.getLong("org.smartrplace.apps.hw.install.prop.autostructureupdaterate");
	public void setAutoStructureUpdateRate(long interval) {
		this.autoStructureUpdateRate = interval;
	}
	public Long getAutoStructureUpdateRate() {
		return autoStructureUpdateRate;
	}
	
	//Process requests to datapoints that do not yet exist when the request is received
	//Usually only relevant for datapoints not representing a SingleValueResource
	// up to now only used if connectingAsClient
	public static class OpenDpRequest {
		public final String key;
		public final String dpIdFromRemote;
		public final boolean isToSend;
		public OpenDpRequest(String key, String value, boolean isToSend) {
			this.key = key;
			this.dpIdFromRemote = value;
			this.isToSend = isToSend;
		}
	}
	/** dpIdFromRemote -> object*/
	private Map<String, OpenDpRequest> openDpRequests = new HashMap<>();
	
	protected final DatapointService dpService;
	protected final ResourceAccess resAcc;
	private final Logger logger = LoggerFactory.getLogger(getClass());
	public boolean forceConsoleLogging = false;
	
	/** If false then list is for receiving datapoints. This means that the transfer ID
	 * is set and is created by the remote partner*/
	//boolean isSendList;
	
	/** See {@link ViaHeartbeatLocalData}*/
	String commPartnerId;
	boolean connectingAsClient;
	
	public ViaHeartbeartOGEMAInstanceDpTransfer(String communicationPartnerId, boolean connectingAsClient,
			DatapointService dpService, ResourceAccess resAcc) {
		this.commPartnerId = communicationPartnerId;
		this.connectingAsClient = connectingAsClient;
		this.dpService = dpService;
		this.resAcc = resAcc;
	}

	protected Datapoint getDatapointForTransferIdReceived(String transferId) {
		Datapoint dp = datapointsToRecvM.get(transferId);
		if(dp == null) {
			for(Entry<Datapoint, String> send: datapointsToSendM.entrySet()) {
				if(send.getValue().equals(transferId)) {
					dp = send.getKey();
					datapointsToRecvM.put(transferId, dp);
					break;
				}
			}
		}
		return dp;
	}
	public boolean receiveDatapointData(String transferId, float value) {
		Datapoint dp = getDatapointForTransferIdReceived(transferId);
		if(dp == null) {
			//received information for unknown datapoint
			logger.warn("Received unknown transferId: "+transferId+" Value:"+value);
			return false;
		}
		ViaHeartbeatInfoProvider infoP = getOrCreateInfoProvider(dp, transferId, infoProvidersM);
		infoP.checkMirrorResorce();
		if(infoP.getStrProv() != null) {
			//We received a single value, but a StringProvider is registered here. Most likely this is an issue with startup and synchronization
			//between clients and servers
			//infoP.getStrProv().received(strValue, now);
			logger.warn("Received value, expected StringProvider data for transferId: "+transferId+" Value:"+value);
			return false;
		} else
			infoP.setValueReceived(value, dpService.getFrameworkTime()); //.setLastValueReceived(value);
		return true;
	}
	
	public boolean registerDatapointForReceive(Datapoint dp,
		Map<String, Datapoint> datapointsToRecv, Map<Datapoint, String> knownDps,
		Map<Datapoint, ViaHeartbeatInfoProvider> infoProviders) {
		for(Entry<String, Datapoint> recv: datapointsToRecv.entrySet()) {
			if(recv.getValue().equals(dp)) {
				return false;
			}
		}
		for(Entry<Datapoint, String> send: knownDps.entrySet()) {
			if(send.getKey().equals(dp)) {
				datapointsToRecv.put(send.getValue(), dp);
				return true;
			}
		}
		SendDatapointDataPlus subRes = new SendDatapointDataPlus();
		addDataProvider(dp, subRes, infoProviders);
		datapointsToRecv.put(subRes.transferId, dp);
		structureUpdateToRemotePending = true;
		return true;
	}
	
	public static class SendDatapointDataPlus {
		String transferId;
		Float value;
		String strVal;
		ViaHeartbeatInfoProvider infoP;
	}
	
	/** Get data for sending. If the datapoint is not registered yet, it is registered. The datapoint
	 * is NOT added to {@link ViaHeartbeatUtil#VIA_HEARTBEAT_SEND} though, so if an update via
	 * updateByDpGroups is made, the sending configuration would be lost
	 * @param dp
	 * @param transferId may be null if not known
	 * @param forceSendAllValues 
	 * @return
	 */
	public SendDatapointDataPlus sendDatapointData(Datapoint dp, String transferId, boolean forceSendAllValues) {
		final SendDatapointDataPlus result;
		if(transferId == null)
			result = registerDatapointForSend(dp, datapointsToSendM, null, infoProvidersM);
		else {
			result = new SendDatapointDataPlus();
			result.infoP = getOrCreateInfoProvider(dp, transferId, infoProvidersM);
		}
		result.infoP.checkMirrorResorce();
		if(result.infoP.getStrProv() != null)
			result.strVal = result.infoP.getValueToSendString(dpService.getFrameworkTime(), forceSendAllValues); //.getCurrentValueToSend();
		else
			result.value = result.infoP.getValueToSend(dpService.getFrameworkTime(), forceSendAllValues); //.getCurrentValueToSend();
		return result;
	}
	
	/** Register for sending and provide data for sending
	 * @param knownDps 
	 * @param infoProviders2 */
	public SendDatapointDataPlus registerDatapointForSend(Datapoint dp,
			Map<Datapoint, String> datapointsToSend, Map<Datapoint, String> knownDps,
			Map<Datapoint, ViaHeartbeatInfoProvider> infoProviders) {
		SendDatapointDataPlus result = new SendDatapointDataPlus();
		result.transferId = datapointsToSend.get(dp);
		if(result.transferId == null) {
			result.transferId = knownDps.get(dp);
			if(result.transferId != null) {
				datapointsToSend.put(dp, result.transferId);
			}
		}
		/*if(result.transferId == null) {
			for(Entry<String, Datapoint> recv: datapointsToRecv.entrySet()) {
				if(recv.getValue().equals(dp)) {
					result.transferId = recv.getKey();
					datapointsToSend.put(dp, result.transferId);
					break;
				}
			}
		}*/
		if(result.transferId == null) {
			addDataProvider(dp, result, infoProviders);
			datapointsToSend.put(dp, result.transferId);
		} else
			result.infoP = infoProviders.get(dp);
		
		return result;
	}
	
	protected void addDataProvider(Datapoint dp, SendDatapointDataPlus result,
			Map<Datapoint, ViaHeartbeatInfoProvider> infoProviders) {
		result.transferId = addTransferId();
		result.infoP = getOrCreateInfoProvider(dp, result.transferId, infoProviders);
	}
	protected ViaHeartbeatInfoProvider getOrCreateInfoProvider(Datapoint dp, String transferId,
			Map<Datapoint, ViaHeartbeatInfoProvider> infoProviders) {
		ViaHeartbeatInfoProvider result = infoProviders.get(dp);
		if(result != null)
			return result;
		result = new ViaHeartbeatInfoProvider(dp, dpService);
		infoProviders.put(dp, result);
		dp.registerInfoProvider(result, 1000);
		return result;
	}
	
	protected String addTransferId() {
		lastTransferId++;
		structureUpdateToRemotePending = true;
		return String.format("%X", lastTransferId);
	}
	
	public void updateByDpGroups(DatapointGroup sendGroup, DatapointGroup recvGroup) {
		Map<String, Datapoint> datapointsToRecv = new HashMap<>();
		Map<Datapoint, String> datapointsToSend = new HashMap<>();
		Map<Datapoint, ViaHeartbeatInfoProvider> infoProviders = new HashMap<>();
		
		Map<Datapoint, String> knownDps = new HashMap<>(datapointsToSend);
		for(Entry<String, Datapoint> dp: this.datapointsToRecvM.entrySet()) {
			knownDps.put(dp.getValue(), dp.getKey());
		}
		
		for(Datapoint dp: sendGroup.getAllDatapoints()) {
			String transferId = knownDps.get(dp);
			if(transferId != null)
				datapointsToSend.put(dp, transferId);
			else {
				registerDatapointForSend(dp, datapointsToSend, knownDps, infoProviders);
			}
		}
		for(Datapoint dp: recvGroup.getAllDatapoints()) {
			String transferId = knownDps.get(dp);
			if(transferId != null)
				datapointsToRecv.put(transferId, dp);
			else {
				registerDatapointForReceive(dp, datapointsToRecv, knownDps, infoProviders);
			}
		}
		try {
			Thread.sleep(1);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		this.datapointsToRecvM = datapointsToRecv;
		this.datapointsToSendM = datapointsToSend;
		this.infoProvidersM = infoProviders;
	}
	
	public static class ProcessRemoteDataResult {
		public SendDatapointData efficientTransferData;
		public String configJsonToSend;
	}
	
	protected void processDpFromCreatorToAcceptor(Datapoint dp, String key) {
		//We cannot put into knownDatapoint directly for the updateByDpGroups that is
		//initiated by updateTransferRegistration later, but this should have the same effect
		datapointsToRecvM.put(key, dp);
		ViaHeartbeatUtil.registerForTansferViaHeartbeatRecv(dp, commPartnerId, dpService);		
	}
	protected void processDpFromAcceptorToCreator(Datapoint dp, String key) {
		datapointsToSendM.put(dp, key);
		ViaHeartbeatUtil.registerForTansferViaHeartbeatSend(dp, commPartnerId, dpService);
	}
	
	/** To be called by heartbeat to process data received*/
	public void processRemoteData(Map<String, Float> dataReceived, Map<String, String> dataReceivedString,
			String configJsonReceived, boolean connectingAsClient) {
		//configJsonReceived is null if no struct update is transmitted
		if(configJsonReceived != null) {
			lastStructUpdateReceived = dpService.getFrameworkTime();
			ViaHeartbeatRemoteTransferList tlist = JSONManagement.importFromJSON(configJsonReceived,
					ViaHeartbeatRemoteTransferList.class);
			if(tlist != null) {
//System.out.println("   Received Structure update: A2C:"+tlist.datapointsFromAccptorToCreator.size()+ " / C2A:"+tlist.datapointsFromCreatorToAcceptor.size());			
				//TODO: We cannot handle any unsubscribes here. For this we will need separate
				//DatapointGroups for local and remote requests
				if(connectingAsClient && (!(tlist.datapointsFromAccptorToCreator.isEmpty() &&
						tlist.datapointsFromCreatorToAcceptor.isEmpty()))) {
					datapointsToSendM.clear();
					datapointsToRecvM.clear();
				}
				if(tlist.isStructureRequestStatus > 0)
					structureUpdateToRemotePending = true;
				if(tlist.datapointsFromCreatorToAcceptor != null && (tlist.isStructureRequestStatus < 2)) {
					for(Entry<String, String> dpl: tlist.datapointsFromCreatorToAcceptor.entrySet()) {
						Datapoint dp = getDatapointForRemoteRequest(dpl.getValue(), connectingAsClient, dpService);
						if(dp == null) {
							openDpRequests.put(dpl.getValue(), new OpenDpRequest(dpl.getKey(), dpl.getValue(), false));
							continue;
						}
						processDpFromCreatorToAcceptor(dp, dpl.getKey());
						//We cannot put into knownDatapoint directly for the updateByDpGroups that is
						//initiated by updateTransferRegistration later, but this should have the same effect
						//datapointsToRecvM.put(dpl.getKey(), dp);
						//ViaHeartbeatUtil.registerForTansferViaHeartbeatRecv(dp, commPartnerId, dpService);
					}
				}
				if(tlist.datapointsFromAccptorToCreator != null && (tlist.isStructureRequestStatus < 2)) {
					for(Entry<String, String> dpl: tlist.datapointsFromAccptorToCreator.entrySet()) {
						Datapoint dp = getDatapointForRemoteRequest(dpl.getValue(), connectingAsClient, dpService);
						if(dp == null) {
							openDpRequests.put(dpl.getValue(), new OpenDpRequest(dpl.getKey(), dpl.getValue(), true));
							continue;
						}
						processDpFromAcceptorToCreator(dp, dpl.getKey());
						//datapointsToSendM.put(dp, dpl.getKey());
						//ViaHeartbeatUtil.registerForTansferViaHeartbeatSend(dp, commPartnerId, dpService);
					}
				}
				allValueUpdatePending = true;
				//ViaHeartbeatUtil.updateTransferRegistration(commPartnerId, this, dpService, connectingAsClient);
			} else  {
				checkOpenRequests();
			}
		} //else
//System.out.println("   Received Structure update tlist == null");			
			
		if(dataReceived != null) for(Entry<String, Float> recv: dataReceived.entrySet()) {
			try {
				receiveDatapointData(recv.getKey(), recv.getValue());
			} catch(Exception e) {
				if(connectingAsClient)
					logger.error("Could not process value received on "+recv.getKey()+" value:"+recv.getValue());
				else
					logger.error("Could not process value received from "+commPartnerId+" on "+recv.getKey()+" value:"+recv.getValue());
				e.printStackTrace();
			}
		}
		long now = dpService.getFrameworkTime();
		if(dataReceivedString != null) for(Entry<String, String> recv: dataReceivedString.entrySet()) {
			String transferId = recv.getKey();
			Datapoint dp = getDatapointForTransferIdReceived(transferId);
			if(dp == null) {
				//received information for unknown datapoint
				logger.warn("Received unknown transferId: "+transferId);
				return;
			}
			Object prov = dp.getParameter(Datapoint.HEARTBEAT_STRING_PROVIDER_PARAM);
			if(prov == null || (!(prov instanceof StringProvider))) {
				logger.warn("No StringProvider for a String received from "+ commPartnerId +" on transferId: "+transferId);
				return;
			}
			((StringProvider)prov).received(recv.getValue(), now);
		}
	}
	
	public void checkOpenRequests() {
		List<OpenDpRequest> all = new ArrayList<>(openDpRequests.values());
		for(OpenDpRequest openReq: all) {
			Datapoint dp = getDatapointForRemoteRequest(openReq.dpIdFromRemote, this.connectingAsClient, dpService);
			if(dp == null) {
				continue;
			}
			if(openReq.isToSend)
				processDpFromAcceptorToCreator(dp, openReq.key);
			else
				processDpFromCreatorToAcceptor(dp, openReq.key);
		}		
	}
	
	/** To be called by heartbeat to obtain data to send*/
	public ProcessRemoteDataResult getRemoteData(boolean connectingAsClient) {

		long now = dpService.getFrameworkTime();
		boolean forceSendAllValues = allValueUpdatePending;
		allValueUpdatePending = false;
		/**autoStructureUpdateRate always must be null on the client
		  Initially structureUpdateToRemotePending is true also on the client. The datapointsToSend/RevcM
		should be empty at this time, so this is only relevant to send isStructureRequestStatus=1*/
		if(autoStructureUpdateRate != null) {
			if(now - lastStructureUpdate > autoStructureUpdateRate)
				structureUpdateToRemotePending = true;
		}
		ProcessRemoteDataResult result = new ProcessRemoteDataResult();
		if(structureUpdateToRemotePending) {
			ViaHeartbeatRemoteTransferList tlist = new ViaHeartbeatRemoteTransferList();
			tlist.datapointsFromCreatorToAcceptor = new HashMap<>();
			for(Entry<Datapoint, String> dp: datapointsToSendM.entrySet()) {
				String rawId = getLocationOrAlias(dp.getKey()); //.id();
				tlist.datapointsFromCreatorToAcceptor.put(dp.getValue(), rawId);
			}
			tlist.datapointsFromAccptorToCreator = new HashMap<>();
			for(Entry<String, Datapoint> dp: datapointsToRecvM.entrySet()) {
				String rawId = getLocationOrAlias(dp.getValue()); //.id();
				tlist.datapointsFromAccptorToCreator.put(dp.getKey(), rawId);
			}
			
			if(!initStructureRequestSent) {
				tlist.isStructureRequestStatus = 1;
				initStructureRequestSent = true;
			}
			result.configJsonToSend = JSONManagement.getJSON(tlist);
			lastStructureUpdate = now;
			structureUpdateToRemotePending = false;
			forceSendAllValues = true;
//System.out.println("   Sending Structure update: A2C:"+tlist.datapointsFromAccptorToCreator.size()+ " / C2A:"+tlist.datapointsFromCreatorToAcceptor.size());			
		}
		
		result.efficientTransferData = new SendDatapointData();
		result.efficientTransferData.values = new HashMap<>();
		result.efficientTransferData.strings = new HashMap<>();
		for(Entry<Datapoint, String> send: datapointsToSendM.entrySet()) {
			SendDatapointDataPlus sdpPlus = sendDatapointData(send.getKey(), send.getValue(), forceSendAllValues);
			if(sdpPlus.value != null)
				result.efficientTransferData.values.put(send.getValue(), sdpPlus.value);
			if(sdpPlus.strVal != null)
				result.efficientTransferData.strings.put(send.getValue(), sdpPlus.strVal);
		}
		return result;
	}
	
	protected String getLocationOrAlias(Datapoint dp) {
		Object prov = dp.getParameter(Datapoint.HEARTBEAT_STRING_PROVIDER_PARAM);
		if(prov != null && (prov instanceof StringProvider)) {
			StringProvider sprov = (StringProvider) prov;
			String result = sprov.getAlias();
			if(result != null)
				return result;
		}
		return dp.id();
	}
	
	protected Datapoint getDatapointForRemoteRequest(String dpIdFromRemote,
			boolean connectingAsClient,
			DatapointService dpService) {
		//Should be raw datapoints, but better check
		String[] dpls = DatapointGroup.getGroupIdAndGw(dpIdFromRemote);
		//final String dpId;
		//TODO: If we would allow for several hierarchies of OGEMA instances then this
		//would not work anymore
		//if(connectingAsClient)
		//	dpId = dpls[0];
		//else
		//	dpId = DatapointGroup.getGroupIdForGw(dpls[0], commPartnerId);
		Datapoint dp = null;
		if(connectingAsClient) {
			try {
				Resource dpRes = resAcc.getResource(dpls[0]);
				if(dpRes != null && dpRes instanceof ValueResource)
					dp = dpService.getDataPointStandard((ValueResource)dpRes);
			} catch(Exception e) {}
			if(dp == null) {
				//dp = dpService.getDataPointStandard(dpls[0]);
				dp = dpService.getDataPointAsIs(dpls[0]);
				if(dp == null) {
					return null;
				}
			}
			openDpRequests.remove(dpIdFromRemote);
		} else
			dp = dpService.getDataPointStandard(dpls[0], commPartnerId);
		return dp;
	}
	
	public Map<String, Datapoint> getDatapointsToRecvM() {
		return datapointsToRecvM;
	}

	public Map<Datapoint, String> getDatapointsToSendM() {
		return datapointsToSendM;
	}

	public Map<String, OpenDpRequest> openRequests() {
		return openDpRequests;
	}
	
	//just for debugging
	public ViaHeartbeatInfoProvider getInfoProvider(Datapoint dp) {
		//Map<Datapoint, ViaHeartbeatInfoProvider> infoProviders;
		ViaHeartbeatInfoProvider result = infoProvidersM.get(dp);
		return result;
	}

}
