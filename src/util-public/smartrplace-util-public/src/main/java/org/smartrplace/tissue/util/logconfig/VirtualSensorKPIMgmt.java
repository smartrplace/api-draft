package org.smartrplace.tissue.util.logconfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.model.sensors.ElectricEnergySensor;
import org.ogema.recordeddata.DataRecorder;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries2;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtil;
import org.slf4j.Logger;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatSchedules;

import de.iwes.util.format.StringFormatHelper;

/** Management of Virtual sensors for a certain application
 * 
 * @author dnestle
 *
 */
public class VirtualSensorKPIMgmt {
	private final TimeseriesSimpleProcUtil util;
	protected final DataRecorder dataRecorder;
	private final Logger logger;
	private final DatapointService dpService;
	/** Source EnergyResource location -> data for accumulation*/
	private Map<String, VirtualSensorKPIDataBase> dpData = new HashMap<>();

	public VirtualSensorKPIMgmt(TimeseriesSimpleProcUtil util, Logger logger, DatapointService dpService) {
		this(util, null, logger, dpService);
	}

	/**
	 * @param util
	 * @param dataRecorder may be null if not VirtualSlotsDB is used in an inherited class
	 * @param logger
	 * @param dpService
	 */
	public VirtualSensorKPIMgmt(TimeseriesSimpleProcUtil util, DataRecorder dataRecorder, Logger logger, DatapointService dpService) {
		this.util = util;
		this.dataRecorder = dataRecorder;
		this.logger = logger;
		this.dpService = dpService;
	}
	
	public static interface VirtualDatapointSetup {
		/** Return {@link SingleValueResource} representing virtual sensor.
		 * then 
		 * @param dpSource
		 * @param mapData
		 * @param intervalToStayBehindNow
		 * @param registerGovernedSchedule
		 * @return  If null no resource is created, just a memory datapoint timeseries. In this case not the resource datapoint
		 * 		is added added to the result, but the pure memory datapoint (both only if result != null)
		 */
		SingleValueResource getAndConfigureValueResource(Datapoint dpSource, VirtualSensorKPIDataBase mapData);
	}
	public VirtualSensorKPIDataBase getDatapointData(Datapoint dpSource, VirtualDatapointSetup setupProvider,
			long intervalToStayBehindNow,
			boolean registerGovernedSchedule, boolean registerRemoteScheduleViaHeartbeat,
			List<Datapoint> result) {
		SingleValueResource source = (SingleValueResource) dpSource.getResource();
		final VirtualSensorKPIDataBase mapData1 = dpData.get(source.getLocation());
		if(mapData1 != null) synchronized (mapData1) {
			if(result != null)
				result.add(mapData1.resourceDp);
			return mapData1;
		}
		final VirtualSensorKPIDataBase mapData = new VirtualSensorKPIDataBase();
		synchronized (mapData) {
		dpData.put(source.getLocation(), mapData);
		SingleValueResource destRes = setupProvider.getAndConfigureValueResource(dpSource, mapData);
		if(registerGovernedSchedule) {
			dpService.virtualScheduleService().addDefaultSchedule(mapData.evalDp, intervalToStayBehindNow);
		} else
			dpService.virtualScheduleService().add(mapData.evalDp, null, intervalToStayBehindNow);
		ReadOnlyTimeSeries accTs = mapData.evalDp.getTimeSeries();
		if(destRes != null) {
			mapData.resourceDp = dpService.getDataPointStandard(destRes);
			mapData.resourceDp.setTimeSeries(accTs);
			if(result != null)
				result.add(mapData.resourceDp);
			logger.info("   Starting Accumlated full for:"+destRes.getLocation()+ " DP size:"+accTs.size());
			long now = dpService.getFrameworkTime();
			SampledValue lastVal = accTs.getPreviousValue(now+1);
logger.info("   Starting Accumlated found previous accFull DP value: "+
((lastVal != null)?StringFormatHelper.getFullTimeDateInLocalTimeZone(lastVal.getTimestamp()):"NONE"));
logger.info("   Starting Accumlated full Recstor size(3):"+accTs.size());
		} else if(result != null)
			result.add(mapData.evalDp);
		
		if(registerRemoteScheduleViaHeartbeat) {
			ViaHeartbeatSchedules schedProv = new ViaHeartbeatSchedules(accTs);
			// Both datapoints can be addressed via heartbeat and will return the same data
			mapData.evalDp.setParameter(Datapoint.HEARTBEAT_STRING_PROVIDER_PARAM, schedProv);
			if(destRes != null)
				mapData.resourceDp.setParameter(Datapoint.HEARTBEAT_STRING_PROVIDER_PARAM, schedProv);
		}

		//TODO: We should add support also for other SingleValueResourceTypes
		if((dpSource.getResource() instanceof FloatResource) && (destRes != null || destRes instanceof FloatResource))
			addSourceResourceListenerFloat(mapData, (FloatResource) destRes, (FloatResource) dpSource.getResource(),
					intervalToStayBehindNow);
		
		return mapData;
		}
		
	}
	
