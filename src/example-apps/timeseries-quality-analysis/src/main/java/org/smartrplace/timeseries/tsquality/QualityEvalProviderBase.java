/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.timeseries.tsquality;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.tools.resource.util.TimeUtils;
import org.ogema.tools.timeseries.iterator.api.SampledValueDataPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.tissue.util.format.StringFormatHelperSP;

import de.iwes.timeseries.eval.api.EvaluationInput;
import de.iwes.timeseries.eval.api.EvaluationInstance.EvaluationListener;
import de.iwes.timeseries.eval.api.ResultType;
import de.iwes.timeseries.eval.api.SingleEvaluationResult;
import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.configuration.ConfigurationInstance;
import de.iwes.timeseries.eval.base.provider.utils.SingleValueResultImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeParam;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoEvaluationCore;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoResultType;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoSingleEvalProviderPreEval;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoSingleEvaluation;
import de.iwes.util.format.StringFormatHelper;

/**
 * Evaluate basic time series qualities per gateway including gap evaluation
 */
public class QualityEvalProviderBase extends GenericGaRoSingleEvalProviderPreEval {
	public static final long MINUTE_MILLIS = 60000;
	public static final long HOUR_MILLIS = 60 * MINUTE_MILLIS;
	public static final long DAY_MILLIS = 24 * HOUR_MILLIS;
	public static final long MAX_NONGAP_TIME_STD = 30* MINUTE_MILLIS;
	public static final float MAX_GAP_FOR_GOOD_SERIES_SHARE = 0.25f;
	public static final float MAX_GAP_FOR_GOLD_SERIES_SHARE = 0.1f;
	public static final long MAX_OVERALL_NONGAP_TIME_STD = 45* MINUTE_MILLIS;
		
	protected List<String> getSensorsToFilerOut(String currentGwId2) {
		String prop = System.getProperty("org.ogema.evaluationofflinecontrol.scheduleviewer.expert.sensorsToFilterOut."+currentGwId);
		if(prop != null) {
			List<String> sensorsToFilterOut = Arrays.asList(prop.split(","));
			return sensorsToFilterOut;
		}
		return null;
	}

	protected static final Logger logger = LoggerFactory.getLogger(QualityEvalProviderBase.class);
    
    /*public QualityEvalProviderBase() {
       super(ID, LABEL, DESCRIPTION);
    }*/

    private static String ID;
    public QualityEvalProviderBase(String id2, String label2, String description2) {
		super(id2, label2, description2);
		ID = id2;
	}

