package org.ogema.devicefinder.api;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.util.DatapointImpl;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.template.LabelledItem;

/** View to {@link DeviceHandlerProvider} that is relevant for datapoint processing of
 * applications
 */
public interface DeviceHandlerProviderDP<T extends Resource> extends LabelledItem {
	Class<T> getResourceType();
	
	/** This method shall be implemented if the driver provides additional
	 * information for each device and/or each data point that shall not be modeled / provided via OGEMA resources e.g. because
	 * the content would cause too much overhead in the OGEMA database or because the information is only
	 * relevant in rare cases.<br>
	 * Note that this is a raw property data access. To get semantic property data you should call
	 * {@link #getPropertyService()}<br>
	 * The data for each device may be split on several property services so a list may be returned.*/
	default List<OGEMADriverPropertyService<?>> getDriverPropertyService() {return null;}
	
	/** Get access to semantic properties of the device and its datapoints. The parameter resources must be the
	 * resources defined by {@link InstallAppDevice#device()} for the devices provided by the handler or datapoint resources
	 * returned by {@link #getDatapoints(InstallAppDevice, DatapointService)} depending on the PropertyAccessLevel. Note that
	 * only access level DATAPOINT and DEVICE is supported here.*/
	default PropertyService getPropertyService() {return null;}
	
	/** Get all datapoints for a device. Datapoints are all sensor and actor resources that might be worth
	 * logging. Usually the datapoints shall be obtained via
	 * {@link DatapointService#getDataPointStandard(org.ogema.core.model.ValueResource)}, but in some
	 * cases also direct instances of {@link DatapointImpl} may be returned.
	 * The service shall add all information to the Datapoints it can provide that are not added
	 * automatically by the DatapointService.*/
	Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService);
	
	/** Provide a sensor value of the device that usually should be updated most frequently and that also
	 * should be most relevant for the user to check whether the device works as it should.
	 * @param device device resource
	 * @param deviceConfiguration configuration resource that contains additional configuration information
	 * @return sensor resource to be displayed in the overview table. If the device has no sensor this should be
	 * 		a feedback or status value of an actor or most relevant configuration value
	 */
	SingleValueResource getMainSensorValue(T device, InstallAppDevice deviceConfiguration);
	default Label getMainSensorLabel(T device, InstallAppDevice deviceConfiguration,
			ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh, String id_vh) {
		return null;
	}

	default SingleValueResource getMainSensorValue(InstallAppDevice deviceConfiguration) {
		@SuppressWarnings("unchecked")
		T device = (T) deviceConfiguration.device().getLocationResource();
		return getMainSensorValue(device, deviceConfiguration);
		
	}
	
	public static class SetpointData {
		public SetpointData() {}
		public SetpointData(SingleValueResource stateControl, SingleValueResource stateFeedback) {
			this.stateControl = stateControl;
			this.stateFeedback = stateFeedback;
		}
		public SingleValueResource stateControl;
		public SingleValueResource stateFeedback;
	}
	
	/** Provide information on all setpoints / actors of the device that are relevant for setpoint supervision
	 * The first element shall be the main setpoint if relevant
	 * @param <S>
	 * @param device
	 * @param deviceConfiguration
	 * @return if deviceConfiguration == null and device == null then return emptyList to indicate that device handler
	 * 		can provide setpoint information
	 */
	default List<SetpointData> getSetpointData(T device, InstallAppDevice deviceConfiguration) {
		return null;
	}
	
	default List<SetpointData> getSetpointData(InstallAppDevice deviceConfiguration) {
		@SuppressWarnings("unchecked")
		T device = (T) deviceConfiguration.device();
		if(!device.exists())
			return getSetpointData(null, null);
		else
			return getSetpointData(device, deviceConfiguration);
	}

	/** Get device name. Usually this should be the same name as the name shown in the table provided if {@link DriverHandlerProvider}
	 * is also implemented.*/
	//String getDeviceName(InstallAppDevice installDeviceRes);
	
	/** Provide information whether a sensor/actor resource inside a device is relevant for
	 * default logging.
	 * Note that the framework configuration will decide if auto logging for the device is activated and
	 * the method may not be called for all datapoints. In the initial implementation this is the case if the property
	 * org.smartrplace.apps.hw.install.autologging is set to true, but in the future the user shall be
	 * able to configure this on a per-device base.*/
	default boolean relevantForDefaultLogging(Datapoint dp, InstallAppDevice iad) {
		return true;
	};
	
	/** Get a 3-4 letter abbreviation indicating the type of device.
	 * @param device shall support a null argument
	 * @param dpService
	 * @return short id
	 * 
	 */
	String getDeviceTypeShortId(DatapointService dpService);
	
	public enum ComType {
		IP,
		ZIGBEE,
		HOMEMATIC,
		BLUETOOTH,
		WMBUS,
		BACNET_SUB,
		LOCAL_VIRTUAL,
		OTHER_UNKNOWN
	}
	default ComType getComType() {
		return ComType.OTHER_UNKNOWN;
	}
	
	/** Set title of table listing the devices processed by the DeviceHandlerProvider*/
	String getTableTitle();
	
	/** Devices controlled that e.g. need to be deactivated when device is set to trash*/
	default List<Resource> devicesControlled(InstallAppDevice iad) {
		return Arrays.asList(new Resource[] {iad.device()});
	}

	/** This method is called when the first resource of a type is created. The handler shall then
	 * fill in the {@link InstallAppDevice} resource and the device resource as a template, e.g. for
	 * alarming. If not template filling is relevant or is not supported then the method does not
	 * need to be overwritten.<br>
	 * You should call org.ogema.eval.timeseries.simple.smarteff.AlarmingUtiH#setTemplateValues for each
	 * value for which alarming shall be configurable with the relevant default alarming limit parameters.
	 * @param appDevice
	 * @param appConfigData
	 */
	default void initAlarmingForDevice(InstallAppDevice appDevice,
			HardwareInstallConfig appConfigData) {}

	/** If true then the devices attached to this DeviceHandler will be synched from a subgateway to 
	 * superior if applicable
	 */
	default boolean addDeviceOrResourceListToSync() {return true;}
	
	/** If false the sub table for the DeviceHandler shall be hidden for the non-expert view*/
	default boolean relevantForUsers() {return true;}

	/** This is a more flexible option to provide a value for
	 * {@link InstallAppDevice#minimumIntervalBetweenNewValues()}
	 * @param iad
	 * @param defaultSettingValue value set in database configuration based on datapoint and device setting. Long.MAX_VALUE if
	 * 		NoValue is disabled for the datapoint
	 * @return if not null this value is used instead of the default {@link InstallAppDevice#minimumIntervalBetweenNewValues()}. Note that
	 * 		the value is only requested and set on startup of alarming. Interval in minutes. Note that it is
	 * 		not possible currently to request a shorter interval then specified in the database for the datapoint.
	 */
	default Float getMinimumNoValueTime(InstallAppDevice iad, float defaultSettingValue) {
		return null;
	}
}
