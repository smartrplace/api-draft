package org.smartrplace.tissue.util.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.model.locations.Location;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatUtil;
import org.smartrplace.gateway.device.CascadingData;
import org.smartrplace.model.sync.mqtt.GatewaySyncData;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;

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
		if(Boolean.getBoolean("org.smartrplace.apps.subgateway")) {
			GatewaySyncData gwSync = ResourceHelper.getOrCreateTopLevelResource(resName, GatewaySyncData.class, appMan);
			String existing = getSyncEntry(gwSync, "rooms"); //,hardwareInstallConfig");
			if(existing == null) {
				gwSync.toplevelResourcesToBeSynchronized().create();
				String sentry = "rooms:"+gatewayIdBase+":rooms:/"; //,hardwareInstallConfig:/";
				ValueResourceUtils.appendValue(gwSync.toplevelResourcesToBeSynchronized(), sentry);
			}
			return gwSync;
		}
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
		return getSyncEntry(gws, device.getLocation());
	}
	public static String getSyncEntry(GatewaySyncData gws, String resourceLocation) {
		for(String s: gws.toplevelResourcesToBeSynchronized().getValues()) {
			SyncEntry entry = new SyncEntry(s);
			if(Arrays.asList(entry.resourcepaths).contains(resourceLocation))
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
	
	/**
	 * 
	 * @param gatewayBaseId
	 * @param resAcc
	 * @return usually of type {@link CascadingData}
	 */
	public static Resource getGatewayResource(String gatewayBaseId, ResourceAccess resAcc) {
		return resAcc.getResource("gw"+gatewayBaseId);
	}
	/**
	 * 
	 * @param gatewayBaseId
	 * @param resAcc
	 * @return usually of type {@link CascadingData}
	 */
	public static Resource getOrCreateGatewayResource(String gatewayBaseId, ApplicationManager appMan) {
		Resource result = getGatewayResource(gatewayBaseId, appMan.getResourceAccess());
		if(result != null)
			return result;
		return ResourceHelper.getOrCreateTopLevelResource("gw"+gatewayBaseId, CascadingData.class, appMan);
	}
	
	public static String getGatewayBaseId(GatewaySyncData syncData) {
		String[] els = syncData.getName().split("_");
		String last = els[els.length-1];
		if(last.length() > 3 && last.length() <= GatewayUtil.GATWAYID_MAX_LENGTH)
			return last;
		return null;
	}

	/** Update all device room locations based on {@link GatewaySyncData#deviceNames()}*/
	public static int setDeviceRoomLocations(GatewaySyncData syncData, ApplicationManager appMan) {
		String gatewayBaseId = getGatewayBaseId(syncData);
		if(gatewayBaseId == null)
			return 0;
		Resource gatewayResource = getGatewayResource(gatewayBaseId, appMan.getResourceAccess());
		if(!syncData.deviceNames().isActive())
			return 0;
		String[] entries = syncData.deviceNames().getValues();
		int count = 0;
		for(String deviceNamesEntry: entries) {
			if(setDeviceRoom(deviceNamesEntry, gatewayResource, appMan) != null)
				count++;
		}
		return count;
	}
	
	public static Room setDeviceRoom(String deviceNamesEntry, Resource gatewayResource, ApplicationManager appMan) {
		String[] els = deviceNamesEntry.split(",");
		if(els.length != 2)
			return null;
		String deviceRoomLocation = els[0];
		String roomNameOrLocation = els[1];
		return setDeviceRoom(deviceRoomLocation, roomNameOrLocation, gatewayResource, appMan);
	}
	
	/**
	 * 
	 * @param deviceRoomLocation
	 * @param roomNameOrLocation
	 * @param gatewayResource usually of type {@link CascadingData}
	 * @param appMan
	 * @return
	 */
	public static Room setDeviceRoom(String deviceRoomLocation, String roomNameOrLocation, 
			Resource gatewayResource, ApplicationManager appMan) {
		Room roomRes = ResourceHelper.getSubResource(gatewayResource, deviceRoomLocation, Room.class);
		if(roomRes == null)
			return null;
		Room destRoom = KPIResourceAccess.getRealRoomAlsoByLocation(roomNameOrLocation, appMan.getResourceAccess());
		if(destRoom == null)
			return null;
		if(roomRes.equalsLocation(destRoom))
			return null;
		roomRes.setAsReference(destRoom);
		return roomRes;
	}

	public static void writeDeviceNamesEntriesOnSuperior(GatewaySyncData gwSyncData, Resource gatewayRes) {
		StringArrayResource deviceNames = gwSyncData.deviceNames();
		List<Location> allLocations = gatewayRes.getSubResources(Location.class, true);
		for(Location loc: allLocations) {
			if(!loc.room().isReference(false))
				continue;
			writeDeviceNamesEntry(loc.room(), deviceNames, true);
		}
	}

	public static void writeDeviceNamesEntriesOnSubGw(GatewaySyncData gwSyncData, DatapointService dpService) {
		StringArrayResource deviceNames = gwSyncData.deviceNames();
		
		List<DeviceHandlerProviderDP<?>> provs = dpService.getDeviceHandlerProviders();
		for(DeviceHandlerProviderDP<?> prov: provs) {
			if(!prov.addDeviceOrResourceListToSync())
				continue;
			Collection<InstallAppDevice> allOfProv = dpService.managedDeviceResoures(prov.id(), false, true);
			for(InstallAppDevice iad: allOfProv) {
				if(iad.device().getLocation().startsWith("EvalCollection"))
					continue;
				Location loc = ((PhysicalElement)iad.device().getLocationResource()).location();
				if(!loc.room().isReference(false))
					continue;
				writeDeviceNamesEntry(loc.room(), deviceNames, false);
			}
		}
	}

	public static String writeDeviceNamesEntry(Room deviceLocationRoom, StringArrayResource deviceNames, boolean isSuperiorSystem) {
		String deviceRoomLocation;
		if(isSuperiorSystem) {
			String fullLoc = deviceLocationRoom.getPath();
			int firstDel = fullLoc.indexOf('/');
			if(firstDel < 1 || firstDel == (fullLoc.length()-1))
				return null;
			deviceRoomLocation = fullLoc.substring(firstDel+1);
		} else
			deviceRoomLocation = deviceLocationRoom.getPath();
		String roomName = ResourceUtils.getHumanReadableShortName(deviceLocationRoom);
		if(roomName.contains(","))
			roomName = deviceLocationRoom.getLocation();
		String result = deviceRoomLocation+","+roomName;

		//Remove existing
		String[] existingVals = deviceNames.getValues();
		List<String> newVals = new ArrayList<>();
		for(String deviceNamesEntry: existingVals) {
			String[] els = deviceNamesEntry.split(",");
			if(els.length != 2) {
				//Clean up corrupt entries
				continue;
			}
			if(els[0].equals(deviceRoomLocation))
				continue;
			newVals.add(deviceNamesEntry);
		}
		newVals.add(result);
		ValueResourceHelper.setCreate(deviceNames, newVals.toArray(new String[0]));
		
		return result;
	}
}