	public static final GaRoDataTypeParam motionType = new GaRoDataTypeParam(GaRoDataType.MotionDetection, false);
    public static final GaRoDataTypeParam humidityType = new GaRoDataTypeParam(GaRoDataType.HumidityMeasurement, false);
    public static final GaRoDataTypeParam tempMesRoomType = new GaRoDataTypeParam(GaRoDataType.TemperatureMeasurementRoomSensor, false);
    public static final GaRoDataTypeParam tempMesThermostatType = new GaRoDataTypeParam(GaRoDataType.TemperatureMeasurementThermostat, false);
    public static final GaRoDataTypeParam tempSetpointType = new GaRoDataTypeParam(GaRoDataType.TemperatureSetpoint, false);
    public static final GaRoDataTypeParam tempSetFBType = new GaRoDataTypeParam(GaRoDataType.TemperatureSetpointFeedback, false);
    public static final GaRoDataTypeParam tempSetSetType = new GaRoDataTypeParam(GaRoDataType.TemperatureSetpointSet, false);
    public static final GaRoDataTypeParam valveType = new GaRoDataTypeParam(GaRoDataType.ValvePosition, false);
    public static final GaRoDataTypeParam powerType = new GaRoDataTypeParam(GaRoDataType.PowerMeter, false);
    public static final GaRoDataTypeParam winType = new GaRoDataTypeParam(GaRoDataType.WindowOpen, false);
    public static final GaRoDataTypeParam chargeType = new GaRoDataTypeParam(GaRoDataType.ChargeSensor, false);
    public static final GaRoDataTypeParam powerSubPhaseType = new GaRoDataTypeParam(GaRoDataType.PowerMeterSubphase, false);
    public static final GaRoDataTypeParam powerOutletType = new GaRoDataTypeParam(GaRoDataType.PowerMeterOutlet, false);
    public static final GaRoDataTypeParam heatPowerType = new GaRoDataTypeParam(GaRoDataType.Heatpower, false);
    public static final GaRoDataTypeParam heatEnergyType = new GaRoDataTypeParam(GaRoDataType.HeatEnergyIntegral, false);
    public static final GaRoDataTypeParam heatFlowType = new GaRoDataTypeParam(GaRoDataType.HeatFlow, false);
    public static final GaRoDataTypeParam heatVolumeType = new GaRoDataTypeParam(GaRoDataType.HeatVolumeIntegral, false);
    public static final GaRoDataTypeParam heatSupplyTempType = new GaRoDataTypeParam(GaRoDataType.HeatSupplyTemperatur, false);
    public static final GaRoDataTypeParam heatReturnTempType = new GaRoDataTypeParam(GaRoDataType.HeatReturnTemperatur, false);
    public static final GaRoDataTypeParam stateFBType = new GaRoDataTypeParam(GaRoDataType.SwitchStateFeedback, false);
    public static final GaRoDataTypeParam energyType = new GaRoDataTypeParam(GaRoDataType.PowerMeterEnergy, false);
    public static final GaRoDataTypeParam energySubPhaseType = new GaRoDataTypeParam(GaRoDataType.PowerMeterEnergySubphase, false);
    public static final GaRoDataTypeParam phValueType = new GaRoDataTypeParam(GaRoDataType.WaterPHValue, false);
    public static final GaRoDataTypeParam conductivityValueType = new GaRoDataTypeParam(GaRoDataType.WaterConductivityValue, false);
    public static final GaRoDataTypeParam redoxValueType = new GaRoDataTypeParam(GaRoDataType.WaterRedoxValue, false);
    public static final GaRoDataTypeParam oxygenValueType = new GaRoDataTypeParam(GaRoDataType.WaterOxygenConcentrationValue, false);
    public static final GaRoDataTypeParam waterTempValueType = new GaRoDataTypeParam(GaRoDataType.WaterTemperatureValue, false);
    public static final GaRoDataTypeParam co2concentrationType = new GaRoDataTypeParam(GaRoDataType.CO2Concentration, false);
    public static final GaRoDataTypeParam internetType = new GaRoDataTypeParam(GaRoDataType.InternetConnection, false);
    public static final GaRoDataTypeParam rssiDeviceType = new GaRoDataTypeParam(GaRoDataType.RSSIDevice, false);
    public static final GaRoDataTypeParam rssiPeerType = new GaRoDataTypeParam(GaRoDataType.RSSIPeer, false);
    public static final GaRoDataTypeParam waterVolumeType = new GaRoDataTypeParam(GaRoDataType.FreshWaterVolume, false);
    public static final GaRoDataTypeParam waterFlowType = new GaRoDataTypeParam(GaRoDataType.FreshWaterFlow, false);
    
	@Override
	/** Provide your data types here*/
	public GaRoDataType[] getGaRoInputTypes() {
		return new GaRoDataType[] {
	        	motionType,
	        	humidityType,
	        	tempMesRoomType,
	        	tempMesThermostatType,
	        	tempSetpointType, //does not exist in GaRoEvalHelper !
	        	tempSetFBType,
	        	tempSetSetType, //setpoint sent to thermostat
	        	valveType,
	        	winType,
	        	powerType,
	        	chargeType,
	        	powerSubPhaseType,
	        	powerOutletType,
	        	heatPowerType,
	        	heatEnergyType,
	        	heatFlowType,
	        	heatVolumeType,
	        	heatSupplyTempType,
	        	heatReturnTempType,
	        	stateFBType,
	        	energyType,
	        	energySubPhaseType,
	        	phValueType,
	        	conductivityValueType,
	        	redoxValueType,
	        	oxygenValueType,
	        	waterTempValueType,
	        	co2concentrationType,
	        	internetType,
	        	rssiDeviceType,
	        	rssiPeerType,
	        	waterVolumeType,
	        	waterFlowType
		};
	}
	
