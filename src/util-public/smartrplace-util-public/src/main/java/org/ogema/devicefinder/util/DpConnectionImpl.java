package org.ogema.devicefinder.util;

import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DpConnection;

public abstract class DpConnectionImpl implements DpConnection {
	protected final String conn;
	Datapoint powerSens;
	Datapoint energySens;
	
	public DpConnectionImpl(String connectionLocation) {
		this.conn = connectionLocation;
	}
	
	@Override
	public String getConnectionLocation() {
		return conn;
	}

	@Override
	public Datapoint getPowerSensorDp() {
		return powerSens;
	}

	@Override
	public boolean setPowerSensorDp(Datapoint dp) {
		this.powerSens = dp;
		return true;
	}

	@Override
	public Datapoint getEnergySensorDp() {
		return energySens;
	}

	@Override
	public boolean setEnergySensorDp(Datapoint dp) {
		this.energySens = dp;
		return true;
	}

	public static class DpElectricityConnectionImpl extends DpConnectionImpl implements DpElectricityConnection {
		public DpElectricityConnectionImpl(String connectionLocation) {
			super(connectionLocation);
		}

		Datapoint voltageSens;
		Datapoint currentSens;
		Datapoint frequencySens;
		Datapoint reactiveSens;
		Datapoint reactiveAngleSens;

		@Override
		public Datapoint getVoltageSensorDp() {
			return voltageSens;
		}

		@Override
		public boolean setVoltageSensorDp(Datapoint dp) {
			this.voltageSens = dp;
			return true;
		}

		@Override
		public Datapoint getCurrentSensorDp() {
			return currentSens;
		}

		@Override
		public boolean setCurrentSensorDp(Datapoint dp) {
			this.currentSens = dp;
			return true;
		}

		@Override
		public Datapoint getReactiveAngleSensorDp() {
			return reactiveAngleSens;
		}

		@Override
		public boolean setReactiveAngleSensorDp(Datapoint dp) {
			this.reactiveAngleSens = dp;
			return true;
		}

		@Override
		public Datapoint getReactivePowerSensorDp() {
			return reactiveSens;
		}

		@Override
		public boolean setReactivePowerSensorDp(Datapoint dp) {
			this.reactiveSens = dp;
			return true;
		}

		@Override
		public Datapoint getFrequencySensorDp() {
			return frequencySens;
		}

		@Override
		public boolean setFrequencySensorDp(Datapoint dp) {
			this.frequencySens = dp;
			return true;
		}
		
	}
	
	public static class DpThermalConnectionImpl extends DpConnectionImpl implements DpThermalConnection {
		public DpThermalConnectionImpl(String connectionLocation) {
			super(connectionLocation);
		}
		Datapoint flowSens;
		Datapoint inputTempSens;
		Datapoint returnTempSens;
		
		@Override
		public Datapoint getFlowSensorDp() {
			return flowSens;
		}

		@Override
		public boolean setFlowSensorDp(Datapoint dp) {
			this.flowSens = dp;
			return true;
		}

		@Override
		public Datapoint getInputTemperatureSensorDp() {
			return inputTempSens;
		}

		@Override
		public boolean setInputTemperatureSensorDp(Datapoint dp) {
			this.inputTempSens = dp;
			return true;
		}

		@Override
		public Datapoint getReturnTemperatureSensorDp() {
			return returnTempSens;
		}

		@Override
		public boolean seReturnTemperatureSensorDp(Datapoint dp) {
			returnTempSens = dp;
			return true;
		}
		
	}
	
	public static class DpFreshWaterConnectionImpl extends DpConnectionImpl implements DpFreshWaterConnection {

		public DpFreshWaterConnectionImpl(String connectionLocation) {
			super(connectionLocation);
		}
	}
	public static class DpFoodConnectionImpl extends DpConnectionImpl implements DpFoodConnection {

		public DpFoodConnectionImpl(String connectionLocation) {
			super(connectionLocation);
		}
	}
}
