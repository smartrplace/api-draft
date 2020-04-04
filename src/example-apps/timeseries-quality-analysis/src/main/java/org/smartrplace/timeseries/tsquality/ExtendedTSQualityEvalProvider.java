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

import java.util.Arrays;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;

import de.iwes.timeseries.eval.api.EvaluationProvider;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeParam;
import de.iwes.timeseries.eval.garo.multibase.generic.GenericGaRoResultType;

/**
 * Note: This variant of {@link DefaultQualityEvalProvider} is only used for
 * ScheduleViewerExpert configuration, no updates are done here
 */
@Service(EvaluationProvider.class)
@Component
public class ExtendedTSQualityEvalProvider extends QualityEvalProviderBase {
	
	/** Adapt these values to your provider*/
    public final static String ID = "extended-quality_eval_provider";
    public final static String LABEL = "Extended Quality: Gap evaluation provider";
    public final static String DESCRIPTION = "Extended Quality: Provides gap evaluation, additional information in log file";
    
    //private static final Logger logger = LoggerFactory.getLogger(ExtendedTSQualityEvalProvider.class);
    
    public ExtendedTSQualityEvalProvider() {
        super(ID, LABEL, DESCRIPTION);
    }

    /*public static final GaRoDataTypeParam motionType = new GaRoDataTypeParam(GaRoDataType.MotionDetection, false);
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
    */
    public static final GaRoDataTypeParam chargeVoltageType = new GaRoDataTypeParam(GaRoDataType.ChargeVoltage, false);
       
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
	        	chargeVoltageType
		};
	}
	
	//Additional entries start with TYPE_NUM, next additional entry would be TYPE_NUM+1 etc.
	public static final int CHARGE_VOLT_IDX = TYPE_NUM;

	@Override
	public GaRoDataTypeParam getParamType(int idxOfReqInput) {
    	switch(idxOfReqInput) {
    	case MOTION_IDX: return motionType;
    	case HUMIDITY_IDX: return humidityType;
    	case TEMPSENS_IDX: return tempMesRoomType;
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
    	case HEATRETURN_IDX: return heatReturnTempType;    	case STATEFB_IDX: return stateFBType;
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
      	case CHARGE_VOLT_IDX: return chargeVoltageType;
   	default: throw new IllegalStateException("unsupported IDX:"+idxOfReqInput);
    	}
    }
	
    private static final List<GenericGaRoResultType> RESULTS = Arrays.asList(TS_TOTAL, TS_WITH_DATA, TS_GOOD,
		   OVERALL_GAP_REL,
		   GAP_TIME_REL, ONLY_GAP_REL, TOTAL_TIME, DURATION_TIME); //EVAL_RESULT
    
	@Override
	protected List<GenericGaRoResultType> resultTypesGaRo() {
		return RESULTS;
	}
}
