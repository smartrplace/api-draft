package org.ogema.devicefinder.api;

import java.util.HashMap;
import java.util.Map;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;

public class DpUpdateAPI {
	/** Gap or interval requiring recalculation requirement*/
	public static class DpGap {
		public long start;
		public long end;
	}
	
	public static class DpUpdated extends DpGap {
		//Time when datapoint time series as updated for a certain interval
		public long updateTime;
	}
	
	/** Replaced by {@link GaRoDataTypeI#aggregationMode()}*/
	@Deprecated 
	public static final Map<String, String> aggregationOptions = new HashMap<>();
	static {
		aggregationOptions.put("0", "(unknown)");
		aggregationOptions.put("1", "power/volume flow");
		aggregationOptions.put("2", "aggregated:counter/reading");
		aggregationOptions.put("3", "consumption per timeStep");
		aggregationOptions.put("4", "consumptionDaily");
		aggregationOptions.put("5", "consumptionDaily");
		aggregationOptions.put("6", "consumptionMonthly");
		aggregationOptions.put("7", "consumptionYearly");
	}
}