	/** TODO: This method shall be moved to an inherited class in the future
	 * 
	 * @param dpSource
	 * @param energyDailyRealAgg the resource into which the result shall be written.
	 * @param result
	 * @return
	 */
	public VirtualSensorKPIDataBase getDatapointDataEnergyAccumulation(Datapoint dpSource,
			String newSubResName, ElectricityConnection conn,
			Long intervalToStayBehindNow,
			boolean registerGovernedSchedule,
			boolean registerRemoteScheduleViaHeartbeat,
			List<Datapoint> result) {
		VirtualSensorKPIDataBase mapData = getDatapointData(dpSource, new VirtualDatapointSetup() {
			
			@Override
			public SingleValueResource getAndConfigureValueResource(Datapoint dpSource, VirtualSensorKPIDataBase mapData) {
				EnergyResource energyDailyRealAgg = conn.getSubResource(newSubResName, ElectricEnergySensor.class).reading();
				energyDailyRealAgg.getSubResource("unit", StringResource.class).<StringResource>create().setValue("kWh");
				energyDailyRealAgg.getParent().activate(true);
				
				dpSource.info().setAggregationMode(AggregationMode.Consumption2Meter);
				mapData.evalDp = util.processSingle(TimeProcUtil.METER_EVAL, dpSource);

				return energyDailyRealAgg;
			}
		}, intervalToStayBehindNow>=0?intervalToStayBehindNow:15*TimeProcUtil.MINUTE_MILLIS,
				registerGovernedSchedule, registerRemoteScheduleViaHeartbeat, result);
		return mapData;
	}
	
