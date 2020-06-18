package org.ogema.devicefinder.util;

import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo;
import org.ogema.devicefinder.api.DatapointInfoProvider;
import org.ogema.devicefinder.api.DpConnection;
import org.ogema.devicefinder.api.OGEMADriverPropertyAccess;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.devicefinder.api.DatapointInfo.UtilityType;
import org.ogema.model.sensors.GenericFloatSensor;
import org.smartrplace.util.frontend.servlet.UserServlet;
import org.smartrplace.util.frontend.servlet.UserServletUtil;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class DatapointImpl extends DatapointDescAccessImpl implements Datapoint {
	protected final String location;
	protected String gatewayId = null;
	protected Resource resource = null;
	protected String timeSeriesID = null;
	protected OGEMADriverPropertyAccess driverAccess = null;
	protected DatapointInfoProvider infoProvider = null;
	protected int infoProviderPriority = Integer.MAX_VALUE;
	protected OgemaLogger logger = null;
	protected Resource sensorActorResource = null;
	protected Resource deviceResource = null;
	
	protected ReadOnlyTimeSeries tseries = null;
	
	//overwrite
	protected DpConnection getConnection(String connectionLocation, UtilityType type) {
		return null;
	};
	
	public DatapointImpl(String location, String gatewayId, Resource resource,
			OGEMADriverPropertyService<Resource> driverService,
			GaRoDataType garoDataType, DPRoom dpRoom, DatapointInfo consumptionInfo,
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
	public DatapointImpl(TimeSeriesDataImpl tsdi) {
		this(tsdi.id(), null, (tsdi.getTimeSeries() instanceof Resource)?(Resource)tsdi.getTimeSeries():null,
				null, null, null, null, null);
		tseries = tsdi.getTimeSeries();
		labelDefault = tsdi.label(null);
	}
	public DatapointImpl(ReadOnlyTimeSeries ts, String tsLocationOrBaseId) {
		this(ts, tsLocationOrBaseId, null);
	}
	public DatapointImpl(ReadOnlyTimeSeries ts, String tsLocationOrBaseId, String label) {
		this(ts, tsLocationOrBaseId, label, false);
	}
	public DatapointImpl(ReadOnlyTimeSeries ts, String tsLocationOrBaseId, String label,
			boolean publishViaServlet) {
		this(ts, tsLocationOrBaseId, label, false, true);
	}
	public DatapointImpl(ReadOnlyTimeSeries ts, String tsLocationOrBaseId, String label,
			boolean publishViaServlet, boolean addDefaultData) {
		this(tsLocationOrBaseId, null, (ts instanceof Resource)?(Resource)ts:null,
				null, null, null, null, null);
		if(label != null)
			this.labelDefault = label;
		setTimeSeries(ts, publishViaServlet);
		if(addDefaultData)
			addStandardData(this);
	}

	public static void addStandardData(Datapoint result) {
		if(result.getGaroDataType() == null) {
			result.setGaroDataType(GaRoEvalHelper.getDataType(result.getLocation()));
		}
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
	public String label(OgemaLocale locale) {
		String result;
		if(locale == null)
			result = labelDefault;
		else {
			result = labels.get(locale);
			if(result == null)
				result = labelDefault;
		}
		if(result != null)
			return result;
		String stdLabel = getRoomName(locale);
		String subRoom = getSubRoomLocation(locale, null);
		if(subRoom != null)
			if(stdLabel.equals(DPRoom.BUILDING_OVERALL_ROOM_LABEL))
				stdLabel = subRoom;
			else
				stdLabel += "-"+subRoom;
		stdLabel += "-"+getTypeName(locale);
		if(!isLocal()) {
			String gwId = getGatewayId();
			return gwId +"::"+stdLabel;
		}
		return stdLabel;
	}
	
	@Override
	public String getTimeSeriesID() {
		return timeSeriesID;
	}
	
	@Override
	public boolean setTimeSeriesID(String id) {
		if(id == null && timeSeriesID == null && tseries != null) {
			setTimeSeries(tseries, true);
			return true;
		}
		if(id == null)
			return false;
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
	public DatapointInfo info() {
		if(infoProvider != null) {
			DatapointInfo result = infoProvider.info();
			if(result != null)
				return result;
		}
		DatapointInfo result = super.info();
		if(result != null)
			return result;
		super.setConsumptionInfo(new DatapointInfoImpl(this));
		return super.info();
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
	@Override
	public TimeSeriesDataImpl getTimeSeriesDataImpl(OgemaLocale locale) {
		ReadOnlyTimeSeries ts = getTimeSeries();
		if(ts != null) {
			String label = label(locale);
			return new TimeSeriesDataImpl(ts, label, label, info().getInterpolationMode());
		}
		return null;
	}
	@Override
	public ReadOnlyTimeSeries getTimeSeries() {
		if(tseries != null)
			return tseries;
		if(timeSeriesID != null)
			return UserServlet.knownTS.get(timeSeriesID).getTimeSeries();
		if(resource instanceof ReadOnlyTimeSeries)
			return (ReadOnlyTimeSeries) resource;
		if(resource instanceof SingleValueResource)
			return ValueResourceHelper.getRecordedData((SingleValueResource) resource);
		return null;
	}
	@Override
	public void setTimeSeries(ReadOnlyTimeSeries tseries) {
		this.tseries = tseries;
	}
	
	@Override
	public void setTimeSeries(ReadOnlyTimeSeries tseries, boolean publishViaServlet) {
		setTimeSeries(tseries);
		if(publishViaServlet) {
			/** The label of the time series given to UserServletUtil/TimeSeriesServlet must be equal to
			 * the timeseriesID. So we cannot use the label when using the timeseries later on
			 */
			setTimeSeriesID(UserServletUtil.getOrAddTimeSeriesData(tseries, id()));
		}
	}
	@Override
	public GenericFloatSensor registerAsVirtualSensor() {
		return null;
	}
	@Override
	public GenericFloatSensor registerAsVirtualSensor(String sensorDeviceName) {
		return null;
	}

	@Override
	public boolean setScale(ScalingProvider scale) {
		this.scale = scale;
		return true;
	}
}
