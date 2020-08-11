package org.ogema.devicefinder.api;

import java.util.Collection;

import org.ogema.core.model.Resource;
import org.ogema.devicefinder.util.DatapointImpl;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.widgets.template.LabelledItem;

/** View to {@link DeviceHandlerProvider} that is relevant for datapoint processing of
 * applications
 */
public interface DeviceHandlerProviderDP<T extends Resource> extends LabelledItem {
	Class<T> getResourceType();
	
	/** TODO: Check if this makes sense. This method shall be implemented if the driver provides additional
	 * information for each data point that shall not be modeled / provided via OGEMA resources e.g. because
	 * the content would cause too much overhead in the OGEMA database or because the information is only
	 * relevant in rare cases.*/
	default OGEMADriverPropertyService<?> getDriverPropertyService() {return null;}
	
	/** Get all datapoints for a device. Datapoints are all sensor and actor resources that might be worth
	 * logging. Usually the datapoints shall be obtained via
	 * {@link DatapointService#getDataPointStandard(org.ogema.core.model.ValueResource)}, but in some
	 * cases also direct instances of {@link DatapointImpl} may be returned.
	 * The service shall add all information to the Datapoints it can provide that are not added
	 * automatically by the DatapointService.*/
	Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService);
	
	/** Get device name. Usually this should be the same name as the name shown in the table provided if {@link DriverHandlerProvider}
	 * is also implemented.*/
	//String getDeviceName(InstallAppDevice installDeviceRes);
	
	/** Provide information whether a sensor/actor resource inside a device is relevant for
	 * default logging.
	 * Note that the framework configuration will decide if auto logging for the device is activated and
	 * the method may not be called for all datapoints. In the initial implementation this is the case if the property
	 * org.smartrplace.apps.hw.install.autologging is set to true, but in the future the user shall be
	 * able to configure this on a per-device base.*/
	default boolean relevantForDefaultLogging(Datapoint dp) {
		return true;
	};
}