	@Override
	public int[] getRoomTypes() {
		return new int[] {-1};
	}
	
	public static final long[] MAX_GAPTIMES_INTERNAL = new long[] {24*HOUR_MILLIS,
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
			24*HOUR_MILLIS,
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //Valve
			3*HOUR_MILLIS, //Window
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //Power
			6*HOUR_MILLIS,
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //power subphase
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //power outlet
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //heat power
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //heat energy
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //heat flow
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //heat volume
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //heat supply temperature
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //heat return temperature
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //state feedback
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //energy
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //energy subphase
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //waterTempValueType
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //co2concentrationtype
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //internettype
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, 
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, //rssiPeerType
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, 
			GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL //waterFlowType 
			}; //Charge
	
	@Override
	protected long[] getMaximumGapTimes() {
		return new long[] {2*HOUR_MILLIS, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL,
				GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL, GenericGaRoSingleEvaluation.MAX_DATA_INTERVAL
				};
	}
	/** It is recommended to define the indices of your input here.*/
	public static final int MOTION_IDX = 0;
	public static final int HUMIDITY_IDX = 1;
	public static final int TEMPSENS_IDX = 2;
	public static final int TEMPSENS_THERM_IDX = 3;
	public static final int SETP_IDX_NONUSED = 4;
	public static final int SETP_FB_IDX = 5;
	public static final int SETP_IDX = 6;
	public static final int VALVE_IDX = 7;
	public static final int WINDOW_IDX = 8;
	public static final int POWER_IDX = 9;
	public static final int CHARGE_IDX = 10;
	public static final int POWERSUB_IDX = 11;
	public static final int POWEROUTLET_IDX = 12;
	public static final int HEATPOWER_IDX = 13;
	public static final int HEATENERGY_IDX = 14;
	public static final int HEATFLOW_IDX = 15;
	public static final int HEATVOLUME_IDX = 16;
	public static final int HEATSUPPLY_IDX = 17;
	public static final int HEATRETURN_IDX = 18;
	public static final int STATEFB_IDX = 19;
	public static final int ENERGY_IDX = 20;
	public static final int ENERGYSUB_IDX = 21;
	public static final int PH_IDX = 22;
	public static final int CONDUCTIVITY_IDX = 23;
	public static final int REDOX_IDX = 24;
	public static final int OXYGEN_IDX = 25;
	public static final int WATERTEMP_IDX = 26;
	public static final int CO2CONC_IDX = 27;
	public static final int INTERNET_IDX = 28;
	public static final int RSSIDEV_IDX = 29;
	public static final int RSSIPEER_IDX = 30;
	public static final int WATERVOL_IDX = 31;
	public static final int WATERFLOW_IDX = 32;
    public static final int TYPE_NUM = 33;
    
    protected GaRoDataTypeParam getParamType(int idxOfReqInput) {
    	switch(idxOfReqInput) {
    	case MOTION_IDX: return motionType;
    	case HUMIDITY_IDX: return humidityType;
    	case TEMPSENS_IDX: return tempMesRoomType;
    	case TEMPSENS_THERM_IDX: return tempMesThermostatType;
    	case SETP_IDX_NONUSED: return tempSetpointType;
    	case SETP_FB_IDX: return tempSetFBType;
    	case SETP_IDX: return tempSetSetType;
    	case VALVE_IDX: return valveType;
    	case WINDOW_IDX: return winType;
    	case POWER_IDX: return powerType;
    	case CHARGE_IDX: return chargeType;
     	case POWERSUB_IDX: return powerSubPhaseType;
    	case POWEROUTLET_IDX: return powerOutletType;
    	case HEATPOWER_IDX: return heatPowerType;
    	case HEATENERGY_IDX: return heatEnergyType;
    	case HEATFLOW_IDX: return heatFlowType;
    	case HEATVOLUME_IDX: return heatVolumeType;
    	case HEATSUPPLY_IDX: return heatSupplyTempType;
    	case HEATRETURN_IDX: return heatReturnTempType;
    	case STATEFB_IDX: return stateFBType;
    	case ENERGY_IDX: return energyType;
    	case ENERGYSUB_IDX: return energySubPhaseType;
       	case PH_IDX: return phValueType;
       	case CONDUCTIVITY_IDX: return conductivityValueType;
       	case REDOX_IDX: return redoxValueType;
       	case OXYGEN_IDX: return oxygenValueType;
       	case WATERTEMP_IDX: return waterTempValueType;
       	case CO2CONC_IDX: return co2concentrationType;
       	case INTERNET_IDX: return internetType;
       	case RSSIDEV_IDX: return rssiDeviceType;
       	case RSSIPEER_IDX: return rssiPeerType;
       	case WATERVOL_IDX: return waterVolumeType;
       	case WATERFLOW_IDX: return waterFlowType;
    	default: throw new IllegalStateException("unsupported IDX:"+idxOfReqInput);
    	}
    }
	
