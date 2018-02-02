package org.smartrplace.rexometer.driver.emoncms.model;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.prototypes.Configuration;

public interface EmonCMSInputConfiguration extends Configuration {

	/** OGEMA resource from which data shall be transmitted to EmonCMS*/
	SingleValueResource source();
	 
	/** 1: Transmit current values only<br>
	 *  2: Transmit log data only<br>
	 *  3: Transmit current value and log data for intervals that have non been transmitted yet 
	*/
	IntegerResource transmissionMode();
	
	/** Only relevant if log data transmission is activated. Log data time stamped after this
	 * time shall be transmitted as soon as possible or with the next update event (if update
	 * rate for log data is activated)
	 * 
	 */
	TimeResource lastDateTransmittedCompletely();
	
	/** -1: OnValueUpdate<br>
	 *  -2: onValueChange<br>
	 *  positive: Specification of fixed time step
	 */
	TimeResource updateRate();
	
	/** Specification of fixed time step. A constant supervision of new log data and sending it immediately
	 * would not make sense
	 */  
	TimeResource updateRateLogData();
	
	/**Usually all new values / log data with the same update rate should be sent in one input / bulk
	 * connection to the EmonCMS. If this is set to true the value shall be sent separatly avoiding
	 * potential small delays when collecting other data
	 */
	BooleanResource sendSeparately();
}

