package org.ogema.devicefinder.api;

public class ConsumptionInfo {

	public ConsumptionInfo(AggregationMode aggregationMode, UtilityType utilityType) {
		this.aggregationMode = aggregationMode;
		this.utilityType = utilityType;
	}
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
		/** For generic processing also an unknwon utility type be processed*/
		UNKNOWN
	}

	public final AggregationMode aggregationMode;
	public final UtilityType utilityType;
}