 	public class EvalCore extends GenericGaRoEvaluationCore {
    	//final int[] idxSumOfPrevious;
    	protected final int size;
 		
 		public final long totalTime;
       	protected final long startTime;
       	protected final long endTime;
   	
    	/** Application specific state variables, see also documentation of the util classes used*/

    	protected long durationTime = 0;
      	
    	public int tsNum;
    	int powerNum;
     	
    	protected final int[] countPoints;
    	protected final long[] lastTimes ;
    	public final long[] durationTimes;
    	protected final long[] firstGapStart;
    	protected final int[] countGaps;

    	long lastTimeStampOverall;
    	public long overallGapTime;
    	
    	public String currentGwId;
    	
    	public EvalCore(List<EvaluationInput> input, List<ResultType> requestedResults,
    			Collection<ConfigurationInstance> configurations, EvaluationListener listener, long time,
    			int size, int[] nrInput, int[] idxSumOfPrevious, long[] startEnd) {
    		this.size = size;
    		
    		totalTime = startEnd[1] - startEnd[0];
    		startTime = startEnd[0];
    		endTime = startEnd[1];
    		
    		tsNum = 0;
    		for(int nr: nrInput) tsNum += nr;
    		tsNum -= powerNum;
    		
    		countPoints = new int[size];
    	    lastTimes = new long[size];
    	    durationTimes = new long[size];
    	    firstGapStart = new long[size];
    	    countGaps = new int[size]; 
      	    
    	    lastTimeStampOverall = startTime;
    	    
    	    currentGwId = QualityEvalProviderBase.this.currentGwId;
      	}
      	
    	/** In processValue the core data processing takes place. This method is called for each input
    	 * value of any input time series.*/
    	@Override
    	protected void processValue(int idxOfRequestedInput, int idxOfEvaluationInput,
    			int totalInputIdx, long timeStamp,
    			SampledValue sv, SampledValueDataPoint dataPoint, long duration) {
    		durationTime += duration;
    		
    		processCallforTS(totalInputIdx, idxOfRequestedInput, idxOfEvaluationInput,
    				timeStamp, false);
    	}
    	
