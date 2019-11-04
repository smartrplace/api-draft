package org.smartrplace.rexometer.driver.emoncms.model;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.communication.CommunicationInformation;

/** URL of the connection shall be given in the field CommunicationInformation.communicationAddress
 *	Information on time series available on the EMonCMS shall not be kept persistently on the
 *	OGEMA system, but always be fetched from the EMonCMS and presented as DataProvider 
 */
public interface EmonCMSConnection extends CommunicationInformation {
	/** List of data that shall be read from EMonCMS
	 */
	ResourceList<EmonCMSReadConfiguration> readConfigurations();

	/** List of data that shall be sent to EMonCMS
	 * TODO: Do not implement yet
	 */
	ResourceList<EmonCMSWriteConfiguration> inputConfigurations();
	
	/** Base URL to connect to (e.g. http://rexometer.example.com/feed)*/
	StringResource url();
	/** Port to connect to if not standard
	 * @deprecated include in url
	 */
	IntegerResource port();
	
	StringResource apiKeyRead();
	StringResource apiKeyWrite();
	
	
}