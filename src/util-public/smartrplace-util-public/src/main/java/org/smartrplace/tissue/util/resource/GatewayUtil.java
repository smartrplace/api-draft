package org.smartrplace.tissue.util.resource;

import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.model.gateway.LocalGatewayInformation;

import de.iwes.util.resource.ResourceHelper;

public class GatewayUtil {
	public static String getGatewayId(ResourceAccess resAcc, String defaultValue) {
		String result = getGatewayId(resAcc);
		if(result != null)
			return result;
		return defaultValue;
	}
	
	public static String getGatewayId(ResourceAccess resAcc) {
		LocalGatewayInformation ogGw = ResourceHelper.getLocalGwInfo(resAcc);
		if(ogGw != null && ogGw.id().exists())
			return ogGw.id().getValue();
		String idProp = System.getProperty("org.smartrplace.remotesupervision.gateway.id");
		if(idProp != null)
			return idProp;
		String idEnv = System.getenv("GATEWAY_ID");
		return idEnv;
	}
}
