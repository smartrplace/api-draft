package org.smartrplace.smaapis;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.PercentageResource;
import org.ogema.core.model.units.PowerResource;
import org.ogema.model.devices.connectiondevices.ElectricityConnectionBox;
import org.ogema.model.prototypes.PhysicalElement;

/**
 *
 * @author jlapp
 */
public interface SmaEnergyBalanceMeasurements extends PhysicalElement {
	
	StringResource lastMeasurementTime();
	
	ElectricityConnectionBox pvGeneration();
	
	ElectricityConnectionBox dieselGeneration();
	
	ElectricityConnectionBox combinedHeatAndPowerGeneration();
	
	ElectricityConnectionBox hydroGeneration();

	ElectricityConnectionBox directConsumption();
	
	ElectricityConnectionBox selfConsumption();
	
	ElectricityConnectionBox gridConsumption();
	
	ElectricityConnectionBox totalConsumption();
	
	PowerResource batteryCharging();
	
	PowerResource gridFeedIn();
	
	PowerResource batteryDischarging();
	
	PercentageResource batteryStateOfCharge();
	
	FloatResource autarkyRate();
	
	FloatResource selfConsumptionRate();
	
	PowerResource selfSupply();
	
}
