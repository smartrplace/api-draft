package org.smartrplace.tissue.util.logconfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DeviceHandlerBase;
import org.ogema.model.connections.ElectricityConnection;
import org.ogema.recordeddata.DataRecorder;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries2;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtilBase;
import org.slf4j.Logger;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatSchedules;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

/** see {@link VirtualSensorKPIMgmt}
 * 
 * @author dnestle
 *
 * D : ResourceType of device or sub-device resource to which the virtual sensor is attached (and - if applicable- where
 * relevant resources that might be necessary as parameters beside the dpInput datapoints may be found)
 * S : ResourceType of virtual sensor resource
 */
public abstract class VirtualSensorKPIMgmtBase<D extends Resource, S extends SingleValueResource> {
	protected final TimeseriesSimpleProcUtilBase tsProcUtil;

	//	protected final DataRecorder dataRecorder2;
	private final Logger logger;
	private final DatapointService dpService;
	/** Source EnergyResource location -> data for accumulation*/
	private final Map<String, VirtualSensorKPIDataBase> dpData = new HashMap<>();
	
	/** Set this directly to true if you want ot use dpData to get back data for a single virtual datapoint per device*/
	public boolean singleVirtualDatapointPerDevice = false;
	
	/** Provide datapoint calculation
	 * 
	 * @param dpSource
	 * @param energyDailyRealAgg the resource into which the result shall be written.
	 * @param result
	 * @return
	 * 
	 * !! Use {@link #addVirtualDatapoint(List, String, Resource, long, boolean, boolean, List)} directly
	 */
	@Deprecated
	public VirtualSensorKPIDataBase getDatapointDataAccumulationSingle(Datapoint dpSource,
			String newSubResName, D device,
			Long intervalToStayBehindNow,
			boolean registerGovernedSchedule,
			boolean registerRemoteScheduleViaHeartbeat,
			List<Datapoint> result) {
		throw new IllegalStateException("Usage of method is optional, but must be implemented if used!");
	};
	/** Overwrite if multi-input is required: Either this or Single version may be overwritten. Usage is optional anyways.
	 * 
	 * !! Use {@link #addVirtualDatapoint(List, String, Resource, long, boolean, boolean, List)} directly
	 */
	@Deprecated
	public VirtualSensorKPIDataBase getDatapointDataAccumulation(List<Datapoint> dpSource,
			String newSubResName, D device,
			Long intervalToStayBehindNow,
			boolean registerGovernedSchedule,
			boolean registerRemoteScheduleViaHeartbeat,
			List<Datapoint> result) {
		return getDatapointDataAccumulationSingle(dpSource.get(0), newSubResName, device, intervalToStayBehindNow,
				registerGovernedSchedule, registerRemoteScheduleViaHeartbeat, result);
	}

	public VirtualSensorKPIMgmtBase(TimeseriesSimpleProcUtilBase util, Logger logger, DatapointService dpService) {
		this(util, null, logger, dpService);
	}
	/*public VirtualSensorKPIMgmt(TimeseriesSimpleProcUtil3 util, Logger logger, DatapointService dpService) {
		this(util, null, logger, dpService);
	}*/

	/**
	 * @param util
	 * @param dataRecorder may be null if not VirtualSlotsDB is used in an inherited class
	 * @param logger
	 * @param dpService
	 */
	public VirtualSensorKPIMgmtBase(TimeseriesSimpleProcUtilBase util, DataRecorder dataRecorder, Logger logger, DatapointService dpService) {
		this.tsProcUtil = util;
//		this.dataRecorder = dataRecorder;
		this.logger = logger;
		this.dpService = dpService;
	}
	
	/*public static interface VirtualDatapointSetup {
		/** Return {@link SingleValueResource} representing virtual sensor.
		 * then 
		 * @param dpSource
		 * @param mapData
		 * @return  If null no resource is created, just a memory datapoint timeseries. In this case not the resource datapoint
		 * 		is added added to the result, but the pure memory datapoint (both only if result != null)
		 */
		//SingleValueResource getAndConfigureValueResource(List<Datapoint> dpSource, VirtualSensorKPIDataBase mapData);		
	//}
	//public static interface VirtualDatapointSetupSingleInput extends VirtualDatapointSetup {
		/** Return {@link SingleValueResource} representing virtual sensor.
		 * then 
		 * @param dpSource
		 * @param mapData
		 * @return  If null no resource is created, just a memory datapoint timeseries. In this case not the resource datapoint
		 * 		is added added to the result, but the pure memory datapoint (both only if result != null)
		 */
	/*	SingleValueResource getAndConfigureValueResource(Datapoint dpSource, VirtualSensorKPIDataBase mapData);
		
		@Override
		default SingleValueResource getAndConfigureValueResource(List<Datapoint> dpSource,
				VirtualSensorKPIDataBase mapData) {
			return getAndConfigureValueResource(dpSource==null||dpSource.isEmpty()?null:dpSource.get(0), mapData);
		}
	}*/
	protected abstract S getAndConfigureValueResourceSingle(Datapoint dpSource, VirtualSensorKPIDataBase mapData,
			 String newSubResName, D device);
	
