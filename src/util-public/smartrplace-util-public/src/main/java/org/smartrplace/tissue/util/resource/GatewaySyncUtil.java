package org.smartrplace.tissue.util.resource;

import java.util.Arrays;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatUtil;
import org.smartrplace.model.sync.mqtt.GatewaySyncData;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;

public class GatewaySyncUtil {
	public static String registerToplevelDeviceForSyncAsClient(Resource device, ApplicationManager appMan) {
		String gatewayIdBase = ViaHeartbeatUtil.getBaseGwId(GatewayUtil.getGatewayId(appMan.getResourceAccess()));
		GatewaySyncData gwSync = getGatewaySyncData(appMan, gatewayIdBase);
		if(gwSync == null)
			return null;
		String existing = getSyncEntry(gwSync, device);
		if(existing != null)
			return existing;
		SyncEntry entry = new SyncEntry(device, gatewayIdBase, gwSync.toplevelResourcesToBeSynchronized());
		String sentry = entry.getEntry();
		gwSync.toplevelResourcesToBeSynchronized().create();
		ValueResourceUtils.appendValue(gwSync.toplevelResourcesToBeSynchronized(), sentry);
		gwSync.toplevelResourcesToBeSynchronized().activate(false);
		return sentry;
	}
	
	public static GatewaySyncData getGatewaySyncDataAsClient(ApplicationManager appMan) {
		return getGatewaySyncData(appMan, null);
	}
	public static GatewaySyncData getGatewaySyncData(ApplicationManager appMan, String gatewayIdBase) {
		if((!Boolean.getBoolean("org.smartrplace.apps.subgateway")) && gatewayIdBase == null)
			throw new IllegalStateException("Only subgateway can call getGatewaySyncData without gatewayId!");
		if(gatewayIdBase == null)
			gatewayIdBase = ViaHeartbeatUtil.getBaseGwId(GatewayUtil.getGatewayId(appMan.getResourceAccess()));
		String resName = "replication_"+gatewayIdBase;
		if(Boolean.getBoolean("org.smartrplace.apps.subgateway"))
			return ResourceHelper.getOrCreateTopLevelResource(resName, GatewaySyncData.class, appMan);
		GatewaySyncData result = ResourceHelper.getTopLevelResource(resName, GatewaySyncData.class, appMan.getResourceAccess());
		if(result != null)
			return result;
		List<GatewaySyncData> allSync = appMan.getResourceAccess().getResources(GatewaySyncData.class);
		for(GatewaySyncData gws: allSync) {
			if(gws.getName().equals(resName))
				return gws;
		}
		return null;
	}
	
	public static String getSyncEntry(GatewaySyncData gws, Resource device) {
		String loc = device.getLocation();
		for(String s: gws.toplevelResourcesToBeSynchronized().getValues()) {
			SyncEntry entry = new SyncEntry(s);
			if(Arrays.asList(entry.resourcepaths).contains(loc))
				return s;
		}
		return null;
	}
	
	public static class SyncEntry {
		public SyncEntry(Resource device, String gwId, StringArrayResource existing) {
			this(getUniqueListname(device.getName(), existing), gwId, device.getLocation(), "gw"+gwId, false);
		}
		public SyncEntry(String listname, String gwId, String resourcepath, String targetpath, boolean isTargetPathRawGwId) {
			this(listname, gwId, new String[]{resourcepath}, targetpath, isTargetPathRawGwId);
		}
		//public SyncEntry(String listname, String gwId, String[] resourcepaths) {
		//	this(listname, gwId, resourcepaths, "/");
		//}
		public SyncEntry(String listname, String gwId, String[] resourcepaths, String targetpath, boolean isTargetPathRawGwId) {
			this.listname = listname;
			this.gwId = gwId;
			this.resourcepaths = resourcepaths;
			this.targetpath = isTargetPathRawGwId?("gw"+targetpath):targetpath;
		}
		public SyncEntry(String entry) {
			String[] els = entry.split(":");
			if(els.length != 4)
				throw new IllegalStateException("Invalid Sync entry:"+entry);
			this.listname = els[0].trim();
			this.gwId = els[1].trim();
			this.resourcepaths = els[2].trim().split(",");
			this.targetpath = els[3].trim();			
		}
		String listname;
		String gwId;
		String[] resourcepaths;
		String targetpath;
		
		public String getEntry() {
			return listname+":"+gwId+":"+StringFormatHelper.getListToSerialize(Arrays.asList(resourcepaths))+":"+targetpath;
		}
	}
	
	public static String getUniqueListname(String stdListName, StringArrayResource existing) {
		for(String exist: existing.getValues()) {
			try {
				SyncEntry se = new SyncEntry(exist);
				if(se.listname.equals(stdListName))
					return getUniqueListname(stdListName+"_A", existing);
			} catch(IllegalArgumentException e) {
				e.printStackTrace();
			}
		}
		return stdListName;
	}
}
