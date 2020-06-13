package org.ogema.devicefinder.util;

import java.util.Collection;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.AccessPriority;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.devicefinder.api.PatternListenerExtended;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public abstract class DeviceHandlerBase<T extends Resource> implements DeviceHandlerProvider<T> {

	protected abstract Class<? extends ResourcePattern<T>> getPatternClass();

	protected PatternListenerExtended<ResourcePattern<T>, T> listener = null;
	
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
	
	@Override
	public String getDeviceName(InstallAppDevice installDeviceRes) {
		return DeviceTableRaw.getName(installDeviceRes);
	}
	
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
}


