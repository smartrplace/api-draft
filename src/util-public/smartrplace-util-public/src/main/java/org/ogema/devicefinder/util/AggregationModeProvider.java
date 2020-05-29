package org.ogema.devicefinder.util;

import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;

public interface AggregationModeProvider {
	default AggregationMode getMode(String tsId) {return null;}
	default DPRoom getRoom(String isId) {return null;}
	default Resource getDeviceResource(String id) {return null;}
	
	default DatapointService getDpService() {return null;}
	/** If {@link #getDpService()} is not null and this method returns true then the datapoinr
	 * is obtained from the DatapointService
	 */
	default boolean obtainFromStandardService(String id) {return true;}
}