    	protected void processCallforTS(int totalInputIdx, int idxOfRequestedInput, int idxOfEvaluationInput,
    			long timeStamp, boolean isVirtual) {
    		if(!isVirtual)
    			(countPoints[totalInputIdx])++;
    		final long durationLoc;
    		if(lastTimes[totalInputIdx] > 0)
    			durationLoc = timeStamp - lastTimes[totalInputIdx];
    		else {
    			if(isVirtual) durationLoc = 0;
    			else durationLoc = timeStamp - startTime;
    		}
    		if(durationLoc <= MAX_GAPTIMES_INTERNAL[idxOfRequestedInput]) {
    			durationTimes[totalInputIdx] += durationLoc;
    		} else {
    			durationTimes[totalInputIdx] += MAX_GAPTIMES_INTERNAL[idxOfRequestedInput];
    			(countGaps[totalInputIdx])++;
    			firstGapStart[totalInputIdx] = timeStamp;
       			//gapTimes[totalInputIdx] += (durationLoc - MAX_GAPTIMES_INTERNAL[idxOfRequestedInput]);
    			GaRoDataTypeParam type = getParamType(idxOfRequestedInput);
    			if(type != null && type.inputInfo != null) {
    				TimeSeriesDataImpl ts = type.inputInfo.get(idxOfEvaluationInput);
    				logger.info("Gap in "+currentGwId+":"+ts.id()+" of "+StringFormatHelper.getFormattedValue(durationLoc));
    			} else logger.info("Gap in "+currentGwId+":"+currentGwId+"#(no inputInfo) of "+StringFormatHelper.getFormattedValue(durationLoc));
    		}
    		
    		lastTimes[totalInputIdx] = timeStamp;
    		
    		long durationOverall = timeStamp - lastTimeStampOverall;
    		if(durationOverall > MAX_OVERALL_NONGAP_TIME_STD) {
    			overallGapTime += durationOverall;
    		}
    		lastTimeStampOverall = timeStamp;
    	}
    	
        protected FinalResult result = null;
        public String roomId;
        public class GapData {
        	public long duration;
        	public long firstGapStart;
        	public String devId;
        }
        public FinalResult getFinalResult() {
			if(result != null) return result;
        	result = new FinalResult();
			
        	roomId = currentRoomId;
        	
        	long minNonGapTime = (long) (totalTime * (1.0f-MAX_GAP_FOR_GOOD_SERIES_SHARE));
        	long minNonGapTimeGold = (long) (totalTime * (1.0f-MAX_GAP_FOR_GOLD_SERIES_SHARE));
			for(int idx = 0; idx < size; idx++) {
				if(lastTimes[idx] > 0 && lastTimes[idx] < (endTime-1)) {
					int idxOfRequestedInput = getRequiredInputIdx(idx);
					int idxOfEvaluationInput = getEvaluationInputIdx(idx);
					processCallforTS(idx, idxOfRequestedInput, idxOfEvaluationInput, endTime-1, true);
				}
			}
			int thisReqIdx = 0;
			int countWithData = 0;
			int countGood = 0;
			int countGoodGold = 0;
			int countTotal = 0;
			int cntGaps = 0;
			Map<String, GapData> devicesWithGaps = new HashMap<>();
			GaRoDataType[] inputs = getGaRoInputTypes();
			for(int idx = 0; idx < size; idx++) {
				/*int idxOfRequestedInput = getRequiredInputIdx(idx);
				int idxOfEvaluationInput = getEvaluationInputIdx(idx);
    			GaRoDataTypeParam type = getParamType(idxOfRequestedInput);
    			String ts = type.inputInfo.get(idxOfEvaluationInput).id();*/
    			String ts = getTimeSeriesId(idx, QualityEvalProviderBase.this);
				if(countPoints[idx] > 0) {
					if(thisReqIdx != SETP_IDX)
						result.withDataNum++;
					countWithData++;
				}
				//If the previous is false, then durationTimes must be zero, but we keep the test as it is for now
				if(durationTimes[idx] >= minNonGapTime) {
					if(thisReqIdx != SETP_IDX)
						result.goodNum++;
					else
						logger.info("!!!! Found SETP_IDX:");
					countGood++;
				} else {
					String devId = getDevicePath(ts);
					GapData curDuration = devicesWithGaps.get(devId);
					long gapTime = totalTime-durationTimes[idx];
					if(curDuration == null || (curDuration.duration < gapTime)) {
						GapData gd = new GapData();
						gd.duration = gapTime;
						gd.firstGapStart = firstGapStart[idx];
						String devIdShort = getDeviceName(idx, QualityEvalProviderBase.this);
						gd.devId = devIdShort;
						devicesWithGaps.put(devId, gd);
					}
				}
				if(durationTimes[idx] >= minNonGapTimeGold) {
					if(thisReqIdx != SETP_IDX)
						result.goodNumGold++;
					countGoodGold++;
				} else {
					//do something with non-golden series
				}
				countTotal++;
				cntGaps += countGaps[idx];
				int nextReqInput;
				if(idx == (size-1)) nextReqInput = Integer.MAX_VALUE;
				else nextReqInput = getRequiredInputIdx(idx+1);
				if(nextReqInput != thisReqIdx) {
					logger.info("Gw:"+currentGwId+" For "+inputs[thisReqIdx].label(null)+" withData:"+countWithData+" good:"+countGood+" golden:"+countGoodGold+" total:"+countTotal+" #Gaps:"+cntGaps);
					countWithData = 0;
					countGood = 0;
					countGoodGold = 0;
					countTotal = 0;
					cntGaps = 0;
				}
				thisReqIdx = nextReqInput;
			}
			for(Entry<String, GapData> gap: devicesWithGaps.entrySet()) {
    			logger.info("Total Gap in device "+currentGwId+":"+gap.getKey()+" of "+StringFormatHelperSP.getFormattedTimeOfDay(gap.getValue().duration, true)+" first starting:"+TimeUtils.getDateAndTimeString(gap.getValue().firstGapStart));
			}
			if(result.goodNum > 0) {
				result.withoutDataSensors = new ArrayList<String>();
				ArrayList<GapData> sortGaps = new ArrayList<GapData>(devicesWithGaps.values());
				sortGaps.sort(new Comparator<GapData>() {
					@Override
					public int compare(GapData o1, GapData o2) {
						//reverse oder, longest duration first
						return Long.compare(o2.duration, o1.duration);
					}
				});
				for(GapData g: sortGaps) {
					List<String> sensorsToFilterOut = getSensorsToFilerOut(currentGwId);
					if(sensorsToFilterOut != null) {
						if(sensorsToFilterOut.contains(g.devId))
							continue;
					}
	
					result.withoutDataSensors.add(g.devId);
				}
			}
			logger.info("Start:"+TimeUtils.getDateString(startTime)+" Gw:"+currentGwId+" Total withData:"+result.withDataNum+" good:"+result.goodNum+" golden:"+result.goodNumGold+" total:"+tsNum);
			return result;
        }
    }
    
