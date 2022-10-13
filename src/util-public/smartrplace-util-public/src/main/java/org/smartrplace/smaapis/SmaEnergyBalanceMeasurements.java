package org.smartrplace.smaapis;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.units.PercentageResource;
import org.ogema.core.model.units.PowerResource;
import org.ogema.model.prototypes.PhysicalElement;

/**
 *
 * @author jlapp
 */
public interface SmaEnergyBalanceMeasurements extends PhysicalElement {
	
	StringResource lastMeasurementTime();
	
	PowerResource pvGeneration();
	
	PowerResource dieselGeneration();
	
	PowerResource combinedHeatAndPowerGeneration();
	
	PowerResource hydroGeneration();

	PowerResource directConsumption();
	
	PowerResource selfConsumption();
	
	PowerResource totalConsumption();
	
	PowerResource batteryCharging();
	
	PowerResource gridFeedIn();
	
	PowerResource batteryDischarging();
	
	PercentageResource batteryStateOfCharge();
	
	FloatResource autarkyRate();
	
	FloatResource selfConsumptionRate();
	
	PowerResource gridConsumption();
	
	PowerResource selfSupply();
	
}
