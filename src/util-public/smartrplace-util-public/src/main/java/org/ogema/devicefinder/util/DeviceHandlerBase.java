package org.ogema.devicefinder.util;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.VoltageResource;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DriverPropertySuccessHandler;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.devicefinder.api.PatternListenerExtended;
import org.ogema.devicefinder.api.PropType;
import org.ogema.model.prototypes.PhysicalElement;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import com.google.common.collect.Sets;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public abstract class DeviceHandlerBase<T extends Resource> implements DeviceHandlerProvider<T> {

	protected abstract Class<? extends ResourcePattern<T>> getPatternClass();

	protected PatternListenerExtended<ResourcePattern<T>, T> listener = null;
	public List<ResourcePattern<T>> getAllPatterns() {
		if(listener == null)
			return Collections.emptyList();
		return listener.getAllPatterns();
	}
	public ResourcePattern<T> getPattern(T device) {
		for(ResourcePattern<T> pat: getAllPatterns()) {
			if(pat.model.equalsLocation(device))
				return pat;
		}
		return null;
	}
	
	protected abstract ResourcePatternAccess advAcc();
	
	@Override
	public String id() {
		return this.getClass().getName();
	}

	@Override
	public String label(OgemaLocale locale) {
		return this.getClass().getSimpleName();
	}

	@Override
	public PatternListenerExtended<ResourcePattern<T>, T> addPatternDemand(
			InstalledAppsSelector app) {
		if(listener == null) {
			listener = new PatternListenerExtendedImpl<ResourcePattern<T>, T>(app, this);
		}
		advAcc().addPatternDemand(getPatternClass(), listener, AccessPriority.PRIO_LOWEST);
		return listener;
	}

	@Override
	public void removePatternDemand() {
		if(listener == null)
			return;
		advAcc().removePatternDemand(getPatternClass(), listener);	
	}
	
	public List<ResourcePattern<T>> getAppPatterns() {
		if(listener == null)
			return null;
		return listener.getAllPatterns();
	}
	
	protected Datapoint addDatapoint(SingleValueResource res, List<Datapoint> result, DatapointService dpService) {
		if(res.isActive()) {
			Datapoint dp = dpService.getDataPointStandard(res);
			result.add(dp);
			return dp;
		}
		return null;
	}
	protected Datapoint addDatapoint(SingleValueResource res, List<Datapoint> result,
			String subLocation, DatapointService dpService) {
		Datapoint dp = addDatapoint(res, result, dpService);
		if(dp != null) {
			dp.addToSubRoomLocationAtomic(null, null, subLocation, false);
			/*synchronized(dp) {
				String existing = dp.getSubRoomLocation(null, null);
				if(existing != null && (!existing.isEmpty()) && (!existing.contains(subLocation)))
					subLocation = existing+"-"+subLocation;
				dp.setSubRoomLocation(null, null, subLocation);
			}*/
		}
		return dp;
	}
	
	/*@Override
	public String getDeviceName(InstallAppDevice installDeviceRes) {
		return DeviceTableRaw.getName(installDeviceRes);
	}*/
	
	protected void setInstallationLocation(InstallAppDevice device, String subLoc, DatapointService dpService) {
		ValueResourceHelper.setCreate(device.installationLocation(), subLoc);
		checkDpSubLocations(device, getDatapoints(device, dpService));
	}
	
	protected void checkDpSubLocations(InstallAppDevice device, Collection<Datapoint> dps) {
		for(Datapoint dp: dps) {
			if(dp.getSubRoomLocation(null, null) == null)
				dp.addToSubRoomLocationAtomic(null, null, device.installationLocation().getValue(), true);
		}		
	}
	
	public Collection<Datapoint> addtStatusDatapointsHomematic(PhysicalElement dev, DatapointService dpService,
			List<Datapoint> result) {
		dev = dev.getLocationResource();
		VoltageResource batteryVoltage = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "battery/internalVoltage/reading", VoltageResource.class);
		if(batteryVoltage != null)
			addDatapoint(batteryVoltage, result, dpService);
		BooleanResource batteryStatus = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "batteryLow", BooleanResource.class);
		if(batteryStatus != null && batteryStatus.exists())
			addDatapoint(batteryStatus, result, dpService);
		BooleanResource comDisturbed = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "communicationStatus/communicationDisturbed", BooleanResource.class);
		if(comDisturbed != null && comDisturbed.exists())
			addDatapoint(comDisturbed, result, dpService);
		IntegerResource rssiDevice = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiDevice", IntegerResource.class);
		if(rssiDevice != null && rssiDevice.exists())
			addDatapoint(rssiDevice, result, dpService);
		IntegerResource rssiPeer = ResourceHelper.getSubResourceOfSibbling(dev,
				"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "rssiPeer", IntegerResource.class);
		if(rssiPeer != null && rssiPeer.exists())
			addDatapoint(rssiPeer, result, dpService);
		return result;
	}

	/** Get anchor resource for homematic property access
	 * 
	 * @param device
	 * @param channelName the resource name must start with this String
	 * @return
	 */
	public static Resource getAnchorResource(PhysicalElement device, String channelName) {
		Resource hmDevice = ResourceHelper.getFirstParentOfType(device, "org.ogema.drivers.homematic.xmlrpc.hl.types.HmDevice");
		if(hmDevice == null)
			return null;
		ResourceList<?> channels = hmDevice.getSubResource("channels", ResourceList.class);
		if(!channels.exists())
			return null;
		for(Resource res: channels.getAllElements()) {
			if(res.getName().startsWith(channelName))
				return res;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static void writePropertyHm(String propertyId, Resource propDev, String value,
			DriverPropertySuccessHandler<?> successHandler,
			OGEMADriverPropertyService<Resource> hmPropService,
			OgemaLogger logger) {
		if(propDev == null)
			return;
		//String propertyId = getPropId(propType);
		if(propertyId == null)
			return;
		hmPropService.writeProperty(propDev, propertyId , logger, value,
				(DriverPropertySuccessHandler<Resource>)successHandler);
		
	}

	public static class PropAccessDataHm {
		public Resource anchorRes;
		public String propId;
		
		public PropAccessDataHm(Resource anchorRes, String propId) {
			this.anchorRes = anchorRes;
			this.propId = propId;
		}
	}
}