    public static class FinalResult {
    	public int goodNum = 0;
    	public int goodNumGold = 0;
       	public int withDataNum = 0;
		protected ArrayList<String> withoutDataSensors;
    }
	/**
 	 * Define the results of the evaluation here including the final calculation
 	*/
    public final static GenericGaRoResultType TS_TOTAL = new GenericGaRoResultType("TS_TOTAL",
    		"Number of standard time series Homematic standard", IntegerResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new SingleValueResultImpl<Integer>(rt, cec.tsNum, inputData);
		}
    };
    public final static GenericGaRoResultType TS_GOOD = new GenericGaRoResultType("TS_GOOD",
    		"Number of standard time series Homematic standard with good data", IntegerResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			FinalResult res = cec.getFinalResult();
			return new SingleValueResultImpl<Integer>(rt, res.goodNum, inputData);
		}
    };
    public final static GenericGaRoResultType TS_GOLD = new GenericGaRoResultType("TS_GOLD",
    		"Number of standard time series Homematic standard with golden data", IntegerResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			FinalResult res = cec.getFinalResult();
			return new SingleValueResultImpl<Integer>(rt, res.goodNumGold, inputData);
		}
    };
    public final static GenericGaRoResultType TS_WITH_DATA = new GenericGaRoResultType("TS_WITH_DATA",
    		"Number of standard time series Homematic standard with any data", IntegerResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			FinalResult res = cec.getFinalResult();
			return new SingleValueResultImpl<Integer>(rt, res.withDataNum, inputData);
		}
    };

    public final static GenericGaRoResultType GAP_TIME_REL = new GenericGaRoResultType("GAP_TIME_REL",
    		"Time share in gaps between existing data", FloatResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new SingleValueResultImpl<Float>(rt, (float) ((double)ec.gapTime/cec.totalTime), inputData);
		}
    };
    public final static GenericGaRoResultType OVERALL_GAP_REL = new GenericGaRoResultType("OVERALL_GAP_REL",
    		"Time share in gaps with no data on entire gateway", FloatResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			//Finalize evaluation
			cec.getFinalResult();
			return new SingleValueResultImpl<Float>(rt, (float) ((double)cec.overallGapTime/cec.totalTime), inputData);
		}
    };
    public final static GenericGaRoResultType ONLY_GAP_REL = new GenericGaRoResultType("ONLY_GAP_REL",
    		"Time share in evals without any data",
    		FloatResource.class, ID) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			if(cec.durationTime == 0 && cec.gapTime == 0)
				return new SingleValueResultImpl<Float>(rt, 1.0f, inputData);
			else
				return new SingleValueResultImpl<Float>(rt, 0.0f, inputData);
		}
    };
    public final static GenericGaRoResultType TOTAL_TIME = new GenericGaRoResultType("TOTAL_HOURS", FloatResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new SingleValueResultImpl<Float>(rt, (float) ((double)cec.totalTime / HOUR_MILLIS), inputData);
		}
    };
    public final static GenericGaRoResultType DURATION_TIME = new GenericGaRoResultType("DURATION_HOURS", FloatResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			return new SingleValueResultImpl<Float>(rt, (float) ((double)cec.durationTime / HOUR_MILLIS), inputData);
		}
    };
    public final static GenericGaRoResultType GAP_SENSORS = new GenericGaRoResultType("$GAP_SENSORS",
    		"IDs of sensors without data or gaps", StringResource.class, null) {
		@Override
		public SingleEvaluationResult getEvalResult(GenericGaRoEvaluationCore ec, ResultType rt,
				List<TimeSeriesData> inputData) {
			EvalCore cec = ((EvalCore)ec);
			FinalResult res = cec.getFinalResult();
			return new SingleValueResultImpl<String>(rt, (res.withoutDataSensors!=null)?stringListToSingle(res.withoutDataSensors):"--", inputData);
			//return new SingleValueResultImpl<String>(rt, (cec.critSensor!=null)?cec.critSensor:"--", inputData);
		}
    };
    private static final List<GenericGaRoResultType> RESULTS = Arrays.asList(TS_TOTAL, TS_WITH_DATA, TS_GOOD, TS_GOLD,
		  OVERALL_GAP_REL,
		   GAP_TIME_REL, ONLY_GAP_REL, TOTAL_TIME, DURATION_TIME,
		   GAP_SENSORS); //EVAL_RESULT
    
	@Override
	protected List<GenericGaRoResultType> resultTypesGaRo() {
		return RESULTS;
	}

	@Override
	protected GenericGaRoEvaluationCore initEval(List<EvaluationInput> input, List<ResultType> requestedResults,
			Collection<ConfigurationInstance> configurations, EvaluationListener listener, long time, int size,
			int[] nrInput, int[] idxSumOfPrevious, long[] startEnd) {
		return new EvalCore(input, requestedResults, configurations, listener, time, size, nrInput, idxSumOfPrevious, startEnd);
	}

	@Override
	public List<PreEvaluationRequested> preEvaluationsRequested() {
		return null;
	}
	
	public static String getDevicePath(String tsId) {
		String[] subs = tsId.split("/");
		if(subs.length > 3) return subs[2];
		else return tsId; //throw new IllegalStateException("Not a valid tsId for Homematic:"+tsId);
	}
	
	public static String getDeviceShortId(String deviceLongId) {
		int len = deviceLongId.length();
		if(len < 4) return deviceLongId;
		String toTest;
		if(deviceLongId.charAt(len-2) == '_') {
			if(len < 6) return deviceLongId;
			toTest =deviceLongId.substring(len-6, len-2);
		} else toTest = deviceLongId.substring(len-4);
		return toTest;
	}
	
	public static String stringListToSingle(Collection<String> inList) {
		if(inList == null) return null;
		String result = null;
		for(String s: inList) {
			if(result == null) result = s;
			else result += ", "+s;
		}
		if(result == null) return "";
		return result ;
	}
}
