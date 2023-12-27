package org.smartrplace.util.virtualdevice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.array.StringArrayResource;
import org.ogema.core.model.array.TimeArrayResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.PercentageResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProviderDP;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmInterfaceInfo;
import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;
import org.ogema.externalviewer.extensions.IntervalConfiguration;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButton;
import org.ogema.externalviewer.extensions.ScheduleViwerOpenUtil;
import org.ogema.externalviewer.extensions.ScheduleViwerOpenUtil.SchedOpenDataProvider;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.gateway.remotesupervision.DataLogTransferInfo;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.Configuration;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.CO2Sensor;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.model.sensors.HumiditySensor;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.resource.util.LoggingUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gateway.device.GatewayDevice;
import org.smartrplace.gateway.device.KnownIssueDataGw;
import org.smartrplace.gateway.device.MemoryTimeseriesPST;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.html.form.label.LabelData;

public class ChartsUtil {
	public static Collection<InstallAppDevice> getCCUs(DatapointService dpService) {
		Collection<InstallAppDevice> result = DpGroupUtil.managedDeviceResoures("HmInterfaceInfo", dpService);
		return result ;
	}
	
	public static Collection<InstallAppDevice> getHAPs(DatapointService dpService) {
		Collection<InstallAppDevice> result = new ArrayList<>();
		Collection<InstallAppDevice> senss = dpService.managedDeviceResoures(SensorDevice.class);
		for(InstallAppDevice sens: senss) {
			if(DeviceTableRaw.isHAPDevice(sens.device().getLocation(), null))
				result.add(sens);
		}
		return result ;
	}
	
	public static Collection<InstallAppDevice> getControllers(DatapointService dpService) {
		Collection<InstallAppDevice> result = DpGroupUtil.managedDeviceResoures("org.smartrplace.router.model.GlitNetRouter", dpService);
		return result ;
	}
	
	public static Collection<InstallAppDevice> getRoomcontrolRooms(DatapointService dpService) {
		Collection<InstallAppDevice> all = dpService.managedDeviceResoures(Room.class);
		List<InstallAppDevice> result = new ArrayList<>();
		for(InstallAppDevice iad: all) {
			if(iad.realDevice().exists() &&
					(iad.realDevice().getResourceType().getName().equals("org.smartrplace.apps.heatcontrol.config.RoomTemperatureSetting"))) {
				result.add(iad);
			}
		}
		return result ;
	}

	public static InstallAppDevice getGateway(DatapointService dpService) {
		Collection<InstallAppDevice> result = dpService.managedDeviceResoures(GatewayDevice.class);
		if(result.size() > 1)
			throw new IllegalStateException("Found more than one gateway in InstalledAppDevices:"+result.size());
		if(result.isEmpty())
			return null;
		return result.iterator().next();
	}

	public static InstallAppDevice getJobSupervisionPST(DatapointService dpService) {
		Collection<InstallAppDevice> result = dpService.managedDeviceResoures(MemoryTimeseriesPST.class);
		if(result.size() > 2)
			throw new IllegalStateException("Found more than one resource of type MemoryTimeseriesPST in InstalledAppDevices:"+result.size());
		if(result.size() == 2) {
			for(InstallAppDevice iad: result) {
				Resource parent = iad.device().getLocationResource().getParent();
				if(parent == null || (!(parent instanceof KnownIssueDataGw)))
					return iad;
			}
			return null;
		}
		if(result.isEmpty())
			return null;
		return result.iterator().next();
	}
	
	public static Collection<FloatResource> getCO2Sensors(DatapointService dpService) {
		Collection<FloatResource> result = new ArrayList<>();
		Collection<InstallAppDevice> senss = dpService.managedDeviceResoures(SensorDevice.class);
		for(InstallAppDevice sens: senss) {
			if(DeviceTableRaw.isSmartProtectDevice(sens.device().getLocation()))
				result.add(((SensorDevice)sens.device()).sensors().getSubResource("co2", CO2Sensor.class).reading());
			if(DeviceTableRaw.isCO2wMBUSDevice(sens.device().getLocation(), DeviceTableRaw.getSubResInfo(sens.device())))
				result.add(sens.device().getSubResource("USER_DEFINED_0_0", GenericFloatSensor.class).reading());
		}
		Collection<InstallAppDevice> hms = dpService.managedDeviceResoures(CO2Sensor.class);
		for(InstallAppDevice sens: hms) {
			result.add(((CO2Sensor)sens.device()).reading());
		}
		return result ;
	}
	
