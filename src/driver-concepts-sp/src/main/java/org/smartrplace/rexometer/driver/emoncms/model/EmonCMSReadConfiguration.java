package org.smartrplace.rexometer.driver.emoncms.model;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.prototypes.Configuration;

/** Configuration for reading data from an Emoncms data base
 * Note: In the future also reading time series data shall be supported, currently only
 * reading the current/latest value is supported.*/
public interface EmonCMSReadConfiguration extends Configuration {

	/** OGEMA resource from which data shall be transmitted to EmonCMS*/
	SingleValueResource destination();
	 
	/** Id to be used to identify the Emoncms field from which to read*/
	IntegerResource fieldId();
	
	/** Time stamp of latest value read. Usually a new value shall only be written to
	 * destination if the time stamp in the Emoncms database changed
	 */
	TimeResource lastValueTimestamp();
	
	/** Polling rate for connections to Emoncms
	 */
	TimeResource pollRate();
}

