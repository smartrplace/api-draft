package org.ogema.devicefinder.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.devicefinder.api.DatapointInfo;
import org.ogema.devicefinder.api.Datapoint;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;

public class DatapointInfoImpl implements DatapointInfo {

	protected AggregationMode aggregationMode;
	protected UtilityType utilityType;
	protected ValueConversion valueConversion;
	protected InterpolationMode interpolationMode;
	
	protected final Datapoint dp;

	public DatapointInfoImpl(Datapoint dp) {
		this.dp = dp;
	};
	public DatapointInfoImpl(Datapoint dp, AggregationMode aggregationMode, UtilityType utilityType) {
		this(dp);
		this.aggregationMode = aggregationMode;
		this.utilityType = utilityType;
	}

	private static final Map<UtilityType, List<GaRoDataType>> typeByUtility = new LinkedHashMap<>();
	static {
		typeByUtility.put(UtilityType.ELECTRICITY, Arrays.asList(new GaRoDataType[] {
				GaRoDataType.PowerMeterEnergy, GaRoDataType.PowerMeterOutlet
		}));
		typeByUtility.put(UtilityType.HEAT_ENERGY, Arrays.asList(new GaRoDataType[] {
				GaRoDataType.HeatEnergyIntegral
		}));
		List<GaRoDataType> mixedlist = new ArrayList<>(typeByUtility.get(UtilityType.ELECTRICITY));
		mixedlist.addAll(typeByUtility.get(UtilityType.HEAT_ENERGY));
		typeByUtility.put(UtilityType.ENERGY_MIXED, mixedlist);
		typeByUtility.put(UtilityType.WATER, Arrays.asList(new GaRoDataType[] {
				GaRoDataType.FreshWater, GaRoDataType.FreshWaterVolume
		}));
		typeByUtility.put(UtilityType.FOOD, Arrays.asList(new GaRoDataType[] {
				GaRoDataType.FoodAmount
		}));
	}
	
	public static List<GaRoDataType> getGaRotypes(UtilityType util) {
		return typeByUtility.get(util);
	}
	
	public static UtilityType getUtilityType(GaRoDataType garoType) {
		for(Entry<UtilityType, List<GaRoDataType>> tlist: typeByUtility.entrySet()) {
			if(tlist.getValue().contains(garoType))
				return tlist.getKey();
		}
		return null;
	}
	@Override
	public AggregationMode getAggregationMode() {
		if(aggregationMode != null)
			return aggregationMode;
		return DatapointInfo.getConsumptionMode(dp.getGaroDataType());
	}
	
	@Override
	public UtilityType getUtilityType() {
		if(utilityType != null)
			return utilityType;
		return getUtilityType(dp.getGaroDataType());
	}
	@Override
	public ValueConversion getValueConversion() {
		return valueConversion;
	}
	@Override
	public void setAggregationMode(AggregationMode mode) {
		aggregationMode = mode;
	}
	@Override
	public void setUtilityType(UtilityType util) {
		utilityType = util;
	}
	@Override
	public void setValueConversion(ValueConversion conversion) {
		valueConversion = conversion;
	}
	@Override
	public InterpolationMode getInterpolationMode() {
		if(interpolationMode == null)
			return InterpolationMode.NONE;
		return interpolationMode;
	}
	@Override
	public void setInterpolationMode(InterpolationMode mode) {
		interpolationMode = mode;
	}

}
