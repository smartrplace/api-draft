package org.ogema.devicefinder.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointInfo;
import org.ogema.devicefinder.api.DatapointInfo.UtilityType;
import org.ogema.devicefinder.api.DatapointInfoProvider;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.DpConnection;
import org.ogema.devicefinder.api.DpUpdateAPI.DpGap;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.devicefinder.api.OGEMADriverPropertyAccess;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatSchedules;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatUtil;
import org.smartrplace.util.frontend.servlet.UserServlet;
import org.smartrplace.util.frontend.servlet.UserServletUtil;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class DatapointImpl extends DatapointDescAccessImpl implements Datapoint {
	private static final long LABEL_UPDATE_RATE = 10000;
	protected final String location;
	protected String gatewayId = null;
	protected Resource resource = null;
	protected String timeSeriesID = null;
	protected OGEMADriverPropertyAccess driverAccess = null;
	protected DatapointInfoProvider infoProvider = null;
	protected int infoProviderPriority = Integer.MAX_VALUE;
	protected OgemaLogger logger = null;
	//protected Resource sensorActorResource = null;
	protected DatapointGroup deviceResource = null;
	protected Map<String, Object> parameters = new HashMap<>();
	
	protected ReadOnlyTimeSeries tseries = null;
	
	protected final DatapointService dpService;
	
	//overwrite
	protected DpConnection getConnection(String connectionLocation, UtilityType type) {
		return null;
	};
	
	public DatapointImpl(String location, String gatewayId, Resource resource,
			OGEMADriverPropertyService<Resource> driverService,
			GaRoDataType garoDataType, DPRoom dpRoom, DatapointInfo consumptionInfo,
			String subRoomLocation) {
		this(location, gatewayId, resource, driverService, garoDataType, dpRoom,
				consumptionInfo, subRoomLocation, null);
	}
	public DatapointImpl(String location, String gatewayId, Resource resource,
			OGEMADriverPropertyService<Resource> driverService,
			GaRoDataType garoDataType, DPRoom dpRoom, DatapointInfo consumptionInfo,
			String subRoomLocation, DatapointService dpService) {
		super(garoDataType, dpRoom, consumptionInfo, subRoomLocation);
		this.location = location;
		if(gatewayId != null)
			gatewayId = ViaHeartbeatUtil.getBaseGwId(gatewayId);
		this.gatewayId = gatewayId;
		if(resource != null)
			this.setResource(resource.getLocationResource());
		else
			this.setResource(null);
		this.dpService =dpService;
		setDriverService(driverService);
	}
	public DatapointImpl(String location, String gatewayId, Resource resource,
			OGEMADriverPropertyService<Resource> driverService) {
		this(location, gatewayId, resource, driverService, null, null, null, null);
	}
	public DatapointImpl(String location, String gatewayId, Resource resource,
			OGEMADriverPropertyService<Resource> driverService, DatapointService dpService) {
		this(location, gatewayId, resource, driverService, null, null, null, null, dpService);
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
	public String id() {
		return getLocation();
	}

	@Override
	public String getGatewayId() {
		return gatewayId;
	}

	@Override
	public Resource getResource() {
		return resource;
	}

	long lastLabelUpdate = -1;
	@Override
	public String label(OgemaLocale locale) {
		String result;
		if(locale == null)
			result = labelDefault;
		else {
			boolean useExist = true;
			if(dpService != null) {
				long now = dpService.getFrameworkTime();
				if(now - lastLabelUpdate > LABEL_UPDATE_RATE) {
					lastLabelUpdate = now;
					useExist = false;
				}
			}
			if(useExist) {
				result = labels.get(locale);
				if(result == null)
					result = labelDefault;
			} else
				result = null;
		}
		if(result != null)
			return result;
		String subRoom = getSubRoomLocation(locale, null);
		String stdLabel = getDeviceLabel(locale, getRoomName(locale), subRoom, isLocal()?null:getGatewayId());
		if((subRoom != null) && (getGaroDataType() == null || getGaroDataType().equals(GaRoDataType.Unknown)) && typeName.isEmpty())
			return stdLabel;
		stdLabel += "-"+getTypeName(locale);

		return stdLabel;
	}
	
	public static String getDeviceLabel(InstallAppDevice appDev, OgemaLocale locale, DatapointService dpService,
			DeviceHandlerProvider<?> tableProvider) {
		return getDeviceLabelPlus(appDev, locale, dpService, tableProvider).deviceLabel;
	}
	public static class DeviceLabelPlus {
		public String deviceLabel;
		public String devTypeShort;
		public String subLoc;
		public DPRoom room;
	}
	public static DeviceLabelPlus getDeviceLabelPlus(InstallAppDevice appDev, OgemaLocale locale, DatapointService dpService,
			DeviceHandlerProvider<?> tableProvider) {
		DeviceLabelPlus result = new DeviceLabelPlus();
		
		//String devTypeShort;
		if(tableProvider != null)
			result.devTypeShort = tableProvider.getDeviceTypeShortId(appDev, dpService);
		else {
			String[] els = appDev.deviceId().getValue().split("-");
			result.devTypeShort = els[0];
		}
		
		PhysicalElement devRes = appDev.device().getLocationResource();
		getDeviceLabelPlus(devRes, locale, dpService, appDev, result);
		return result;
	}
	
	public static void getDeviceLabelPlus(PhysicalElement devRes, OgemaLocale locale, DatapointService dpService,
			InstallAppDevice appDev, DeviceLabelPlus result) {
		Room roomRes = devRes.location().room();
		//final DPRoom room;
		if(roomRes.exists()) {
			result.room = dpService.getRoom(roomRes.getLocation());
			result.room.setResource(roomRes);
		} else 
			result.room = null;
		
		if(result.devTypeShort != null && (result.devTypeShort.equals("UNK")))
			result.devTypeShort = null;
		if(result.devTypeShort == null)
			result.devTypeShort = DeviceTableRaw.getDeviceStdName(devRes);
		
		//String subLoc = null;
		if((appDev != null) && appDev.installationLocation().isActive()) {
			if(result.devTypeShort != null)
				result.subLoc = result.devTypeShort+"-"+appDev.installationLocation().getValue();
			else
				result.subLoc = appDev.installationLocation().getValue();
		} else {
			String subName;
			if(appDev != null) {
				String devName = appDev.deviceId().getValue();
				int devnr = ScheduleViewerOpenButtonEval.getNumberById(devName);
				subName = ""+devnr;
			} else
				subName = ScheduleViewerOpenButtonEval.getDeviceShortIdPlus(devRes.getLocation()); //DeviceTableRaw.getSubNameForDevice(devRes, dpService);
			if(result.devTypeShort != null) {
				if(subName != null && (!subName.isEmpty()))
					result.subLoc = result.devTypeShort + subName;
				else
					result.subLoc = result.devTypeShort;
			} else
				result.subLoc = subName;
		}
		result.deviceLabel = DatapointImpl.getDeviceLabel(null,
				result.room!=null?result.room.label(null):Datapoint.UNKNOWN_ROOM_NAME, result.subLoc, null);
		/*PhysicalElement devRes = appDev.device();
		Room room = devRes.location().room();
		String roomName = room.exists()?ResourceUtils.getHumanReadableShortName(room):"NRI";
		return getDeviceLabel(locale, roomName, appDev.installationLocation().getValue(), null);*/
	}
	/** This should only be used for remote devices AND this is also used to generate the
	 * default DATAPOINT labels*/
	public static String getDeviceLabel(OgemaLocale locale, String roomName, String subRoom, String gwId) {
		String stdLabel = roomName;
		if(subRoom != null)  {
			if(stdLabel.equals(DPRoom.BUILDING_OVERALL_ROOM_LABEL))
				stdLabel = subRoom;
			else
				stdLabel += "-"+subRoom;
		}
		if(gwId != null) {
			String gwToUse = ViaHeartbeatUtil.getBaseGwId(gwId);
			return gwToUse +"::"+stdLabel;
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
		/*Resource devRes = getDeviceResource();
		if(devRes != null && dpService != null) {
			DatapointGroup dev = dpService.getGroup(devRes.getLocation());
			if(dev != null) {
				return "*"+dev.label(null);
			}
		}*/
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
	/*@Override
	public DatapointGroup getSensorActorResource() {
		return sensorActorResource;
	}
	@Override
	public boolean setSensorActorResource(Resource resource) {
		sensorActorResource = resource;
		return true;
	}*/
	
	@Override
	public DatapointGroup getDeviceResource() {
		return deviceResource;
	}
	@Override
	public boolean setDeviceResource(DatapointGroup resource) {
		this.deviceResource = resource;
		return true;
	}
	@Override
	public boolean setDeviceResource(Resource devResource) {
		if(dpService == null)
			return Datapoint.super.setDeviceResource(devResource);
		DatapointGroup dev = dpService.getGroup(devResource.getLocation());
		dev.setParameter(DatapointGroup.DEVICE_TYPE_FULL_PARAM, devResource.getResourceType().getName());
		if(dev.label(null) == null) {
			if(devResource instanceof PhysicalElement) {
				DeviceLabelPlus result = new DeviceLabelPlus();
				getDeviceLabelPlus((PhysicalElement) devResource, null, dpService, null, result);
				//DeviceTableRaw.getNameForDevice(devResource, dpService);
				dev.setLabel(null, result.deviceLabel);
			} else {
				@SuppressWarnings("deprecation")
				String devName = DeviceTableRaw.getNameForDevice(devResource, dpService);
				dev.setLabel(null, devName);				
			}
		}
		dev.setType("DEVICE");
		return true;
	}
	
	@Override
	public TimeSeriesDataImpl getTimeSeriesDataImpl(OgemaLocale locale) {
		ReadOnlyTimeSeries ts = getTimeSeries();
		if(ts != null) {
			String label = label(null);
			return new TimeSeriesDataImpl(ts, label, label, info().getInterpolationMode());
		}
		return null;
	}
	@Override
	public ReadOnlyTimeSeries getTimeSeries() {
		if(tseries != null)
			return tseries;
		if(timeSeriesID != null) {
			TimeSeriesDataImpl ts = UserServlet.knownTS.get(timeSeriesID);
			if(ts == null) {
				timeSeriesID = null;
				return null;
			}
			return ts.getTimeSeries();
		} if(resource instanceof ReadOnlyTimeSeries)
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

	@Override
	public Float getCurrentValue() {
		if(resource != null) try {
			return ValueResourceUtils.getFloatValue((SingleValueResource) resource);
		} catch(NumberFormatException e) {
			return null;
		}
		if(infoProvider != null)
			return infoProvider.getCurrentValue();
		return null;
	}

	@Override
	public boolean setCurrentValue(Float value) {
		if(resource != null) try {
			return ValueResourceUtils.setValue((SingleValueResource) resource, value);
		} catch(NumberFormatException e) {
			return false;
		}
		if(infoProvider != null)
			return infoProvider.setCurrentValue(value);
		return false;
	}
	

	@Override
	public Object getParameter(String id) {
		return parameters.get(id);
	}

	@Override
	public void setParameter(String id, Object param) {
		parameters.put(id, param);
	}

	/** Be very careful to use this !! Only intended to be used by DatapointService implementation*/
	public void setResource(Resource resource) {
		if(resource != null && (!location.equals(resource.getLocation())))
			throw new IllegalArgumentException("Trying to set resource "+resource.getLocation()+" for Datapoint "+location);
		this.resource = resource;
	}
	
	@Override
	public String toString() {
		return "DP:"+location;
	}

	protected final ArrayList<DpUpdated> updates = new ArrayList<>();
	
	@Override
	public void notifyTimeseriesChange(long startTime, long endTime) {
		long now = dpService.getFrameworkTime();
		
		//if the datapoint is marked for sending to a remote schedule, notify here
		//TODO: Currently we only can resend entire datapoint time series
		ViaHeartbeatSchedules sprov = (ViaHeartbeatSchedules)this.getParameter(Datapoint.HEARTBEAT_STRING_PROVIDER_PARAM);
		if(sprov != null) {
			sprov.resendAllOnNextOccasion();
		}
		
		if(!updates.isEmpty()) {
			DpUpdated last = updates.get(updates.size()-1); //.getLast();
			if(startTime <= last.end && endTime >= last.start) {
				//overlapping
				last.start = Math.min(startTime, last.start);
				last.end = Math.max(endTime, last.end);
				last.updateTime = now;
				return;
			}
		}
		DpUpdated upd = new DpUpdated();
		upd.start = startTime;
		upd.end = endTime;
		upd.updateTime = now;
		updates.add(upd);
		//Simple solution against a memory leak
		//if(updates.size() > 20) {
		//	updates.removeFirst();
		//}
		if(updates.size() > 30) {
			updates.subList(updates.size()-20, updates.size()-1).clear();
		}
	}

	@Override
	public List<DpUpdated> getIntervalsChanged(long since) {
		if(updates.isEmpty())
			return Collections.emptyList();
		/*Iterator<DpUpdated> it = updates.descendingIterator();
		while(it.hasNext()) {
			DpUpdated upd = it.next();*/
		LinkedList<DpUpdated> result = new LinkedList<>();
		boolean foundChangeBeforeSince = false;
		for(int idx=updates.size()-1; idx>=0; idx--) {
			DpUpdated upd = updates.get(idx); //.descendingIterator();
			if(upd.updateTime >= since) {
				result.addFirst(upd);
			} else {
				foundChangeBeforeSince = true;
				break;
			}			
		}
		if(!foundChangeBeforeSince) {
			DpUpdated upd = getAllInterval(updates.get(updates.size()-1).updateTime);
			return Arrays.asList(new DpUpdated[] {upd});
		}
		return result ;
	}

	@Override
	public DpUpdated getSingleIntervalChanged(long since) {
		if(updates.isEmpty())
			return null;
		List<DpUpdated> intvs = getIntervalsChanged(since);
		if(intvs.isEmpty())
			return null;
		@SuppressWarnings({ "unchecked", "rawtypes" })
		DpUpdated result = getStartEndForUpdList((List)intvs);
		return result;
	}
	
	public static DpUpdated getAllInterval(long updateTime) {
		DpUpdated upd = new DpUpdated();
		upd.start = 0;
		upd.end = Long.MAX_VALUE;
		upd.updateTime = updateTime;
		return upd;
	}
	
	public static DpUpdated getStartEndForUpdList(List<DpGap> toUpdate) {
		DpUpdated result = new DpUpdated();
		result.start = toUpdate.get(0).start;
		result.end = toUpdate.get(0).end;
		for(DpGap intv: toUpdate) {
			if(intv.start < result.start)
				result.start = intv.start;
			if(intv.end > result.end)
				result.end = intv.end;
		}
		return result;
	}
}