	/**
	 * 
	 * @param mapData
	 * @param energyDailyRealAgg may be null for pure memory timeseries
	 * @param sourceRes may not be null as the update function relies on a {@link ResourceValueListener}
	 * @param intervalToStayBehindNow Sometimes the input data for the current time may not be available right away. In this
	 * 		case we always stop the calculations a bit earlier then the current time, e.g. 15 minutes for a typical metering situation
	 */
	protected void addSourceResourceListenerFloat(VirtualSensorKPIDataBase mapData, FloatResource energyDailyRealAgg,
			FloatResource sourceRes, long intervalToStayBehindNow) {
		mapData.aggListener = new ResourceValueListener<EnergyResource>() {
			//long lastVal = 0;
			@Override
			public void resourceChanged(EnergyResource resource) {
logger.info("   In EnergyServer energyDaily onValueChanged:"+resource.getLocation());
				//we just have to perform a read to trigger an update
				long nowReal = dpService.getFrameworkTime();
				SampledValue lastSv = null;
				long lastVal = 0;
				ReadOnlyTimeSeries accTs = mapData.evalDp.getTimeSeries();
				lastSv = accTs.getPreviousValue(nowReal+1);
				if(lastSv != null)
					lastVal = lastSv.getTimestamp();  
				((ProcessedReadOnlyTimeSeries2)accTs).setUpdateLastTimestampInSourceOnEveryCall(true);
				
				/** We have to go back a little bit with the end to make sure the value is really in. Otherwise it will not be read
				 * again as the logic assumes that the time series does not change after now.
				 */
				long now = nowReal - intervalToStayBehindNow;
				if(lastVal >= now)
					return;
				((ProcessedReadOnlyTimeSeries2)accTs).resetKnownEnd(lastVal, false);
				List<SampledValue> svs = accTs.getValues(lastVal+1, now+1);
logger.info("   In EnergyServer energyDaily onValueChanged: Found new vals:"+svs.size()+" Checked from "+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastVal));
if(!svs.isEmpty())
logger.info("   Last value written at: "+StringFormatHelper.getFullTimeDateInLocalTimeZone(svs.get(svs.size()-1).getTimestamp()));
				if(!svs.isEmpty() && (energyDailyRealAgg != null))
					energyDailyRealAgg.setValue(svs.get(svs.size()-1).getValue().getFloatValue());
if((energyDailyRealAgg != null))
logger.info("OnValueChanged Summary for "+energyDailyRealAgg.getLocation()+":\r\n"+
						(lastSv!=null?"Found existing last SampledValue in SlotsDB at "+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastSv.getTimestamp()):"")+
						",\r\n Calculated values for DP"+mapData.evalDp.getLocation()+" from "+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastVal+1)+" to "+StringFormatHelper.getFullTimeDateInLocalTimeZone(now+1)+
						",\r\n Found "+svs.size()+" new values. Wrote into "+mapData.evalDp.getLocation()); //+
						//".\r\n Set lastVal to "+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastVal));
			}
		};
		sourceRes.addValueListener(mapData.aggListener, true);	
	}
	
	/** Also updates the memory timeseries of datapoint as any read will update anyways
	 * 
	 * @param sched
	 * @param dp
	 * @param rewriteAll if true then the schedule will be entirely rewritten based on the datapoint series, otherwise
	 * 		writing is performed like for slotsDB
	 */
	public static void updateScheduleFromDatapoint(Schedule sched, Datapoint dp,
			long nowReal, long intervalToStayBehindNow, boolean rewriteAll) {
		if(rewriteAll) {
			sched.deleteValues();
			ReadOnlyTimeSeries accTs = dp.getTimeSeries();
			List<SampledValue> allVals = accTs.getValues(0, nowReal-intervalToStayBehindNow);
			sched.addValues(allVals);
			return;
		}
		updateSlotsDbFromDatapoint(sched, dp, nowReal, intervalToStayBehindNow);
	}
	
	/** Also updates the memory timeseries of datapoint as any read will update anyways
	 * recStor must either be of type Schedule or RecordedDataStorage*/
	public static void updateSlotsDbFromDatapoint(ReadOnlyTimeSeries recStor, Datapoint dp, long nowReal, long intervalToStayBehindNow) {
		SampledValue lastSv = null;
		//if(lastVal <= 0) {
		long lastVal = 0;
		lastSv = recStor.getPreviousValue(nowReal+1);
		if(lastSv != null)
			lastVal = lastSv.getTimestamp();  
		//}
		ReadOnlyTimeSeries accTs = dp.getTimeSeries();
		((ProcessedReadOnlyTimeSeries2)accTs).setUpdateLastTimestampInSourceOnEveryCall(true);
		
		/** We have to go back a little bit with the end to make sure the value is really in. Otherwise it will not be read
		 * again as the logic assumes that the time series does not change after now.
		 */
		long now = nowReal - intervalToStayBehindNow;
		if(lastVal >= now)
			return;
		List<SampledValue> svs = accTs.getValues(lastVal+1, now+1);
		SampledValue lastTs = LogConfigSP.storeData(svs, recStor, 20*TimeProcUtil.MINUTE_MILLIS);
		if(lastTs != null) {
			((ProcessedReadOnlyTimeSeries2)accTs).resetKnownEnd(lastTs.getTimestamp(), false);
			svs = accTs.getValues(lastTs.getTimestamp()+1, now+1);
			SampledValue lastTs2 = LogConfigSP.storeData(svs, recStor, 20*TimeProcUtil.MINUTE_MILLIS);
			if(lastTs2 != null) {
				if((now - lastTs2.getTimestamp()) > 2*TimeProcUtil.HOUR_MILLIS) {
					//store anyways to bridge a gap that will not fill anymore
					LogConfigSP.storeData(svs, recStor, Long.MAX_VALUE);
				}
			}
		}
	}
}
