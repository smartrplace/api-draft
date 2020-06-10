package org.ogema.devicefinder.api;

import org.ogema.devicefinder.api.DatapointInfo.UtilityType;

public interface DpConnection {
	String getConnectionLocation();
	//boolean setConnection(Connection connection);
	
	Datapoint getPowerSensorDp();
	boolean setPowerSensorDp(Datapoint dp);
	
	Datapoint getEnergySensorDp();
	boolean setEnergySensorDp(Datapoint dp);

	UtilityType getUtilityType();
	
	public static interface DpElectricityConnection extends DpConnection {
		
		@Override
		default UtilityType getUtilityType() {
			return UtilityType.ELECTRICITY;
		}
		
		Datapoint getVoltageSensorDp();
		boolean setVoltageSensorDp(Datapoint dp);
		
		Datapoint getCurrentSensorDp();
		boolean setCurrentSensorDp(Datapoint dp);

		Datapoint getReactiveAngleSensorDp();
		boolean setReactiveAngleSensorDp(Datapoint dp);

		Datapoint getReactivePowerSensorDp();
		boolean setReactivePowerSensorDp(Datapoint dp);

		Datapoint getFrequencySensorDp();
		boolean setFrequencySensorDp(Datapoint dp);
	}
	
	public static interface DpThermalConnection extends DpConnection {
		
		@Override
		default UtilityType getUtilityType() {
			return UtilityType.HEAT_ENERGY;
		}

		Datapoint getFlowSensorDp();
		boolean setFlowSensorDp(Datapoint dp);
		
		Datapoint getInputTemperatureSensorDp();
		boolean setInputTemperatureSensorDp(Datapoint dp);

		Datapoint getReturnTemperatureSensorDp();
		boolean seReturnTemperatureSensorDp(Datapoint dp);
	}
	
	public static interface DpFreshWaterConnection extends DpConnection {
		@Override
		default UtilityType getUtilityType() {
			return UtilityType.WATER;
		}
	}
	
	public static interface DpFoodConnection extends DpConnection {
		@Override
		default UtilityType getUtilityType() {
			return UtilityType.FOOD;
		}
	}

}
