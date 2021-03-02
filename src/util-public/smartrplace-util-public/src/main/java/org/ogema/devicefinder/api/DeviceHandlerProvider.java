package org.ogema.devicefinder.api;

import java.util.Collection;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.util.DeviceHandlerSimple;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.ogema.simulation.shared.api.RoomInsideSimulationBase;
import org.ogema.simulation.shared.api.SingleRoomSimulationBase;
import org.smartrplace.apps.hw.install.config.HardwareInstallConfig;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.autoconfig.api.DeviceTypeProvider;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;

/** Implement this service to add another device table to the hardware installation app and to provide
 * everything to provide device information for device and application configuration.<br>
 *
 * For implementation of simple standard device handlers implement the class {@link DeviceHandlerSimple}.<br>
 *
 * Note that the package and class name of a {@link DeviceHandlerProvider} should not be refactored after productive usage
 * started as the {@link #id()} method by default uses the class and package name and this id is stored persistently in the
 * {@link InstallAppDevice} resource of the device. So you would have to overwrite the id() method before you perform the
 * refactoring.<br>
 *
 * The following information is only relevant if you need to implement a more complex device handler.<br>
 * 
 * For more complex imeplemtnation you can also check exisiting implementations, e.g in
 * {@link https://github.com/smartrplace/smartr-efficiency/tree/master/monitoring-service-base/src/main/java/org/smartrplace/mqtt/devicetable},
 * especially [DeviceHandlerMQTT_Aircond](https://github.com/smartrplace/smartr-efficiency/tree/master/monitoring-service-base/src/main/java/org/smartrplace/mqtt/devicetable/DeviceHandlerMQTT_Aircond.class)
 * 
 * General implementation and testing recommendations:<br>
 *  - If the device has a setpoint and a feedback a simulation must be implemented that listens for the
 *    setpoint and sets the feedback, usually with a little time delay. See the example implementation for
 *    a template. Test that the feedback resource is set to the setpoint resource with a time delay.<br>
 *  - All measurement and feedback values should implement polling to show value updates without having
 *    to reload the page. See the example implementation for using the method setPollingInterval on the
 *    respective widgets. Use the DEFAULT_POLL_RATE defined by DeviceTableBase.<br>
 *  - If the device has at least one measurement or feedback value then the "Last Contact" column should
 *    be added. The value receiving the most updates usually should be used here, usually this is a
 *    measurement value.<br>
 *  - The following fields / methods should be part of almost all device types:<br>
 *  	 - addRoomWidget(object, vh, id, req, row, appMan, deviceRoom);<br>
 *		 - addInstallationStatus(object, vh, id, req, row, appMan, deviceRoom);<br>
 *		 - addComment(object, vh, id, req, row, appMan, deviceRoom);<br>
 *		 - addSubLocation(object, vh, id, req, row, appMan, deviceRoom);<br>
 * 
 * @param <T> resource type of the device for which data is provided by the implementation
 */
public interface DeviceHandlerProvider<T extends Resource> extends DeviceHandlerProviderDP<T> {
	/** Provide implementation of {@link PatternListenerExtended} that adds exactly those devices that
	 * shall be part of the table. If the same resource type is also provided by other drivers that require
	 * a different table in the hardware installation app or a different simulation then separate 
	 * implementations of DeviceHandlerProvider are recommended that only accept their relevant devices each.
	 * The method is called by the hardware-installation-app when the installation mode is activated, not
	 * necessarily on startup of the framework.
	 * @param advAcc
	 * @param app 
	 * @return
	 */
	PatternListenerExtended<ResourcePattern<T>, T> addPatternDemand(InstalledAppsSelector app);
	
	/** The method is called then installation mode is deactivated. In this case the pattern demand
	 * shall be removed by the application in order to avoid unnecessary load on resource checking in
	 * the system
	 * @param advAcc
	 */
	void removePatternDemand();
	
	/** Provide table to be included into the hardware-installation app. It is recommended to use the
	 * pre-implemented methods for common table elements such as
	 * {@link DeviceTableBase#addNameWidget(org.smartrplace.apps.hw.install.config.InstallAppDevice, org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper, String, de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest, de.iwes.widgets.html.complextable.RowTemplate.Row, ApplicationManager)}
	 * 
	 * @param page
	 * @param appMan
	 * @param alert the alert of the page can be used to display mesages for the user then changes are made
	 * 		in the table
	 * @param appSelector same object also provided with {@link #addPatternDemand(ResourcePatternAccess, InstalledAppsSelector)}
	 * @return object of class extending DeviceTableBase
	 */
	DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector);
	
	/** Provide simulation for a device. The primary goal of the simulation is provide a realistic feedback on
	 * setpoint actions on the device as many applications expect a feedback from the hardware device when
	 * a setpoint operation is made on an actor. For pure sensor devices a simulation usually is not necessary.
	 * 
	 * Note: It is recommended to set the {@link GaRoDataType}s provided by the driver that are not
	 * 		part of the standard types in this method also
	 * 
	 * @param resource device resource
	 * @param roomSimulation roomSimulation if applicable. Note that this is not implemented yet. Note that
	 * 		this also means that the step method in the object returned is not called
	 * @return null if no simulation is available for the device
	 */
	default List<RoomInsideSimulationBase> startSimulationForDevice(InstallAppDevice device, T deviceResource,
			SingleRoomSimulationBase roomSimulation,
			DatapointService dpService) {
		return null;
	}
	
	/** Like {@link #startSimulationForDevice(InstallAppDevice, Resource, SingleRoomSimulationBase, DatapointService)},
	 * but this method is called on device startup for every device on productive systems without
	 * simulation as well as on test systems with simulation
	 * @param device
	 * @param deviceResource
	 * @param roomSimulation
	 * @param dpService
	 * @return
	 */
	default List<RoomInsideSimulationBase> startSupportingLogicForDevice(InstallAppDevice device, T deviceResource,
			SingleRoomSimulationBase roomSimulation,
			DatapointService dpService) {
		return null;
	}
	/** Get a 3-4 letter abbreviation indicating the type of device.
	 * @param device shall support a null argument
	 * @param dpService
	 * @return short id
	 * 
	 */
	default String getDeviceTypeShortId(InstallAppDevice device, DatapointService dpService) {
		if(device == null)
			return getResourceType().getClass().getSimpleName().replaceAll("[^A-Z]", "");
		if (device.device().exists())
			return device.device().getClass().getSimpleName().replaceAll("[^A-Z]", "");
		return getDeviceTypeShortId(null, dpService);
		//return "UNK"; // unknown
	}

	/** Required for hardware installation. Provided by DeviceHandlerBase*/
	List<ResourcePattern<T>> getAllPatterns();
	
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

	/** Provide version of alarming initializiation. If a new version is provided then
	 * {@link #initAlarmingForDevice(InstallAppDevice, HardwareInstallConfig)}
	 * is called once more. Implementation should take care then that existing special configurations
	 * for the devices are not overwritten.
	 */
	default String getInitVersion() {return "";}
	
	/** Optionally als DeviceTypeProviders can be declared*/
	default Collection<DeviceTypeProvider<?>> getDeviceTypeProviders() {
		return null;
	}
	
	default void addMoreWidgetsExpert(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {};

}
