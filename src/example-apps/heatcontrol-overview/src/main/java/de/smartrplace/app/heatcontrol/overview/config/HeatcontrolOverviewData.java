package de.smartrplace.app.heatcontrol.overview.config;

import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.model.prototypes.Data;

public interface HeatcontrolOverviewData extends Data {
	TemperatureResource comfortTemperature();
	TemperatureResource lowerTemperature();
	/** 0: Do not control manual mode of thermostat at all
	 *  1: Always set to manual mode
	 *  2: If non-manual mode detected switch back to manual and activate
	 *     comfort temperature for manual control duraration of thermostat control
	 */
	IntegerResource controlManualMode(); 
}
