package org.ogema.devicefinder.util;

import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.ConsumptionInfo;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfoProvider;
import org.ogema.devicefinder.api.OGEMADriverPropertyAccess;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class DatapointImpl extends DatapointDescAccessImpl implements Datapoint {
	protected String location;
	protected String gatewayId = null;
	protected Resource resource = null;
	protected String timeSeriesID = null;
	protected OGEMADriverPropertyAccess driverAccess = null;
	protected DatapointInfoProvider infoProvider = null;
	protected int infoProviderPriority = Integer.MAX_VALUE;
	protected OgemaLogger logger = null;
	protected Resource sensorActorResource = null;
	protected Resource deviceResource = null;
	
	public DatapointImpl(String location, String gatewayId, Resource resource,
			OGEMADriverPropertyService<Resource> driverService,
			GaRoDataType garoDataType, DPRoom dpRoom, ConsumptionInfo consumptionInfo,
			String subRoomLocation) {
		super(garoDataType, dpRoom, consumptionInfo, subRoomLocation);
		this.location = location;
		this.gatewayId = gatewayId;
		this.resource = resource;
		setDriverService(driverService);
	}
	public DatapointImpl(String location, String gatewayId, Resource resource,
			OGEMADriverPropertyService<Resource> driverService) {
		this(location, gatewayId, resource, driverService, null, null, null, null);
	}

	@Override
	public boolean setDriverService(OGEMADriverPropertyService<Resource> driverService) {
		if(resource != null && driverService != null) {
			//TODO: Probably this does not work like this, but we have to implement finding the right dataPointResource for
			//the driver later on
			Resource dataPointResource = resource;
			this.driverAccess = new OGEMADriverPropertyAccessImpl<Resource>(driverService, logger,
				dataPointResource);
			return true;
		}
		return false;
	}
	
	@Override
	public Boolean isLocal() {
		return (gatewayId == null || gatewayId.equals(GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID));
	}

	@Override
	public String getLocation() {
		return location;
	}

	@Override
	public String getGatewayId() {
		return gatewayId;
	}

	@Override
	public Resource getResource() {
		return resource;
	}

	@Override
	public String id() {
		return location;
	}

	@Override
	public String label(OgemaLocale locale) {
		return label;
	}

	@Override
	public String getTimeSeriesID() {
		return timeSeriesID;
	}
	
	@Override
	public boolean setTimeSeriesID(String id) {
		timeSeriesID = id;
		return true;
	}

	@Override
	public OGEMADriverPropertyAccess getPropertyAccess() {
		return driverAccess;
	}

	@Override
	public boolean registerInfoProvider(DatapointInfoProvider provider, int priority) {
		//TODO: Remember also providers that are not used now so that they can be used if the provider with
		//highest priority is unregistered
		if(priority < this.infoProviderPriority) {
			infoProvider = provider;
			return true;
		}		
		return false;
	}

	@Override
	public boolean unregisterInfoProvider(DatapointInfoProvider provider) {
		if(provider == infoProvider) {
			infoProvider = null;
			this.infoProviderPriority = Integer.MAX_VALUE;
			return true;
		}
		return false;
	}
	
	@Override
	public GaRoDataType getGaroDataType() {
		if(infoProvider != null) {
			GaRoDataType result = infoProvider.getGaroDataType();
			if(result != null)
				return result;
		}
		return super.getGaroDataType();
	}
	
	@Override
	public DPRoom getRoom() {
		if(infoProvider != null) {
			DPRoom result = infoProvider.getRoom();
			if(result != null)
				return result;
		}
		return super.getRoom();
	}
	
	@Override
	public String getRoomName(OgemaLocale locale) {
		DPRoom dpr = getRoom();
		if(dpr != null)
			return dpr.label(locale);
		return Datapoint.super.getRoomName(locale);
	}
	
	@Override
	public String getSubRoomLocation(OgemaLocale locale, Object context) {
		if(infoProvider != null) {
			String result = infoProvider.getSubRoomLocation(locale, context);
			if(result != null)
				return result;
		}
		return super.getSubRoomLocation(locale, context);
	}
	
	@Override
	public ConsumptionInfo getConsumptionInfo() {
		if(infoProvider != null) {
			ConsumptionInfo result = infoProvider.getConsumptionInfo();
			if(result != null)
				return result;
		}
		return super.getConsumptionInfo();
	}
	@Override
	public Resource getSensorActorResource() {
		return sensorActorResource;
	}
	@Override
	public boolean setSensorActorResource(Resource resource) {
		sensorActorResource = resource;
		return true;
	}
	
	@Override
	public Resource getDeviceResource() {
		return deviceResource;
	}
	@Override
	public boolean setDeviceResource(Resource resource) {
		this.deviceResource = resource;
		return true;
	}
}
