package org.smartrplace.apps.hw.install.prop;

import org.ogema.devicefinder.util.DatapointInfoProviderImpl;

public class ViaHeartbeatInfoProvider extends DatapointInfoProviderImpl {
	private Float lastValueWrittenForSend = null;
	private Float lastValueReceived = null;
	
	@Override
	public boolean setCurrentValue(Float value) {
		lastValueWrittenForSend = value;
		return true;
	}
	
	@Override
	public Float getCurrentValue() {
		return lastValueReceived;
	}

	/** The following methods shall be used by the heartbeat transfer*/
	public void setLastValueReceived(Float value) {
		lastValueReceived = value;
	}
	
	public Float getCurrentValueToSend() {
		return lastValueWrittenForSend;
	}
}
