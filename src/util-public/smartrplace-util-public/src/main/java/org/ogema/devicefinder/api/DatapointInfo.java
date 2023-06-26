package org.ogema.devicefinder.api;

import org.ogema.core.timeseries.InterpolationMode;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

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
		Consumption2Meter,
		/** Average power or other value. The average shall apply to the entire time series until the
		 * next value. The last two values of the time series shall be equal and indicate the duration of
		 * the last interval. This can be applied to almost all evaluated measurement values and other
		 * evaluation results.
		 */
		AVERAGE_VALUE_PER_STEP
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
		/** Heating degree days for a room or for the entire building*/
		HEATING_DEGREE_DAYS,
		COOLING_DEGREE_DAYS,
		/** For generic processing also an unknown utility type be processed*/
		UNKNOWN
	}

	public static final UtilityType[] defaultSRCTypes = new UtilityType[] {UtilityType.ELECTRICITY, UtilityType.HEAT_ENERGY,
			UtilityType.WATER};
	
	public static String getDefaultShortLabel(UtilityType type) {
		switch(type) {
		case ELECTRICITY: return "Elec";
		case HEAT_ENERGY: return "Heat";
		case ENERGY_MIXED: return "Energy";
		case WATER: return "Water";
		case HEATING_DEGREE_DAYS: return "HeatDD";
		case COOLING_DEGREE_DAYS: return "CoolDD";
		case FOOD: return "Food";
		case UNKNOWN: return "UNK";
		default: throw new IllegalStateException("Unknown type: "+type);
		}
	}
	public static String getDefaultShortLabel(UtilityType type, OgemaLocale locale) {
		if(locale != OgemaLocale.GERMAN)
			return getDefaultShortLabel(type);
		switch(type) {
		case ELECTRICITY: return "Strom";
		case HEAT_ENERGY: return "Wärme";
		case ENERGY_MIXED: return "Energie";
		case WATER: return "Wasser";
		case HEATING_DEGREE_DAYS: return "HeizGT";
		case COOLING_DEGREE_DAYS: return "KühlGT";
		case FOOD: return "Futter";
		case UNKNOWN: return "UNK";
		default: throw new IllegalStateException("Unknown type: "+type);
		}
	}
	
	public static String getDefaultUnit(UtilityType type) {
		switch(type) {
		case ELECTRICITY: return "kWh";
		case HEAT_ENERGY: return "kWh";
		case ENERGY_MIXED: return "kWh";
		case WATER: return "m3";
		case HEATING_DEGREE_DAYS: return "Kd";
		case COOLING_DEGREE_DAYS: return "Kd";
		case FOOD: return "kg";
		case UNKNOWN: return "-";
		default: throw new IllegalStateException("Unknown type: "+type);
		}
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
	
	/** Check if the datapoint has a connection
	 * 
	 * @return null if no connection available, otherwise the connection of the datapoint is returned
	 */
	public DpConnection getExistingConnection();
	
	/** Get or create connection for the location requested
	 * 
	 * @param connectionLocation
	 * @param type
	 * @return suitable DpConnection for the UtilityType. The connection is unique for the gateway
	 */
	public DpConnection getConnection(String connectionLocation, UtilityType type);
	
	/** The sum up level defines which meters can be aggregated. Standard sum up level are defined in
	 * {@link SumUpLevel}
	 * @return
	 */
	public int getSumUpLevel();
	public boolean setSumUpLevel(int level);
}
