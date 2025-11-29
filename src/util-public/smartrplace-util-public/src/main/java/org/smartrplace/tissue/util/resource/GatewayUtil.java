package org.smartrplace.tissue.util.resource;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.RecordableResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatUtil;
import org.smartrplace.gateway.device.GatewayDevice;

import de.iwes.util.logconfig.LogHelper;
import de.iwes.util.resource.ResourceHelper;

public class GatewayUtil {
	public static final int GATWAYID_MAX_LENGTH = 12;

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

	public static String getGatewayBaseId(ResourceAccess resAcc) {
		String pre = getGatewayId(resAcc);
		if(pre == null)
			return null;
		return ViaHeartbeatUtil.getBaseGwId(pre);
	}

	public static int getGatewayBaseIdInt(ResourceAccess resAcc) {
		String str = getGatewayBaseId(resAcc);
		if(str == null)
			return 0;
		try {
			return Integer.parseInt(str);
		} catch(NumberFormatException e) {
			return 1;
		}
	}
	
	public static String getGatewayNameFromURL(ApplicationManager appMan) {
		String baseUrl = ResourceHelper.getLocalGwInfo(appMan).gatewayBaseUrl().getValue();
		String gwName = baseUrl;
		if(gwName.startsWith("https://"))
			gwName = gwName.substring("https://".length());
		int idx = gwName.indexOf(".smartrplace.");
		if(idx >= 0)
			gwName = gwName.substring(0, idx);
		String[] els = gwName.split("-");
		gwName = null;
		boolean init = false;
		for(String el: els) {
			if(!init) {
				init = true;
				gwName = el.substring(0,1).toUpperCase() + el.substring(1);			
			} else 
				gwName += "-"+el.substring(0,1).toUpperCase() + el.substring(1);			
		}
		//if(gwName.length() >= 2)
		//	gwName = gwName.substring(0,1).toUpperCase() + gwName.substring(1);
		return gwName;
	}

	public static boolean isCloudGateway(ResourceAccess resAcc) {
		String gwId = getGatewayBaseId(resAcc);
		if(gwId.startsWith("9"))
			return true;
		//if(Boolean.getBoolean("org.smartrplace.tissue.util.resource.iscloudgw"))
		//	return true;
		//if(Boolean.getBoolean("org.ogema.devicefinder.util.supportcascadedccu"))
		//	return true;
		return false;
	}

	public static long lastGatwayActiveTime(ApplicationManager appMan) {
		GatewayDevice gw = ResourceHelper.getLocalDevice(appMan.getResourceAccess());
		if(gw == null)
			return -1;
		long now = appMan.getFrameworkTime();
		return Math.max(lastEventValueBeforeStart(gw.usedSpace_Root().reading(), now),
				lastEventValueBeforeStart(gw.gitUpdateStatus(), now));
		
	}
	private static long lastEventValueBeforeStart(RecordableResource res, long now) {
		List<SampledValue> vals = res.getHistoricalData().getValues(now-24*TimeProcUtil.HOUR_MILLIS, now-10*TimeProcUtil.MINUTE_MILLIS);
		if(vals.isEmpty())
			return -1;
		return vals.get(vals.size()-1).getTimestamp();
	}
	public static boolean isGatewayJustRestarted(ApplicationManager appMan) {
		long lastActive = lastGatwayActiveTime(appMan);
		return appMan.getFrameworkTime() - lastActive < 30*TimeProcUtil.MINUTE_MILLIS;
	}
}
