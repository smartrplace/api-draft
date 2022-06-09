package org.ogema.devicefinder.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.devicefinder.api.DatapointInfo.UtilityType;
import org.ogema.messaging.api.MessageTransport;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.app.useradmin.api.UserDataAccess;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.autoconfig.api.OSGiConfigAccessService;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper.RecIdVal;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper.TypeChecker;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Service for interchanging extended Datapoint information between applications
 * TODO: Shall be moved to ogema-widgets repository when more stable
 * The service provides a standard implementation for Datapoint that collects information from the driver and from
 * a DatapointInfoProvider. Further information can be added directly when it is generated e.g. from the
 * TimeSeriesServlet or via a listener. I should be possible to replace the entire standard implementation in the 
 * future, but this should be done with great care in order to to loose important standard features, so this is no
 * offered in the first place.
 * TODO: We need a listener concept for this service, but we do not implement this in the first step
 */
public interface DatapointService {
	/** Get Datapoint information for a resource location. The information is filled with all
	 * standard information. If you make a lot of calls and you know that all information you need
	 * is already stored for the resource location then you can speed up the function using
	 * {@link #getDataPointAsIs(String)}.<br>
	 * The Datapoint object can be extended via its set/add functions.
	 * @param resourceLocation
	 * @param gatewayId
	 * @return
	 */
	Datapoint getDataPointStandard(String resourceLocation, String gatewayId);
	Datapoint getDataPointAsIs(String resourceLocation, String gatewayId);
	
	/** Like {@link #getDataPointStandard(String, String)}, but for local gateway
	 * @param resourceLocation
	 * @return
	 */
	Datapoint getDataPointStandard(String resourceLocation);
	Datapoint getDataPointAsIs(String resourceLocation);

	/** Like {@link #getDataPointStandard(String, String)}, but for local gateway
	 * @param valRes
	 * @return
	 */
	Datapoint getDataPointStandard(ValueResource valRes);
	Datapoint getDataPointAsIs(ValueResource valRes);
	
	/** We need some kind of filtering, but initially this is up to the application*/
	List<Datapoint> getAllDatapoints();
	Collection<Datapoint> getAllDatapoints(String gwId);
	
	public static enum DataTypeRegistrationStatus {
		/** means all known data types even if no datapoint is registered*/
		ALL,
		/** default evaluation types. This usually is a set of standard types plus types registered*/
		EVAL_DEFAULT,
		/** types are returned for which at least one datapoint is registered*/
		REGISTERED,
		/** types are returned that are explicitly registered for evaluation and plotting*/		
		FOR_EVAL
	}
	
	/** Get GaRoDataTypes for which data points have been registered
	 * 
	 * @param filter filter to be applied to known types
	 * @return list of types
	 */
	List<GaRoDataType> getRegisteredDataTypes(DataTypeRegistrationStatus filter);
	
	/** Works like {@link #getRegisteredDataTypes(DataTypeRegistrationStatus)}, but also returns
	 * additional description information.
	 * 
	 * @param filter filter to be applied to known types
	 * @param includeEmptyDescriptions if true the list returned has the same length as {@link #getRegisteredDataTypes(DataTypeRegistrationStatus)}
	 * 		for the same filter. If false types without description available are omitted.
	 * @return map including type descriptions. The key usually is the {@link GaRoDataTypeI#label(null)}
	 */
	Map<String, RecIdVal> getDataTypeDescriptions(DataTypeRegistrationStatus filter,
			boolean includeEmptyDescriptions);
	
	/** Add description for a data type. If the type already has a description registered then the information shall be
	 * merged into the existing description with additional priority for the new information. Note that additions
	 * to the default data types defined in {@link GaRoEvalHelper} may not work in the same way.
	 * 
	 * @param type
	 * @param snippets
	 * @param labelEnglish
	 * @return RecIdVal generated
	 */
	RecIdVal addDataTypeDescription(GaRoDataType type, List<String> snippets, String labelEnglish, boolean registerForEvalution);
	RecIdVal addDataTypeDescription(GaRoDataType type, List<String> snippets, Map<OgemaLocale, String> labels);
	RecIdVal addDataTypeDescription(GaRoDataType type, TypeChecker typeChecker, String labelEnglish, boolean registerForEvalution);
	RecIdVal addDataTypeDescription(GaRoDataType type, TypeChecker typeChecker, Map<OgemaLocale, String> labels);
	RecIdVal addDataTypeDescription(RecIdVal recIdVal);
	
