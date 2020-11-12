package org.smartrplace.apps.hw.install.prop;

import java.util.Map;

public class ViaHeartbeatRemoteTransferList {
	/** TranferID -> Datapoint ID*/
	public Map<String, String> datapointsFromCreatorToAcceptor;
	public Map<String, String> datapointsFromAccptorToCreator;
	
	/** 0: no structure request to partner<br>
	 *  1: request structure update from partner, also process maps like normal<br>
	 *  2: like 1, but the rest of the TransferList shall be ignored, this is only a reqest for a structure update<br>
	 *  This is required to request an update from the partner after startup of the system. Be careful not to set this
	 *  field otherwise to avoid triggering structure update as a ping-pong.
	 */
	public int isStructureRequestStatus;
}
