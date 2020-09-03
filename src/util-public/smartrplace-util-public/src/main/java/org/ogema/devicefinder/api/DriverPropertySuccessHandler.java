package org.ogema.devicefinder.api;

import org.ogema.core.model.Resource;

public interface DriverPropertySuccessHandler<T extends Resource> {
	/** Notification that a requested read or write operation is finished
	 * 
	 * @param dataPointResource
	 * @param propertyId may be null e.g. if all propertyIds were requested or processed
	 * @param success if false the operation was not successful
	 * @param message further information on errors or success status, may be null
	 */
	void operationFinished(T dataPointResource, String propertyId, boolean success, String message);
}
