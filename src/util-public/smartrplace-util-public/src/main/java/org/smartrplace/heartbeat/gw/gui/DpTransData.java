package org.smartrplace.heartbeat.gw.gui;

import org.ogema.devicefinder.api.Datapoint;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatInfoProvider;

public class DpTransData {
	final Datapoint dp;
	final String key;
	final boolean isToSend;
	
	final ViaHeartbeatInfoProvider infoProvider;

	public DpTransData(Datapoint dp, String key, boolean isToSend,
			ViaHeartbeatInfoProvider infoProvider) {
		this.dp = dp;
		this.key = key;
		this.isToSend = isToSend;
		this.infoProvider = infoProvider;
	}
}