	public static class HmCO2SensorElements {
		public CO2Sensor co2Sens;
		public TemperatureSensor tempSens;
		public HumiditySensor humSens;
		public OnOffSwitch onOffSwitch;
		
		/** Null if directly acquired via {@link ChartsUtil#getElements(CO2Sensor)}*/
		public InstallAppDevice iad;
	}
	public static HmCO2SensorElements getElements(CO2Sensor co2) {
		Resource dev = co2.getLocationResource().getParent();
		HmCO2SensorElements result = new HmCO2SensorElements();
		result.co2Sens = co2;
		if(dev == null)
			return result;
		//List<Resource> allSubs = dev.sensors().getSubResources(false);
		List<TemperatureSensor> tss = dev.getSubResources(TemperatureSensor.class, false);
		if(tss.size() == 1)
			result.tempSens = tss.get(0);
		List<HumiditySensor> hss = dev.getSubResources(HumiditySensor.class, false);
		if(hss.size() == 1)
			result.humSens = hss.get(0);
		if(result.humSens == null && result.tempSens == null) {
			List<SensorDevice> sensDev = dev.getSubResources(SensorDevice.class, false);
			if(sensDev.size() == 1) {
				tss = sensDev.get(0).getSubResources(TemperatureSensor.class, true);
				if(tss.size() == 1)
					result.tempSens = tss.get(0);
				hss = sensDev.get(0).getSubResources(HumiditySensor.class, true);
				if(hss.size() == 1)
					result.humSens = hss.get(0);				
			}
		}
		
		List<OnOffSwitch> oos = dev.getSubResources(OnOffSwitch.class, false);
		if(hss.size() == 1)
			result.onOffSwitch = oos.get(0);
		return result ;
	}
	public static List<HmCO2SensorElements> getHmCO2SensorData(DatapointService dpService) {
		Collection<InstallAppDevice> input = dpService.managedDeviceResoures(CO2Sensor.class);
		List<HmCO2SensorElements> result = new ArrayList<>();
		for(InstallAppDevice co2: input) {
			HmCO2SensorElements data = getElements((CO2Sensor)co2.device());
			data.iad = co2;
			result.add(data);
		}
		return result;
	}
	
