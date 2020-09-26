package org.smartrplace.apps.hw.install.prop;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;

public class ViaHeartbeartOGEMAInstanceDpTransfer {
	/** Short transfer Id -> Local Datapoint*/
	Map<String, Datapoint> datapointsToRecvM = new HashMap<>();
	Map<Datapoint, String> datapointsToSendM = new HashMap<>();
	Map<Datapoint, ViaHeartbeatInfoProvider> infoProvidersM = new HashMap<>();
	protected int lastTransferId = 0;
	protected boolean structureUpdateToRemotePending = false;
	
	/** If false then list is for receiving datapoints. This means that the transfer ID
	 * is set and is created by the remote partner*/
	//boolean isSendList;
	
	/** See {@link ViaHeartbeatLocalData}*/
	String commPartnerId;
	boolean connectingAsClient;
	
	public ViaHeartbeartOGEMAInstanceDpTransfer(String communicationPartnerId, boolean connectingAsClient) {
		this.commPartnerId = communicationPartnerId;
		this.connectingAsClient = connectingAsClient;
	}

	public boolean receiveDatapointData(String transferId, float value) {
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
		if(dp == null) {
			//received information for unknown datapoint
			throw new IllegalStateException("Received unknown transferId: "+transferId+" Value:"+value);
		}
		ViaHeartbeatInfoProvider infoP = infoProvidersM.get(dp);
		infoP.setLastValueReceived(value);
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
		SendDatapointData subRes = new SendDatapointData();
		addDataProvider(dp, subRes, infoProviders);
		datapointsToRecv.put(subRes.transferId, dp);
		structureUpdateToRemotePending = true;
		return true;
	}
	
	public static class SendDatapointData {
		String transferId;
		Float value;
		ViaHeartbeatInfoProvider infoP;
	}
	
	/** Get data for sending. If the datapoint is not registered yet, it is registered. The datapoint
	 * is NOT added to {@link ViaHeartbeatUtil#VIA_HEARTBEAT_SEND} though, so if an update via
	 * updateByDpGroups is made, the sending configuration would be lost
	 * @param dp
	 * @param transferId may be null if not known
	 * @return
	 */
	public SendDatapointData sendDatapointData(Datapoint dp, String transferId) {
		final SendDatapointData result;
		if(transferId == null)
			result = registerDatapointForSend(dp, datapointsToSendM, null, infoProvidersM);
		else
			result = new SendDatapointData();
		result.value = result.infoP.getCurrentValueToSend();
		return result;
	}
	
	/** Register for sending and provide data for sending
	 * @param knownDps 
	 * @param infoProviders2 */
	public SendDatapointData registerDatapointForSend(Datapoint dp,
			Map<Datapoint, String> datapointsToSend, Map<Datapoint, String> knownDps,
			Map<Datapoint, ViaHeartbeatInfoProvider> infoProviders) {
		SendDatapointData result = new SendDatapointData();
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
	
	protected void addDataProvider(Datapoint dp, SendDatapointData result,
			Map<Datapoint, ViaHeartbeatInfoProvider> infoProviders) {
		result.transferId = addTransferId();
		result.infoP = new ViaHeartbeatInfoProvider() {
		};
		infoProviders.put(dp, result.infoP);
		dp.registerInfoProvider(result.infoP, 1000);
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
		public List<SendDatapointData> efficientTransferData;
		public String configJsonToSend;
	}
	
	/** To be called by heartbeat to process data received*/
	public void processRemoteData(List<SendDatapointData> dataReceived,
			String configJsonReceived, boolean connectingAsClient,
			DatapointService dpService) {
		if(configJsonReceived != null) {
			ViaHeartbeatRemoteTransferList tlist = JSONManagement.importFromJSON(configJsonReceived,
					ViaHeartbeatRemoteTransferList.class);
			if(tlist != null) {
				//TODO: We cannot handle any unsubscribes here. For this we will need separate
				//DatapointGroups for local and remote requests
				if(tlist.datapointsFromCreatorToAcceptor != null) {
					for(Entry<String, String> dpl: tlist.datapointsFromCreatorToAcceptor.entrySet()) {
						Datapoint dp = getDatapointForRemoteRequest(dpl.getValue(), connectingAsClient, dpService);
						//We cannot put into knownDatapoint directly for the updateByDpGroups that is
						//initiated by pdateTransferRegistration later, but this should have the same effect
						datapointsToRecvM.put(dpl.getKey(), dp);
						ViaHeartbeatUtil.registerForTansferViaHeartbeatRecv(dp, commPartnerId, dpService);
					}
				}
				if(tlist.datapointsFromAccptorToCreator != null) {
					for(Entry<String, String> dpl: tlist.datapointsFromAccptorToCreator.entrySet()) {
						Datapoint dp = getDatapointForRemoteRequest(dpl.getValue(), connectingAsClient, dpService);
						datapointsToSendM.put(dp, dpl.getKey());
						ViaHeartbeatUtil.registerForTansferViaHeartbeatSend(dp, commPartnerId, dpService);
					}
				}
				ViaHeartbeatUtil.updateTransferRegistration(commPartnerId, this, dpService, connectingAsClient);
			}
		}
		for(SendDatapointData recv: dataReceived) {
			receiveDatapointData(recv.transferId, recv.value);
		}
	}		
	/** To be called by heartbeat to obtain data to send*/
	public ProcessRemoteDataResult getRemoteData(boolean connectingAsClient,
			DatapointService dpService) {

		ProcessRemoteDataResult result = new ProcessRemoteDataResult();
		if(structureUpdateToRemotePending) {
			ViaHeartbeatRemoteTransferList tlist = new ViaHeartbeatRemoteTransferList();
			tlist.datapointsFromCreatorToAcceptor = new HashMap<>();
			for(Entry<Datapoint, String> dp: datapointsToSendM.entrySet()) {
				String rawId = DatapointGroup.getGroupIdAndGw(dp.getKey().id())[0];
				tlist.datapointsFromCreatorToAcceptor.put(dp.getValue(), rawId);
			}
			for(Entry<String, Datapoint> dp: datapointsToRecvM.entrySet()) {
				String rawId = DatapointGroup.getGroupIdAndGw(dp.getValue().id())[0];
				tlist.datapointsFromCreatorToAcceptor.put(dp.getKey(), rawId);
			}
			result.configJsonToSend = JSONManagement.getJSON(tlist);
			structureUpdateToRemotePending = false;
		}
		
		result.efficientTransferData = new ArrayList<>();
		for(Entry<Datapoint, String> send: datapointsToSendM.entrySet()) {
			result.efficientTransferData.add(sendDatapointData(send.getKey(), send.getValue()));
		}
		return result;
	}
	
	protected Datapoint getDatapointForRemoteRequest(String dpIdFromRemote,
			boolean connectingAsClient,
			DatapointService dpService) {
		//Should be raw datapoints, but better check
		String[] dpls = DatapointGroup.getGroupIdAndGw(dpIdFromRemote);
		final String dpId;
		//TODO: If we would allow for several hierarchies of OGEMA instances then this
		//would not work anymore
		if(connectingAsClient)
			dpId = dpls[0];
		else
			dpId = DatapointGroup.getGroupIdForGw(dpls[0], commPartnerId);
		Datapoint dp = dpService.getDataPointStandard(dpId);
		return dp;
	}
	//public static class SingleDpTransfer {
	//	 Datapoint dp;
	//	 /** Short Id*/
	//	 String transferId;
	//}
}
