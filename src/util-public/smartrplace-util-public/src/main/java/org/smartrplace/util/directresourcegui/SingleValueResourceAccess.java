package org.smartrplace.util.directresourcegui;

import org.ogema.core.model.Resource;

public class SingleValueResourceAccess<S extends Resource> {
	public final String altIdUsed;
	public final boolean useGatewayInfo;
	public final S optSource;
	public SingleValueResourceAccess(S optSource, String altId) {
		this.optSource = optSource;
		if(altId != null) {
			if(altId.startsWith("L:")) {
				altIdUsed = altId.substring(2);
				useGatewayInfo = true;
			} else {
				altIdUsed = altId;
				useGatewayInfo = false;
			}
		} else {
			altIdUsed = null;
			useGatewayInfo = true;
		}
	}
}
