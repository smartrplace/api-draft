package org.smartrplace.apps.hw.install.prop;

import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.util.DatapointInfoProviderImpl;
import org.ogema.tools.resource.util.ValueResourceUtils;

import de.iwes.util.resource.ValueResourceHelper;

public class ViaHeartbeatInfoProvider extends DatapointInfoProviderImpl {

	private Float lastValue = null;
	//private Float lastValueWrittenForSend = null;
	//private Float lastValueReceived = null;
	private final SingleValueResource sres;
	
	public ViaHeartbeatInfoProvider() {
		this.sres = null;
	}
	public ViaHeartbeatInfoProvider(SingleValueResource sres) {
		this.sres = sres;
	}

	@Override
	public boolean setCurrentValue(Float value) {
		if(sres != null) {
			if(value != null)
				ValueResourceUtils.setValue(sres, value);
		}
		else
			lastValue = value;
		//lastValueWrittenForSend = value;
		return true;
	}
	
	@Override
	public Float getCurrentValue() {
		if(sres != null) {
			return ValueResourceUtils.getFloatValue(sres);
		}
		else
			return lastValue;
		//return lastValueReceived;
	}

	/** The following methods shall be used by the heartbeat transfer*/
	//public void setLastValueReceived(Float value) {
	//	lastValueReceived = value;
	//}
	
	//public Float getCurrentValueToSend() {
	//	return lastValueWrittenForSend;
	//}
}
