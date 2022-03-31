package org.smartrplace.util.virtualdevice;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.PercentageResource;
import org.ogema.core.recordeddata.RecordedData;
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
import org.ogema.model.devices.sensoractordevices.SensorDevice;
import org.ogema.model.gateway.remotesupervision.DataLogTransferInfo;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.CO2Sensor;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.tools.resource.util.LoggingUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gateway.device.GatewayDevice;
import org.smartrplace.gateway.device.KnownIssueDataGw;
import org.smartrplace.gateway.device.MemoryTimeseriesPST;
import org.smartrplace.tissue.util.logconfig.LogTransferUtil;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
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
	
	public static Label getDutyCycleLabel(HmInterfaceInfo device, InstallAppDevice deviceConfiguration,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id_vh) {
		return getDutyCycleLabel(device, "dutyCycleLb", deviceConfiguration, vh, id_vh, device.dutyCycle().reading());
	}
	public static Label getDutyCycleLabel(HmInterfaceInfo device, String preId, InstallAppDevice deviceConfiguration,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id_vh,
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
		Label dutyCycleLb = new Label(vh.getParent(), preId+id_vh, vh.getReq()) {
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
		final GetPlotButtonResult resultMain = new GetPlotButtonResult();
		
		resultMain.devHand = devHand;
		if(resultMain.devHand != null) {
			if(!addDataPointInfoLabel)
				resultMain.datapoints2 = null;
			else  {
				if(Boolean.getBoolean("org.smartrplace.apps.hw.install.gui.omitdatapoints"))
					resultMain.datapoints2 = Collections.emptyList();
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
						if(res != null && (res instanceof SingleValueResource) && (datalogs != null) &&
								LogTransferUtil.isResourceTransferred((SingleValueResource) res, datalogs)) {
							transferred++;
						}
					}
				}
				String text = ""+resultMain.datapoints2.size()+"/"+logged+"/"+transferred;
				final boolean isTemplate = DeviceTableRaw.isTemplate(object, resultMain.devHand);
				if(isTemplate) {
					text += "/T";
				}
				if(addDataPointInfoLabel)
					resultMain.dataPointInfoLabel = vh.stringLabel(DATAPOINT_INFO_HEADER, id, text, row);
				}
			
			SchedOpenDataProvider provider = new SchedOpenDataProvider() {
				
				@Override
				public IntervalConfiguration getITVConfiguration() {
					return IntervalConfiguration.getDefaultDuration(IntervalConfiguration.ONE_DAY, appMan);
				}
				
				@Override
				public List<TimeSeriesData> getData(OgemaHttpRequest req) {
					List<TimeSeriesData> result = new ArrayList<>();
					OgemaLocale locale = req!=null?req.getLocale():null;
					if(resultMain.datapoints2 == null)
						resultMain.datapoints2 = resultMain.devHand.getDatapoints(object, dpService);
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
			resultMain.plotButton = ScheduleViwerOpenUtil.getScheduleViewerOpenButton(vh.getParent(), "plotButton"+id,
					"Plot", provider, schedViewProv, req);
		}
		return resultMain;
	}
}