	protected S getAndConfigureValueResource(List<Datapoint> dpSource,
			VirtualSensorKPIDataBase mapData, String newSubResName, D device) {
		return getAndConfigureValueResourceSingle(dpSource==null||dpSource.isEmpty()?null:dpSource.get(0), mapData,
				newSubResName, device);
	}

	/**
 	* !! Use #{@link #addVirtualDatapointSingle(Datapoint, String, Resource, long, boolean, boolean, List)} instead
 	* */
	@Deprecated
	public VirtualSensorKPIDataBase getDatapointData(Datapoint dpSource,
			String newSubResName, D device,
			//VirtualDatapointSetup setupProvider,
			long intervalToStayBehindNow,
			boolean registerGovernedSchedule, boolean registerRemoteScheduleViaHeartbeat,
			List<Datapoint> result) {
		List<Datapoint> dpSources = Arrays.asList(new Datapoint[] {dpSource});
		return addVirtualDatapoint(dpSources, newSubResName, device, intervalToStayBehindNow, registerGovernedSchedule,
				registerRemoteScheduleViaHeartbeat, result);
	}

	public VirtualSensorKPIDataBase addVirtualDatapointSingle(Datapoint dpSource,
			//VirtualDatapointSetup setupProvider,
			String newSubResName, D destinationResParent,
			long intervalToStayBehindNow,
			boolean registerGovernedSchedule, boolean registerRemoteScheduleViaHeartbeat,
			List<Datapoint> result) {
		return addVirtualDatapointSingle(dpSource, newSubResName, destinationResParent, intervalToStayBehindNow, registerGovernedSchedule,
				registerRemoteScheduleViaHeartbeat, result, null);
		
	}
	public VirtualSensorKPIDataBase addVirtualDatapointSingle(Datapoint dpSource,
			//VirtualDatapointSetup setupProvider,
			String newSubResName, D destinationResParent,
			long intervalToStayBehindNow,
			boolean registerGovernedSchedule, boolean registerRemoteScheduleViaHeartbeat,
			List<Datapoint> result, Integer absoluteTiming) {
		List<Datapoint> dpSources = Arrays.asList(new Datapoint[] {dpSource});
		return addVirtualDatapoint(dpSources, newSubResName, destinationResParent, intervalToStayBehindNow, registerGovernedSchedule,
				registerRemoteScheduleViaHeartbeat, result, absoluteTiming);		
	}
	public VirtualSensorKPIDataBase addVirtualDatapoint(List<Datapoint> dpSource,
			//VirtualDatapointSetup setupProvider,
			String newSubResName, D device,
			long intervalToStayBehindNow,
			boolean registerGovernedSchedule, boolean registerRemoteScheduleViaHeartbeat,
			List<Datapoint> result) {
		return addVirtualDatapoint(dpSource, newSubResName, device, intervalToStayBehindNow, registerGovernedSchedule,
				registerRemoteScheduleViaHeartbeat, result, null);
	}
	/** Note: For Standard evaluations use StandardEvalAccess#addVirtualDatapoint
	 * 
	 * @param dpSource the list may not be empty. The first element is used as pivot relevant for identification
	 * @param setupProvider
	 * @param intervalToStayBehindNow relevant for the VirtualScheduleRegistration regarding updates due to change of the referene time
	 * @param registerGovernedSchedule if true a governed schedule is registered, otherwise the datapoint is just registered
	 * 		for updates due to a change of the reference time
	 * @param registerRemoteScheduleViaHeartbeat
	 * @param result
	 * @return
	 */
	public VirtualSensorKPIDataBase addVirtualDatapoint(List<Datapoint> dpSource,
			//VirtualDatapointSetup setupProvider,
			String newSubResName, D destinationResParent,
			long intervalToStayBehindNow,
			boolean registerGovernedSchedule, boolean registerRemoteScheduleViaHeartbeat,
			List<Datapoint> result,
			Integer absoluteTiming) {

		String sourceLocation;
		//TODO: We should be able to just use dpSource.getLocation, but to change as little as possible we do it like this
		/*SingleValueResource sourceX = (SingleValueResource) dpSource.get(0).getResource();
		if(sourceX != null)
			sourceLocation = sourceX.getLocation();
		else*/
		sourceLocation = dpSource.get(0).getLocation();
		if(singleVirtualDatapointPerDevice) {
			final VirtualSensorKPIDataBase mapData1 = dpData.get(sourceLocation);
			if(mapData1 != null) synchronized (mapData1) {
				if(result != null)
					result.add(mapData1.resourceDp);
				return mapData1;
			}
		}
		final VirtualSensorKPIDataBase mapData = new VirtualSensorKPIDataBase();
		synchronized (mapData) {
		dpData.put(sourceLocation, mapData);
		S destRes = getAndConfigureValueResource(dpSource, mapData, newSubResName, destinationResParent);
		if(mapData.evalDp == null)
			return null;
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
			logger.trace("   Starting Accumlated full for:"+destRes.getLocation()+ " DP size:"+accTs.size());
			long now = dpService.getFrameworkTime();
			SampledValue lastVal = accTs.getPreviousValue(now+1);
logger.trace("   Starting Accumlated found previous accFull DP value: "+
((lastVal != null)?StringFormatHelper.getFullTimeDateInLocalTimeZone(lastVal.getTimestamp()):"NONE"));
logger.trace("   Starting Accumlated full Recstor size(3):"+accTs.size());
		} else if(result != null)
			result.add(mapData.evalDp);
		
		if(registerRemoteScheduleViaHeartbeat) {
			ViaHeartbeatSchedules schedProv = ViaHeartbeatSchedules.registerDatapointForHeartbeatDp2Schedule(mapData.evalDp, null, mapData.absoluteTiming);
			//ViaHeartbeatSchedules schedProv = new ViaHeartbeatSchedules(accTs);
			// Both datapoints can be addressed via heartbeat and will return the same data
			//mapData.evalDp.setParameter(Datapoint.HEARTBEAT_STRING_PROVIDER_PARAM, schedProv);
			if(destRes != null)
				mapData.resourceDp.setParameter(Datapoint.HEARTBEAT_STRING_PROVIDER_PARAM, schedProv);
		}

if(Boolean.getBoolean("suppress.addSourceResourceListenerFloat"))
return mapData;
		
		//TODO: We should add support also for other SingleValueResourceTypes
		boolean useAlignedListenerNewVersion = (mapData.evalDp.getTimeSeries() instanceof ProcessedReadOnlyTimeSeries3)
				&& (absoluteTiming != null);
		for(Datapoint dp: dpSource) {
			if(useAlignedListenerNewVersion) {
				if((dp.getResource() instanceof FloatResource) && (destRes != null || destRes instanceof FloatResource)) {
					addLastCompleteValueFromScheduleToResource(
							(FloatResource) dp.getResource(), (FloatResource) destRes, absoluteTiming, mapData);
				}
			}
			else if((dp.getResource() instanceof FloatResource) && (destRes != null || destRes instanceof FloatResource))
				addSourceResourceListenerFloat(mapData, (FloatResource) destRes, (FloatResource) dp.getResource(),
						intervalToStayBehindNow);
		}
		return mapData;
		}
		
	}
	
	/**
	 * 
	 * @param mapData
	 * @param destRes may be null for pure memory timeseries
	 * @param sourceRes may not be null as the update function relies on a {@link ResourceValueListener}
	 * @param intervalToStayBehindNow Sometimes the input data for the current time may not be available right away. In this
	 * 		case we always stop the calculations a bit earlier then the current time, e.g. 15 minutes for a typical metering situation
	 */
	protected void addSourceResourceListenerFloat(VirtualSensorKPIDataBase mapData, FloatResource destRes,
			FloatResource sourceRes, long intervalToStayBehindNow) {
		mapData.aggListener = new ResourceValueListener<FloatResource>() {
			//long lastVal = 0;
			@Override
			public void resourceChanged(FloatResource resource) {
logger.info("   In EnergyServer energyDaily onValueChanged:"+resource.getLocation());
				//we just have to perform a read to trigger an update
				long nowReal = dpService.getFrameworkTime();
				SampledValue lastSv = null;
				long lastVal = 0;
				ReadOnlyTimeSeries accTs = mapData.evalDp.getTimeSeries();
				lastSv = accTs.getPreviousValue(nowReal+1);
				if(lastSv != null)
					lastVal = lastSv.getTimestamp();
				if(accTs instanceof ProcessedReadOnlyTimeSeries2)
					((ProcessedReadOnlyTimeSeries2)accTs).setUpdateLastTimestampInSourceOnEveryCall(true);
				
				/** We have to go back a little bit with the end to make sure the value is really in. Otherwise it will not be read
				 * again as the logic assumes that the time series does not change after now.
				 */
				long now = nowReal - intervalToStayBehindNow;
				if(lastVal >= now)
					return;
				if(accTs instanceof ProcessedReadOnlyTimeSeries2)
					((ProcessedReadOnlyTimeSeries2)accTs).resetKnownEnd(lastVal, false);
				List<SampledValue> svs = accTs.getValues(lastVal+1, now+1);
/*logger.info("   In EnergyServer energyDaily onValueChanged: Found new vals:"+svs.size()+" Checked from "+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastVal));
if(!svs.isEmpty())
logger.info("   Last value written at: "+StringFormatHelper.getFullTimeDateInLocalTimeZone(svs.get(svs.size()-1).getTimestamp()));
				if(!svs.isEmpty() && (destRes != null))
					destRes.setValue(svs.get(svs.size()-1).getValue().getFloatValue());
if((destRes != null))
logger.info("OnValueChanged Summary for "+destRes.getLocation()+":\r\n"+
						(lastSv!=null?"Found existing last SampledValue in SlotsDB at "+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastSv.getTimestamp()):"")+
						",\r\n Calculated values for DP"+mapData.evalDp.getLocation()+" from "+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastVal+1)+" to "+StringFormatHelper.getFullTimeDateInLocalTimeZone(now+1)+
						",\r\n Found "+svs.size()+" new values. Wrote into "+mapData.evalDp.getLocation()); //+
						//".\r\n Set lastVal to "+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastVal));*/
			}
		};
		sourceRes.addValueListener(mapData.aggListener, true);	
	}
	
	protected void addLastCompleteValueFromScheduleToResource(FloatResource timerByValueChangedSource,
			FloatResource destRes, int absoluteTiming,
			VirtualSensorKPIDataBase mapData) {
		if(destRes == null)
			return;
		mapData.aggListener = new ResourceValueListener<FloatResource>() {
			//long lastVal = 0;
			@Override
			public void resourceChanged(FloatResource resource) {
logger.info("   In EnergyServer energyDaily onValueChanged:"+resource.getLocation());
				
				long nowReal = dpService.getFrameworkTime();
				long nowItvStart = AbsoluteTimeHelper.getIntervalStart(nowReal, absoluteTiming);
				
				long lastWritten = destRes.getLastUpdateTime();

				//If we have already written once during the current interval then we do not have
				//to write again
				if(lastWritten > nowItvStart)
					return;

				SampledValue lastSv = null;
				ReadOnlyTimeSeries accTs = mapData.evalDp.getTimeSeries();
				lastSv = accTs.getPreviousValue(nowItvStart);
				if(lastSv == null)
					return;

				float value = lastSv.getValue().getFloatValue();
				if(Float.isNaN(value))
					return;
				ValueResourceHelper.setCreate(destRes, value);
			}
		};
		timerByValueChangedSource.addValueListener(mapData.aggListener, true);	
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
		if(accTs instanceof ProcessedReadOnlyTimeSeries2)
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
			if(accTs instanceof ProcessedReadOnlyTimeSeries2)
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
	
	public static List<Datapoint> registerEnergySumDatapointOverSubPhases(ElectricityConnection conn, AggregationMode inputAggMode,
			TimeseriesSimpleProcUtilBase util, DatapointService dpService, String startLevel) {
		return registerEnergySumDatapointOverSubPhases(conn, inputAggMode, util, dpService, startLevel, false, null);
	}
	public static List<Datapoint> registerEnergySumDatapointOverSubPhasesFromDay(ElectricityConnection conn, AggregationMode inputAggMode,
			TimeseriesSimpleProcUtilBase util, DatapointService dpService, Datapoint hourlySum) {
		return registerEnergySumDatapointOverSubPhases(conn, inputAggMode, util, dpService, "day", false, hourlySum);
	}
	public static List<Datapoint> registerEnergySumDatapointOverSubPhases(ElectricityConnection conn, AggregationMode inputAggMode,
			TimeseriesSimpleProcUtilBase util, DatapointService dpService, String startLevel, boolean registerForTransferViaHeartbeatAsMainMeter) {
		return registerEnergySumDatapointOverSubPhases(conn, inputAggMode, util, dpService, startLevel,
				registerForTransferViaHeartbeatAsMainMeter, null);
	}
	public static List<Datapoint> registerEnergySumDatapointOverSubPhases(ElectricityConnection conn, AggregationMode inputAggMode,
			TimeseriesSimpleProcUtilBase util, DatapointService dpService, String startLevel, boolean registerForTransferViaHeartbeatAsMainMeter,
			Datapoint hourlySum) {
		if(conn == null || (!conn.exists()))
			return Collections.emptyList();
		List<Datapoint> energyDailys = new ArrayList<>();
		for(ElectricityConnection phaseConn: conn.subPhaseConnections().getAllElements()) {
			EnergyResource inputEnergy = phaseConn.energySensor().reading();
			DeviceHandlerBase.addDatapoint(inputEnergy, energyDailys, dpService);
		}
		if(hourlySum != null)
			return registerEnergySumDatapointFromDay(energyDailys, inputAggMode, util, startLevel, registerForTransferViaHeartbeatAsMainMeter,
					registerForTransferViaHeartbeatAsMainMeter?dpService:null,
					hourlySum, null);
		return registerEnergySumDatapoint(energyDailys, inputAggMode, util, startLevel, registerForTransferViaHeartbeatAsMainMeter,
				registerForTransferViaHeartbeatAsMainMeter?dpService:null);
	}
	/** The method creates datapoints representing the hourly, daily, monthly and yearly sums of the input timeseries,
	 * which are aggregated for this purpose. The datapoints created are returned. By registerForTransferViaHeartbeatAsMainMeter
	 * it is also possible to trigger registration of main meter data transmission to a superior.
	 * 
	 * @param inputEnergy
	 * @param inputAggMode
	 * @param util
	 * @param startLevel
	 * @param registerForTransferViaHeartbeatAsMainMeter if true the results are registered for sending via heartbeat. This
	 * 		assumes that the inputEnergy time series represent the data from the main meter of the gateway. This
	 *      is also implies that main meter aliases are registered.
	 * @param dpService only relevant if registerForTransfer is true, otherwise may be null
	 * @return all datapoints created for sum e.g hourly, daily, monthly, yearly sum
	 */
	public static List<Datapoint> registerEnergySumDatapoint(List<Datapoint> inputEnergy, AggregationMode inputAggMode,
			TimeseriesSimpleProcUtilBase util, String startLevel, boolean registerForTransferViaHeartbeatAsMainMeter,
			DatapointService dpService) {
		if(inputAggMode != null) for(Datapoint dp: inputEnergy) {
			if(dp != null)
				dp.info().setAggregationMode(inputAggMode);
		}
		List<Datapoint> result = new ArrayList<>();
		//boolean started = false;
		Datapoint hourlySum = null;
		if(startLevel.toLowerCase().contains("hour") || startLevel.toLowerCase().contains("15min")) {
			//started = true;
			hourlySum = util.processMultiToSingle(
					startLevel.toLowerCase().contains("hour")?TimeProcUtil.SUM_PER_HOUR_EVAL:TimeProcUtil.SUM_PER_15M_EVAL,
					inputEnergy);
			if(hourlySum == null) {
				System.out.println("    !!!! WARNING: No hourly sum for inputSize:"+inputEnergy.size()+ "First:"+
						(inputEnergy.isEmpty()?"--":inputEnergy.get(0).getLocation()));
				return Collections.emptyList();
			}
			result.add(hourlySum);
			hourlySum.setLabelDefault(startLevel.toLowerCase().contains("hour")?"kWhHourly":"kWh15min");
			if(registerForTransferViaHeartbeatAsMainMeter)
				hourlySum.addAlias(Datapoint.ALIAS_MAINMETER_HOURLYCONSUMPTION);		
		}
		
		return registerEnergySumDatapointFromDay(inputEnergy, inputAggMode, util, startLevel, registerForTransferViaHeartbeatAsMainMeter, dpService,
				hourlySum, result);
	}
	
	
	public static List<Datapoint> registerEnergySumDatapointFromDay(List<Datapoint> inputEnergy, AggregationMode inputAggMode,
			TimeseriesSimpleProcUtilBase util, String startLevel, boolean registerForTransferViaHeartbeatAsMainMeter,
			DatapointService dpService,
			Datapoint hourlySum,
			List<Datapoint> result) {
		Datapoint dailySum = null;
		Datapoint monthlySum = null;
		Datapoint yearlySum = null;

		if(result == null)
			result = new ArrayList<>();
		
		boolean started = hourlySum != null;
		
		if(startLevel.toLowerCase().contains("day") || started) {
			if(!started)
				dailySum = util.processMultiToSingle(TimeProcUtil.SUM_PER_DAY_EVAL, inputEnergy);
			else
				dailySum = util.processSingle(TimeProcUtil.PER_DAY_EVAL, hourlySum);
			started = true;
			//dailySum = util.processMultiToSingle(TimeProcUtil.SUM_PER_DAY_EVAL, inputEnergy);
			if(dailySum == null) {
				System.out.println("    !!!! WARNING: No daily sum for inputSize:"+inputEnergy.size()+ "First:"+
						(inputEnergy.isEmpty()?"--":inputEnergy.get(0).getLocation()));
				return Collections.emptyList();
			}
			result.add(dailySum);
			dailySum.setLabelDefault("kWhDaily");
			if(registerForTransferViaHeartbeatAsMainMeter)
				dailySum.addAlias(Datapoint.ALIAS_MAINMETER_DAILYCONSUMPTION);
			
		}
		if(startLevel.toLowerCase().contains("month") || started) {
			if(!started)
				monthlySum = util.processMultiToSingle(TimeProcUtil.SUM_PER_MONTH_EVAL, inputEnergy);
			else
				monthlySum = util.processSingle(TimeProcUtil.PER_MONTH_EVAL, dailySum);
			started = true;
			result.add(monthlySum);
			monthlySum.setLabelDefault("kWhMonthly");
			if(registerForTransferViaHeartbeatAsMainMeter)
				monthlySum.addAlias(Datapoint.ALIAS_MAINMETER_MONTHLYCONSUMPTION);
		}
		if(started) { //if(startLevel.toLowerCase().contains("year") || started) {
			started = true;
			yearlySum = util.processSingle(TimeProcUtil.PER_YEAR_EVAL, monthlySum);
			result.add(yearlySum);
			yearlySum.setLabelDefault("kWhYearly");
			if(registerForTransferViaHeartbeatAsMainMeter)
				yearlySum.addAlias(Datapoint.ALIAS_MAINMETER_YEARLYCONSUMPTION);
		}
		if(!registerForTransferViaHeartbeatAsMainMeter)
			return result;
		
		DatapointGroup dpgKpi = dpService.getGroup(DatapointGroup.GATEWAY_KPIS);
		dpgKpi.addDatapoint(dailySum);
		dpgKpi.addDatapoint(monthlySum);
		dpgKpi.addDatapoint(yearlySum);
		dpgKpi.setType("GATEWAY_KPIs");
		ViaHeartbeatSchedules.registerDatapointForHeartbeatDp2Schedule(dailySum, AbsoluteTiming.DAY);
		ViaHeartbeatSchedules.registerDatapointForHeartbeatDp2Schedule(monthlySum, AbsoluteTiming.MONTH);
		ViaHeartbeatSchedules.registerDatapointForHeartbeatDp2Schedule(yearlySum, AbsoluteTiming.YEAR);

		return result;
	}
	
	public static void waitForCollectingGatewayServerInit(ResourceAccess resAcc) {
		if(!Boolean.getBoolean("org.smartrplace.app.srcmon.iscollectinggateway"))
			return;
		Resource mirrorList = resAcc.getResource("serverMirror");
		if(mirrorList == null)
			return;
		IntegerResource initStatus = mirrorList.getSubResource("initStatus", IntegerResource.class);
		int count = 0;
		while(initStatus.isActive() && (initStatus.getValue() < 2)) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			if(count > 400) {
				System.out.println("WARNING: in waitForCollectingGatewayServerInit blocked for more than 40 seconds!");
				count = 0;
			}
			count++;
		}		
	}
}
