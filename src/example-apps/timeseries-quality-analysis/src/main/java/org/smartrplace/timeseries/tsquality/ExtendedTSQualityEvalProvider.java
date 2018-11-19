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
 * Note: This variant of {@link PrimaryLuthMonQualityEvalProvider} is only used for
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
	        	chargeVoltageType
		};
	}
	
	public static final int CHARGE_VOLT_IDX = 11;

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