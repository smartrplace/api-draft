package org.ogema.devicefinder.api;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.devicefinder.util.DeviceTableRaw;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.template.LabelledItem;

/** Implement this service to add another device table to the hardware installation app and to provide
 * everything to provide driver information for device and application configuration
 *
 * It is also recommended to use an existing implementation as template such as
 * {@link https://github.com/smartrplace/smartr-efficiency/tree/master/monitoring-service-base/src/main/java/org/smartrplace/mqtt/devicetable},
 * especially [DeviceHandlerMQTT_Aircond](https://github.com/smartrplace/smartr-efficiency/tree/master/monitoring-service-base/src/main/java/org/smartrplace/mqtt/devicetable/DeviceHandlerMQTT_Aircond.class)
 * 
 * General implementation and testing recommendations:<br>
 * ...
 * 
 * @param <T> resource type of the device for which data is provided by the implementation
 */
public interface DriverHandlerProvider extends LabelledItem {

	public static interface DriverDeviceConfig {
		
		/** Get the reference to the device configured if the configured device was found and created
		 * successfully.
		 * @return null if no device resource has been created yet
		 */
		Resource getInstallResource();		
	}
	
	/** Configurations that are made once for a driver 
	/** Get list of devices already configured*/
	List<DriverDeviceConfig> getDeviceConfigs();

	/** Get device handler providers. Note that {@link DeviceHandlerProvider}s may be configured
	 * somewhere else, in this case null may be returned even if the resource type is supported by
	 * the driver.
	 * @param <T>
	 * @param resourceType
	 * @return
	 */
	//<T extends Resource> DeviceHandlerProvider<T> getDeviceHandlerProvider(Class<T> resourceType);
	
	/** Get device handler providers relevant for the devices provided by the driver.
	 * Note that {@link DeviceHandlerProvider}s may be configured
	 * somewhere else, in this case relevant entries may be missing or null may be returned.
	 * @param registerByFramework if true the entries in the list will be registered as service
	 * 		each by the framework so that they can be found by the HardwareInstallation App.
	 * 		Otherwise the service registration must be done by the driver.
	 * @return
	 */
	List<DeviceHandlerProvider<?>> getDeviceHandlerProviders(boolean registerByFramework);
	
	/** This table usually only has a single line
	 * @return null if no init configurations for the driver that are independent of single devices
	 * 		can be made for the driver*/
	DeviceTableRaw<DriverHandlerProvider, Resource> getDriverInitTable();
	
	/** This table contains an entry per device configured. The devices obtained may have different
	 * resourece types and this may required several {@link DeviceTableBase}s.
	 * This is the table for configuration of device access data. This table may contain some measurements from
	 * a device configured, but shall not be used for the configuration of room information etc. Note that
	 * such a table only exists for devices that are configured manually. For drivers that detect devices
	 * automatically only {@link DeviceHandlerProvider} is used.
	 * @param page
	 * @param alert
	 * @param appSelector can be used to get the {@link InstallAppDevice} resoure for the DriverDeviceConfig
	 * 		if a device resource is available for an entry
	 * @param addUnfoundDevices if false devices that have not device resource and thus no InstalAppDevice
	 * 		resource shall not be part of the table. Other entries shall be determined based on
	 * 		selector#getDevicesSelected
	 * @return null if no per-device configurations can be made for the driver. In this case the
	 * 		{@link #getDriverInitTable()} should return an object as at least one of the tables shall
	 * 		be provided. */
	DeviceTableRaw<DriverDeviceConfig, Resource> getDriverPerDeviceConfigurationTable(WidgetPage<?> page,
			Alert alert, InstalledAppsSelector selector, boolean addUnfoundDevices);
	
	/** TODO: Check whether this shall be part of this interface or in {@link DeviceHandlerProvider}.
	 * This method shall be implemented if the driver provides additional
	 * information for each data point that shall not be modeled / provided via OGEMA resources e.g. because
	 * the content would cause too much overhead in the OGEMA database or because the information is only
	 * relevant in rare cases.*/
	default OGEMADriverPropertyService<?> getDriverPropertyService() {return null;}
	
	/** Get link to driver documentation page. Usually this is an absolute URL
	 * 
	 * @param publicVersion if true a page accessible publicly for all users shall be returned. Note that some
	 * 		more granularity in the access level will be introduced soon
	 * @return null if no fitting documentation page is available
	 */
	String getDriverDocumentationPageURL(boolean publicVersion);
	
	/** Get a link to the driver configuration page. For drivers like ModBus typically profiles definied registers
	 * to be used have to be defined for device types, which cannot be done via the DriverHandlerProvider tables.
	 * This must be a relative URL for the local system.
	 * @return null if no driver configuration page is required/available.
	 */
	default String getDriverConfigurationPageURL() {return null;}
}
