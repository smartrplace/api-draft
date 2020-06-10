package org.ogema.devicefinder.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.devicefinder.api.DatapointInfo.UtilityType;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
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
	
	Collection<Class<? extends Resource>> getManagedDeviceResoureceTypes();
	Collection<InstallAppDevice> managedDeviceResoures(Class<? extends Resource> resourceType);
	<T extends Resource> DeviceHandlerProviderDP<T> getDeviceHandlerProvider(InstallAppDevice installAppDeviceRes);
	
	/** Provide framework time where ApplicationManager is not available*/
	long getFrameworkTime();
	
	GatewayResource getStructure(String id, String gatewayId);
	
	/** Note: For now the room id shall always be the same as the label(English)*/
	DPRoom getRoom(String id, String gatewayId);
	DPRoom getRoom(String id);
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
}