	public static Label getDutyCycleLabel(HmInterfaceInfo device, InstallAppDevice deviceConfiguration,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id_vh) {
		return getDutyCycleLabel("dutyCycleLb", deviceConfiguration, vh.getParent(), vh.getReq(), id_vh, device.dutyCycle().reading());
	}
	public static Label getDutyCycleLabel(String preId, InstallAppDevice deviceConfiguration,
			OgemaWidget parent, OgemaHttpRequest req, String id_vh,
			FloatResource dutyCycleRes) {
		PercentageResource res = deviceConfiguration.getSubResource(HmSetpCtrlManagerTHSetp.dutyCycleYellowMin, PercentageResource.class);
		float minYellow;
		float minRed;
		if(res.isActive())
			minYellow = res.getValue();
		else
			minYellow = SetpointControlManager.CONDITIONAL_PRIO_DEFAULT;
		 res = deviceConfiguration.getSubResource(HmSetpCtrlManagerTHSetp.dutyCycleRedMin, PercentageResource.class);
		if(res.isActive())
			minRed = res.getValue();
		else
			minRed = SetpointControlManager.PRIORITY_PRIO_DEFAULT;
		Label dutyCycleLb = new Label(parent, preId+id_vh, req) {
			private static final long serialVersionUID = 6380831122071345220L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				float val = dutyCycleRes.getValue();
				if(val > minRed) {
					removeStyle(LabelData.BOOTSTRAP_ORANGE, req);
					addStyle(LabelData.BOOTSTRAP_RED, req);
				} else if(val > minYellow) {
					removeStyle(LabelData.BOOTSTRAP_RED, req);
					addStyle(LabelData.BOOTSTRAP_ORANGE, req);
				} else {
					removeStyle(LabelData.BOOTSTRAP_ORANGE, req);
					removeStyle(LabelData.BOOTSTRAP_RED, req);
					addStyle(LabelData.BOOTSTRAP_GREEN, req);
				}
				setText(String.format("%.0f%%", val*100), req);
			}
		};
		return dutyCycleLb;		
	}
	
	public static Label getCarrierSensLabel(String preId, InstallAppDevice deviceConfiguration,
			OgemaWidget parent, OgemaHttpRequest req, String id_vh,
			FloatResource dutyCycleRes) {
		float minYellow = 0.18f;
		float minRed = 0.3f;
		Label carrierSensLb = new Label(parent, preId+id_vh, req) {
			private static final long serialVersionUID = 6380831122071345220L;

			@Override
			public void onGET(OgemaHttpRequest req) {
				float val = dutyCycleRes.getValue();
				if(val > minRed) {
					removeStyle(LabelData.BOOTSTRAP_ORANGE, req);
					addStyle(LabelData.BOOTSTRAP_RED, req);
				} else if(val > minYellow) {
					removeStyle(LabelData.BOOTSTRAP_RED, req);
					addStyle(LabelData.BOOTSTRAP_ORANGE, req);
				} else {
					removeStyle(LabelData.BOOTSTRAP_ORANGE, req);
					removeStyle(LabelData.BOOTSTRAP_RED, req);
					addStyle(LabelData.BOOTSTRAP_GREEN, req);
				}
				setText(String.format("%.0f%%", val*100), req);
			}
		};
		return carrierSensLb;		
	}

	public static void getDutyCycleLabelOnGET(InstallAppDevice deviceConfiguration,
			FloatResource dutyCycleRes,
			Label label, OgemaHttpRequest req) {
		PercentageResource res = deviceConfiguration.getSubResource(HmSetpCtrlManagerTHSetp.dutyCycleYellowMin, PercentageResource.class);
		float minYellow;
		float minRed;
		if(res.isActive())
			minYellow = res.getValue();
		else
			minYellow = SetpointControlManager.CONDITIONAL_PRIO_DEFAULT;
		 res = deviceConfiguration.getSubResource(HmSetpCtrlManagerTHSetp.dutyCycleRedMin, PercentageResource.class);
		if(res.isActive())
			minRed = res.getValue();
		else
			minRed = SetpointControlManager.PRIORITY_PRIO_DEFAULT;
		float val = dutyCycleRes.getValue();
		if(val > minRed) {
			label.removeStyle(LabelData.BOOTSTRAP_ORANGE, req);
			label.addStyle(LabelData.BOOTSTRAP_RED, req);
		} else if(val > minYellow) {
			label.removeStyle(LabelData.BOOTSTRAP_RED, req);
			label.addStyle(LabelData.BOOTSTRAP_ORANGE, req);
		} else {
			label.removeStyle(LabelData.BOOTSTRAP_ORANGE, req);
			label.removeStyle(LabelData.BOOTSTRAP_RED, req);
			label.addStyle(LabelData.BOOTSTRAP_GREEN, req);
		}
		label.setText(String.format("%.0f%%", val*100), req);	
	}
	
	public static class GetPlotButtonResult {
		public DeviceHandlerProviderDP<?> devHand;
		public Collection<Datapoint> datapoints2;
		public Label dataPointInfoLabel;
		public ScheduleViewerOpenButton plotButton;
	}
	
	public static final String DATAPOINT_INFO_HEADER = "DP/Log/Transfer/Tmpl";
	/** Create widgets for a plotButton. The dataPointInfoLabel is directly added to the row if requested,
	 * the plotButton needs to be added to the row by separate operation
	 * 
	 * @param id
	 * @param controller
	 * @param vh
	 * @param req
	 * @return
	 */
	public static GetPlotButtonResult getPlotButton(String id, final InstallAppDevice object,
			final DatapointService dpService, final ApplicationManager appMan,//final HardwareInstallController controller2,
			ObjectResourceGUIHelper<?, ?> vh, Row row, OgemaHttpRequest req,
			DefaultScheduleViewerConfigurationProviderExtended schedViewProv) {
		DeviceHandlerProviderDP<Resource> devHand = dpService.getDeviceHandlerProvider(object);
		return getPlotButton(id, object, dpService, appMan, false, vh, row, req,
				devHand, schedViewProv, null);
	}
	public static GetPlotButtonResult getPlotButton(String id, final InstallAppDevice object,
			final DatapointService dpService, final ApplicationManager appMan,//final HardwareInstallController controller2,
			boolean addDataPointInfoLabel,
			ObjectResourceGUIHelper<?, ?> vh, Row row, OgemaHttpRequest req,
			DeviceHandlerProviderDP<?> devHand,
			DefaultScheduleViewerConfigurationProviderExtended schedViewProv,
			ResourceList<DataLogTransferInfo> datalogs) {
		return getPlotButton(id, object, dpService, appMan, addDataPointInfoLabel, vh, row, req, devHand, schedViewProv, datalogs, null,
				vh.getParent());
	}
	
	public static GetPlotButtonResult getPlotButtonBase(String id, final InstallAppDevice object,
			final DatapointService dpService, final ApplicationManager appMan,
			OgemaWidget parent, Row row, OgemaHttpRequest req,
			DefaultScheduleViewerConfigurationProviderExtended schedViewProv) {
		DeviceHandlerProviderDP<Resource> devHand = dpService.getDeviceHandlerProvider(object);
		return getPlotButton(id, object, dpService, appMan, false, null, row, req, devHand, schedViewProv, null, null, parent);
	}

	public static GetPlotButtonResult getPlotButton(String id, final InstallAppDevice object,
			final DatapointService dpService, final ApplicationManager appMan,//final HardwareInstallController controller2,
			boolean addDataPointInfoLabel,
			ObjectResourceGUIHelper<?, ?> vh, Row row, OgemaHttpRequest req,
			DeviceHandlerProviderDP<?> devHand,
			DefaultScheduleViewerConfigurationProviderExtended schedViewProv,
			ResourceList<DataLogTransferInfo> datalogs,
			Collection<Datapoint> datapointsToUse) {
		return getPlotButton(id, object, dpService, appMan, addDataPointInfoLabel, vh, row, req, devHand, schedViewProv, datalogs, datapointsToUse,
				vh.getParent());
	}

	public static GetPlotButtonResult getPlotButton(String id, final InstallAppDevice object,
			final DatapointService dpService, final ApplicationManager appMan,//final HardwareInstallController controller2,
			boolean addDataPointInfoLabel,
			ObjectResourceGUIHelper<?, ?> vh, Row row, OgemaHttpRequest req,
			DeviceHandlerProviderDP<?> devHand,
			DefaultScheduleViewerConfigurationProviderExtended schedViewProv,
			ResourceList<DataLogTransferInfo> datalogs,
			Collection<Datapoint> datapointsToUse, OgemaWidget parent) {
		IntervalConfiguration itv = IntervalConfiguration.getDefaultDuration(IntervalConfiguration.ONE_DAY, appMan);
		return getPlotButton(id, object, dpService, appMan, addDataPointInfoLabel, vh, row, req, devHand, schedViewProv,
				datalogs, datapointsToUse, itv, parent, "Plot");
	}
	/**
	 * 
	 * @param id
	 * @param object
	 * @param dpService
	 * @param appMan
	 * @param addDataPointInfoLabel
	 * @param vh
	 * @param row
	 * @param req
	 * @param devHand
	 * @param schedViewProv
	 * @param datalogs
	 * @param datapointsToUse if null the datapoints are determined baed on devHand and object. Otherwise the datapoints specified here
	 * 		are used. Note that this is not relevant if addDataPointInfoLabel is true.
	 * @return
	 */
	public static GetPlotButtonResult getPlotButton(String id, final InstallAppDevice object,
			final DatapointService dpService, final ApplicationManager appMan,//final HardwareInstallController controller2,
			boolean addDataPointInfoLabel,
			ObjectResourceGUIHelper<?, ?> vh, Row row, OgemaHttpRequest req,
			DeviceHandlerProviderDP<?> devHand,
			DefaultScheduleViewerConfigurationProviderExtended schedViewProv,
			ResourceList<DataLogTransferInfo> datalogs,
			Collection<Datapoint> datapointsToUse, IntervalConfiguration itv, OgemaWidget parent,
			String buttonText) {
		final GetPlotButtonResult resultMain = new GetPlotButtonResult();
		
		resultMain.devHand = devHand;
		if(resultMain.devHand != null || datapointsToUse != null) {
			if(!addDataPointInfoLabel)
				resultMain.datapoints2 = null;
			else  {
				if(datapointsToUse != null)
					resultMain.datapoints2 = datapointsToUse;
				else
					resultMain.datapoints2 = resultMain.devHand.getDatapoints(object, dpService);
				int logged = 0;
				int transferred = 0;
				for(Datapoint dp: resultMain.datapoints2) {
					ReadOnlyTimeSeries ts = dp.getTimeSeries();
					if(ts == null || (!(ts instanceof RecordedData)))
						continue;
					RecordedData rec = (RecordedData)ts;
					if(LoggingUtils.isLoggingEnabled(rec))
						logged++;
					if(Boolean.getBoolean("org.smartrplace.app.srcmon.isgateway")) {
						Resource res = appMan.getResourceAccess().getResource(rec.getPath());
						if(res != null && (res instanceof SingleValueResource) && (datalogs != null)) { // &&
								//LogTransferUtil.isResourceTransferred((SingleValueResource) res, datalogs)) {
							transferred++;
						}
					}
				}
				String text = ""+resultMain.datapoints2.size()+"/"+logged+"/"+transferred;
				final boolean isTemplate = resultMain.devHand != null && DeviceTableRaw.isTemplate(object, resultMain.devHand);
				if(isTemplate) {
					text += "/T";
				}
				if(addDataPointInfoLabel)
					resultMain.dataPointInfoLabel = vh.stringLabel(DATAPOINT_INFO_HEADER, id, text, row);
			}
			
			SchedOpenDataProvider provider = new SchedOpenDataProvider() {
				
				@Override
				public IntervalConfiguration getITVConfiguration() {
					return itv; //IntervalConfiguration.getDefaultDuration(itv, appMan);
				}
				
				@Override
				public List<TimeSeriesData> getData(OgemaHttpRequest req) {
					List<TimeSeriesData> result = new ArrayList<>();
					OgemaLocale locale = req!=null?req.getLocale():null;
					if(resultMain.datapoints2 == null) {
						if(datapointsToUse != null)
							resultMain.datapoints2 = datapointsToUse;
						else
							resultMain.datapoints2 = resultMain.devHand.getDatapoints(object, dpService);
					}
					for(Datapoint dp: resultMain.datapoints2) {
						TimeSeriesDataImpl tsd = dp.getTimeSeriesDataImpl(locale);
						if(tsd == null)
							continue;
						TimeSeriesDataExtendedImpl tsdExt = new TimeSeriesDataExtendedImpl(tsd, tsd.label(null), tsd.description(null));
						tsdExt.type = dp.getGaroDataType();
						result.add(tsdExt);
					}
					return result;
				}
			};
			resultMain.plotButton = ScheduleViwerOpenUtil.getScheduleViewerOpenButton(parent, "plotButton"+id,
					buttonText!=null?buttonText:"Plot", provider, schedViewProv, req);
		}
		return resultMain;
	}
	
	public static Configuration getRoomTemperatureSetting(Room room, ResourceAccess resAcc) {
		Resource srcConfig = resAcc.getResource("smartrplaceHeatcontrolConfig");
		if(srcConfig == null)
			return null;
		ResourceList<?> roomData = srcConfig.getSubResource("roomData", ResourceList.class);
		if(roomData == null || (!roomData.exists()))
			return null;
		for(Resource r: roomData.getAllElements()) {
			if((!(r instanceof Configuration)))
				continue;
			if(r.getSubResource("room", Room.class).equalsLocation(room))
				return (Configuration) r;
		}
		return null;
	}

	public static List<String> getHistoricalRoomLocation(long start, long end, PhysicalElement device) {
		StringArrayResource history = device.location().roomHistory();
		if(!history.isActive())
			return null;
		TimeArrayResource historyStart = device.location().roomStart();
		int len = Math.min(history.size(), historyStart.size());
		if(len == 0)
			return null;
		List<String> result = new ArrayList<>();
		long[] startTimes = historyStart.getValues();
		String[] rooms = history.getValues();
		for(int idx=0; idx<len; idx++) {
			long roomStart = startTimes[idx];
			long roomEnd;
			if(idx < (len-1))
				roomEnd = startTimes[idx+1];
			else
				roomEnd = Long.MAX_VALUE;
			if(((roomStart < start && roomEnd > end)
					|| (roomStart >= start && roomStart <= end)
					|| (roomEnd >= start && roomEnd <= end))
					&& rooms[idx] != null && (!rooms[idx].isBlank()))
				result.add(rooms[idx]);
		}
		return result;
	}
	
	public static class DeviceInRoomHistoryResult {
		public InstallAppDevice iad;
		public long start;
		public long end;
	}
	private static Map<String, List<DeviceInRoomHistoryResult>> devicesByRoomHistory = new HashMap<>();
	private static long lastDevByRoomHistoryUpdate = -1;
	
	/** Get only devices listed by Location#roomHistory. No trash devices are returned.
	 * 
	 * @param start if null then no filtering of time is performed
	 * @param end
	 * @param room
	 * @param now
	 * @return may be null if start = null if no devices are found for the room
	 */
	public static List<DeviceInRoomHistoryResult> getDeviceswithHistoryInRoom(Long start, long end, Room room,
			DatapointService dpService) {
		String roomLoc = room.getLocation();
		long now = dpService.getFrameworkTime();
		synchronized (devicesByRoomHistory) {
			List<DeviceInRoomHistoryResult> result = devicesByRoomHistory.get(roomLoc);
			if(result == null || (now - lastDevByRoomHistoryUpdate) > TimeProcUtil.MINUTE_MILLIS) {
				Map<String, List<DeviceInRoomHistoryResult>> newMap = new HashMap<>();
				Collection<InstallAppDevice> allIad = dpService.managedDeviceResoures(null);
				for(InstallAppDevice iad: allIad) {
					if(iad.isTrash().getValue())
						continue;
					PhysicalElement device = iad.device();
					StringArrayResource history = device.location().roomHistory();
					if(!history.isActive() || history.size() == 0)
						continue;
					TimeArrayResource historyStart = device.location().roomStart();
					int len = Math.min(history.size(), historyStart.size());
					if(len == 0)
						continue;
					
					String[] rooms = history.getValues();
					long[] startTimes = historyStart.getValues();
					for(int idx=0; idx<len; idx++) {
						String roomLocLoop = rooms[idx];
						List<DeviceInRoomHistoryResult> resultLoop = newMap.get(roomLocLoop);
						if(resultLoop == null) {
							resultLoop = new ArrayList<>();
							newMap.put(roomLocLoop, resultLoop);
						}
						DeviceInRoomHistoryResult newEntry = new DeviceInRoomHistoryResult();
						newEntry.iad = iad;
						newEntry.start = startTimes[idx];
						if(idx < (len-1))
							newEntry.end = startTimes[idx+1];
						else
							newEntry.end = Long.MAX_VALUE;
						resultLoop.add(newEntry);
					}
				}
				devicesByRoomHistory = newMap;
				lastDevByRoomHistoryUpdate = now;
				result = devicesByRoomHistory.get(roomLoc);
			}
			if(result != null && start != null) {
				List<DeviceInRoomHistoryResult> resultFinal = new ArrayList<>();
				for(DeviceInRoomHistoryResult iad: result) {
					if(isDeviceInRoomHistory(iad.iad.device(), start, end, roomLoc))
						resultFinal.add(iad);
				}
				return resultFinal;
			}
			return result;
		}
	}
	
	public static boolean isDeviceInRoomHistory(PhysicalElement device, long start, long end, String roomLocation) {
		StringArrayResource history = device.location().roomHistory();
		if(!history.isActive() || history.size() == 0)
			return false;
		TimeArrayResource historyStart = device.location().roomStart();
		int len = Math.min(history.size(), historyStart.size());
		if(len == 0)
			return false;
		long[] startTimes = historyStart.getValues();
		String[] rooms = history.getValues();
		for(int idx=0; idx<len; idx++) {
			String roomLocLoop = rooms[idx];
			if(roomLocLoop.equals(roomLocation))
				continue;
			long roomStart = startTimes[idx];
			long roomEnd;
			if(idx < (len-1))
				roomEnd = startTimes[idx+1];
			else
				roomEnd = Long.MAX_VALUE;
			if(((roomStart < start && roomEnd > end)
					|| (roomStart >= start && roomStart <= end)
					|| (roomEnd >= start && roomEnd <= end))
					&& rooms[idx] != null && (!rooms[idx].isBlank()))
				return true;
		}
		return false;
	}
}
