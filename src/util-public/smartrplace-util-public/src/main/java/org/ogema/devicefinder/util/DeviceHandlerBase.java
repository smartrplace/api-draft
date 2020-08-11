package org.ogema.devicefinder.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.ogema.core.model.Resource;
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
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.api.PatternListenerExtended;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.Sensor;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

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
	
	protected void addDatapoint(SingleValueResource res, List<Datapoint> result, DatapointService dpService) {
		if(res.isActive())
			result.add(dpService.getDataPointStandard(res));
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
				dp.setSubRoomLocation(null, null, device.installationLocation().getValue());
		}		
	}
	
	public Collection<Datapoint> addtStatusDatapointsHomematic(PhysicalElement dev, DatapointService dpService,
			List<Datapoint> result) {
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

}


