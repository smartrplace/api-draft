package org.ogema.devicefinder.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.devicefinder.api.InstallationProgressService.RouterInfo;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmInterfaceInfo;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.config.PreKnownDeviceData;

public interface InstallationProgressService {
	String getComSystemId();
	
	/** Notification that a deviceId is used*/
	//void newDeviceIdUsage(String deviceId);
	
	/** Get all PreknownDeviceData on the system or only entries that are not
	 * used by deviceIds yet
	 * @param unusedOnly
	 * @return
	 * TODO: Maybe replace by filtering
	 */
	//List<PreKnownDeviceData> preKnownDeviceData(boolean unusedOnly);
	
	public enum PreKnownDeviceDataUsageStatus {
		FAULTY,
		CONFIGURATION_PENDING_ON_ROUTER_ONLY,
		CONFIGURATION_PENDING,
		RESOURCE_CONFIGURED
	}
	public static abstract class PreknownUsage {
		public PreKnownDeviceDataUsageStatus status;
		
		/** Only relevant if used*/
		public String router;
		
		/** May be provided if device is fully configured*/
		public InstallAppDevice iad;
		
		/** true if manu confirmed and no config pending*/
		public abstract boolean isFullyConfigured();
		
		/**Clear fault status or remove from router
		 * 
		 * @return true if error occurs
		 */
		public abstract boolean remove();
		
		public abstract boolean moveToRouter(String destinationRouter);
	}
	/**
	 * 
	 * @param data
	 * @return list of PreknownUsages. If (maybe faulty) configured on more than one router may return more than one result.
	 */
	List<PreknownUsage> getUsageStatus(PreKnownDeviceData data);
	
	//boolean isDeviceSetupPending();
	
	/** Number of devices not connected correctly, usually requiring a manual interaction
	 * 
	 * @param routerAddress router resource location or address if null all known routers shall be evaluated.
	 * @return
	 */
	//int numberOfDeviceConnectionFaults(String routerAddress);
	public static class RouterInfo {
		public InstallAppDevice iad;
		public InstallAppDevice iadIP;
		public HmInterfaceInfo deviceIP;
		public InstallAppDevice iadCC;
		public HmInterfaceInfo deviceCC;
		public int faultyDevices;
		public int totalDevices;
		
		/** Devices on router, not on resources yet */
		public int routerOnlyDevices;
		
		/** Devices with base resource, but not fully configured*/
		public int unconfiguredResourceDevices;
		
		/** Total number of devices in resources including those not on the router*/
		public int totalResourceDevices;
		
		/** e.g. 0=IP / 1=CC */
		public int activeSubType;
	}
	RouterInfo getRouterInfo(String name);
	
	String getActiveRouter();
	
	/**
	 * 
	 * @param routerAddress null if teach-in mode shall be stopped for all routers
	 */
	void setActiveRouter(String routerAddress);
	
	List<String> getRouters();
	Collection<RouterInfo> getRouterInfos();
	
	/** Get Map<CCU-InstallAppDevice -> CCU-shortID for GUI*/
	Map<InstallAppDevice, String> getValuesToSet();

	/** 0 = off, all unselected routers also off<br>
	 *  1 = on, all unselected routers off<br>
	 *  11+: selected router off, value-10 indicated number of unselected routers that are on
	 *  21+: selected router on, value-10 indicated number of unselected routers that are on
	 */
	int getInstallationModeStatus();
	
	/** 0 = IP
	 *  1 = CC
	 *  2 = IP Analysis only (no teach in mode activated)
	 *  3 = CC Analysis only (no teach in mode activated)
	 * @return
	 */
	int getActiveSubType();
	void setActiveSubType(int type);
	
	/*public interface RouterData {
		String getAddress();
		String deviceId();
		InstallAppDevice getIAD();
		Resource getDeviceResource();
	}*/
	
	/*public interface InstallationProgress {
		List<InstallationProgress> followUpIds();
	}
	InstallationProgress getProgressState();
	InstallationProgress performProgress(String selection);*/
}
