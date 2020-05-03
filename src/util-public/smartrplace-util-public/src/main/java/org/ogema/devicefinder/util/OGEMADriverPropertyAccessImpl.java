package org.ogema.devicefinder.util;

import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.OGEMADriverPropertyAccess;
import org.ogema.devicefinder.api.OGEMADriverPropertyService;
import org.ogema.devicefinder.api.OGEMADriverPropertyService.AccessAvailability;

public class OGEMADriverPropertyAccessImpl<T extends Resource> implements OGEMADriverPropertyAccess {
	protected final OGEMADriverPropertyService<T> driverService;
	protected final OgemaLogger logger;
	protected final T dataPointResource;

	public OGEMADriverPropertyAccessImpl(OGEMADriverPropertyService<T> driverService, OgemaLogger logger,
			T dataPointResource) {
		this.driverService = driverService;
		this.logger = logger;
		this.dataPointResource = dataPointResource;
	}

	@Override
	public void updateProperty(String propertyId) {
		driverService.updateProperty(dataPointResource, propertyId, logger);	
	}

	@Override
	public void writeProperty(String propertyId, String value) {
		driverService.writeProperty(dataPointResource, propertyId, logger, value);
	}

	@Override
	public void updateProperties() {
		driverService.updateProperties(dataPointResource, logger);
	}

	@Override
	public Class<? extends Resource> getDataPointResourceType() {
		return driverService.getDataPointResourceType();
	}

	@Override
	public AccessAvailability getReadWriteType(String propertyName) {
		return driverService.getReadWriteType(dataPointResource, propertyName);
	}
}
