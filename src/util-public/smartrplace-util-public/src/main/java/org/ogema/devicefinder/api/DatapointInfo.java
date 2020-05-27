package org.ogema.devicefinder.api;

import org.ogema.core.timeseries.InterpolationMode;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;

/** The interface is intended as general access point for various information that may either be stored
 * explicitly here or may be derived from other Datapoint information
 * 
 * @author dnestle
 *
 */
public interface DatapointInfo {

	public static enum AggregationMode {
		/** In this mode the input is expected as power value that has to be integrated over time to get
		 * a daily value*/
		Power2Meter,
		/** In this mode the input is a meter value that has to be read e.g. once per day to
		 * generate daily values or the first derivate has to be calculated to get power values
		 */
		Meter2Meter,
		/** In this mode the input contains consumption values that reflect the consumption since the 
		 * last value provided. So these values have to be added up to generate a real meter or have to
		 * be divided by the respective time step to get power estimation values*/
		Consumption2Meter
	}
	
	public static enum UtilityType {
		/** Default unit is kWh or kW*/
		ELECTRICITY,
		/** Default unit is kWh or kW*/
		HEAT_ENERGY,
		/** Electricity and heat energy, default unit is kWh or kW*/
		ENERGY_MIXED,
		/** Default unit is m3 or l/h*/
		WATER,
		/** Default unit is kg*/
		FOOD,
		/** For generic processing also an unknown utility type be processed*/
		UNKNOWN
	}

	/** Definition of the conversion of datapoint values into OGEMA standard conformant values*/
	public static interface ValueConversion {
		/** Convert into standard conformant value
		 * @param dpValue value in the internal representation of the Datapoint
		 * @return standard conformant value
		*/
		float getStdValue(float dpValue);
	}
		
	public static AggregationMode getConsumptionMode(GaRoDataType type) {
		if(GaRoDataType.volumeTypes.contains(type))
			return AggregationMode.Meter2Meter;
		if(GaRoDataType.volumeStepTypes.contains(type))
			return AggregationMode.Consumption2Meter;
		if(GaRoDataType.powerTypes.contains(type))
			return AggregationMode.Power2Meter;
		return null;
	}
	
	public AggregationMode getAggregationMode();
	public UtilityType getUtilityType();
	public ValueConversion getValueConversion();
	public void setAggregationMode(AggregationMode mode);
	public void setUtilityType(UtilityType util);
	public void setValueConversion(ValueConversion conversion);

	public InterpolationMode getInterpolationMode();
	public void setInterpolationMode(InterpolationMode mode);
}