	/**
	 * 
	 * @param type
	 * @return null if no description is available for the type
	 */
	RecIdVal registerTypeForEvaluation(GaRoDataType type);
	
	/** Provide framework time where ApplicationManager is not available*/
	long getFrameworkTime();
	
	GatewayResource getStructure(String id, String gatewayId);
	List<GatewayResource> getAllStructures();
	
	/** Note: For now the room id shall be the room location on the source gateway. For local rooms the name shall be determined
	 * from the resource, for others user {@link DPRoom#setLabel(String, OgemaLocale)}.*/
	DPRoom getRoom(String id, String gatewayId);
	DPRoom getRoom(String id);
	List<DPRoom> getAllRooms();
	//TODO: Devices have to be modeled as GatewayResource also when the concept has been proven for rooms
	
	/** Get all connections registered so far
	 * 
	 * @param type if null connections for all types are returnde
	 * @param gatewayId
	 * @return
	 */
	List<DpConnection> getConnections(UtilityType type, String gatewayId);
	
	/** Like {@link #getConnections(UtilityType, String)} but just for the local gateway
	 * 
	 * @param type
	 * @return
	 */
	List<DpConnection> getConnections(UtilityType type);
	
	/** Device information
	 * 
	 * @param basedOnDeviceHandlers if true only types supported by active DeviceHandlers are returned, otherwise
	 * 		all types used in knownDevices are returned. This may make a particular difference during startup,
	 * 		but also if DeviceHandlers are not registered anymore and if not device has been found by a DeviceHandler.
	 * @return
	 */
	Collection<Class<? extends Resource>> getManagedDeviceResourceTypes(boolean basedOnDeviceHandlers);
	Collection<InstallAppDevice> managedDeviceResoures(Class<? extends Resource> resourceType);
	Collection<InstallAppDevice> managedDeviceResoures(String deviceHandlerId, boolean shortId);
	
	/** Get resources managed by deviceHandler
	 * 
	 * @param deviceHandlerId if null devices for all DeviceHandlers will be returned
	 * @param shortId if true the deviceHandlerId only has to end with the shortId, otherwise it has
	 * 		to be equal to be added to the result
	 * @param returnAlsoTrash if false then trash resources will be filtered out. Default is false.
	 * @return
	 */
	Collection<InstallAppDevice> managedDeviceResoures(String deviceHandlerId, boolean shortId, boolean returnAlsoTrash);
	
	InstallAppDevice getMangedDeviceResource(PhysicalElement device);
	InstallAppDevice getMangedDeviceResourceForSubresource(Resource subRes);
	
	/** This handler also provides access to Driver Properties*/
	<T extends Resource> DeviceHandlerProviderDP<T> getDeviceHandlerProvider(InstallAppDevice installAppDeviceRes);
	<T extends Resource> DeviceHandlerProviderDP<T> getDeviceHandlerProvider(String devHandId);
	
	List<DeviceHandlerProviderDP<?>> getDeviceHandlerProviders();
	
	/** Get all groups for which {@link #getGroup(String)} has been called at least once<br>
	 * Note that each DatapointGroup for a DEVICE has the device() resource location as id<br>
	 * groups of type DEVICE_TYPE have the DeviceHandler id as id*/
	List<DatapointGroup> getAllGroups();
	
	/** Get or create group*/
	DatapointGroup getGroup(String id);
	
	/** true if {@link #getGroup(String)} has been called once for the respective id*/
	boolean hasGroup(String id);
	
	AlarmingService alarming();
	
	/** Provides all known driver property services. This is required by {@link DeviceHandlerProvider}s in order to provide
	 * access to driver properties.*/
	Map<String, OGEMADriverPropertyService<?>> driverpropertyServices();
	
	TimedJobMgmtService timedJobService();
	Collection<MessageTransport> messageTransportServices();
	MessageTransport messageTransportService(String adressType);
	UserDataAccess userAdminDataService();
	
	default OSGiConfigAccessService configService() {
		return null;
	}
	default VirtualScheduleService virtualScheduleService() {
		return null;
	};
}
