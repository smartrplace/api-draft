package org.ogema.devicefinder.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.devicefinder.api.DatapointInfo;
import org.ogema.devicefinder.api.DpConnection;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;

public class DatapointInfoImpl implements DatapointInfo {

	protected AggregationMode aggregationMode;
	protected UtilityType utilityType;
	protected ValueConversion valueConversion;
	protected InterpolationMode interpolationMode;
	protected DpConnection connection;
	protected int sumUpLevel = 0;
	
	protected final DatapointImpl dp;

	public DatapointInfoImpl(DatapointImpl dp) {
		this.dp = dp;
	};
	public DatapointInfoImpl(DatapointImpl dp, AggregationMode aggregationMode, UtilityType utilityType) {
		this(dp);
		this.aggregationMode = aggregationMode;
		this.utilityType = utilityType;
	}

	private static final Map<UtilityType, List<GaRoDataType>> typeByUtility = new LinkedHashMap<>();
	static {
		typeByUtility.put(UtilityType.ELECTRICITY, Arrays.asList(new GaRoDataType[] {
				GaRoDataType.PowerMeterEnergy, GaRoDataType.PowerMeterOutlet,
				GaRoDataType.PowerMeter, GaRoDataType.PowerMeterSubphase,
				GaRoDataType.EnergyIntegralOutlet, GaRoDataType.PowerMeterEnergySubphase
		}));
		typeByUtility.put(UtilityType.HEAT_ENERGY, Arrays.asList(new GaRoDataType[] {
				GaRoDataType.HeatEnergyIntegral, GaRoDataType.Heatpower
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
	
	@Override
	public DpConnection getExistingConnection() {
		return connection;
	}
	
	@Override
	public DpConnection getConnection(String connectionLocation, UtilityType type) {
		if(connection != null) {
			if(connection.getUtilityType() != type)
				throw new IllegalStateException("Type for "+connectionLocation+" already set to "+connection.getUtilityType().name()+", requested:"+type.name());
			return connection;
		}
		connection = dp.getConnection(connectionLocation, type);
		return connection;
	}
	@Override
	public int getSumUpLevel() {
		return sumUpLevel;
	}
	@Override
	public boolean setSumUpLevel(int level) {
		this.sumUpLevel = level;
		return true;
	}

}
