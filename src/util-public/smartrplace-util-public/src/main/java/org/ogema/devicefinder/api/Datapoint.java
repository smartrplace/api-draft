package org.ogema.devicefinder.api;

import org.ogema.core.model.Resource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.model.sensors.Sensor;
import org.ogema.widgets.configuration.service.OGEMAConfigurationProvider;
import org.smartrplace.apps.hw.install.dpres.SensorDeviceDpRes;
import org.smartrplace.util.frontend.servlet.UserServlet;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public interface Datapoint extends DatapointDescAccess, GatewayResource {
	
	public static final String UNKNOWN_ROOM_ID = "noRoomInfo";

	/** The method label shall always return a non-null String. The label shall be used by evaluation tables,
	 * charts etc. by default as human readable label. The label usually is a combination of
	 * the type, the room and the sub-room information. Also the gateway information should be included if
	 * not on the local gateway.<br>
	 * TODO: Currently there is a different implementation for a similar task in
	 * 		TimeSeriesNameProviderImpl#getShortNameForTypeI
	 */
	@Override
	String label(OgemaLocale locale);
	
	/** Unique id for the datapoint source. For datapoints stored in an instance of
	 * {@link DatapointService} uniqueness shall be guaranteed within the gateway scope of the
	 * DatapointService providing the data point. So gatewayId plus location shall be sufficient to
	 * identify a datapoint.<br>
	 * In some cases a location String may be requird that contains also gateway information. In this
	 *   case the gateway information shall be added as a String prefix in the form of <GatewayID>::
	 *   like for the default label implementation.
	 * For resources usually the resource location shall be returned. For other timeseries the
	 * timeseriesID shall be returned. For timeseries with an evaluation/calculation history the history
	 * shall be returned as JSON String (details tbd).*/
	@Override
	String getLocation();
	
	/** id and location shall always be the same*/
	@Override
	default String id() {
		return getLocation();
	}
	
	/** The method getRoomName shall always return a non-null String even if no room 
	 * information is available;
	 */
	default String getRoomName(OgemaLocale locale) {
		return "noRoomInfo";
	};
	
	/** Get the timeseriesID used by the TimeSeriesServlet
	 * @return null if no such timeseriesId has been registered
	 */
	String getTimeSeriesID();
	
	/** Note that the timeseries itself has to be put into the Map {@link UserServlet#knownTS}
	 * separatly in order to be accessible via the timeseriesID
	 * @param id
	 * @return true if set ID was set succesfully (returns never false)
	 */
	boolean setTimeSeriesID(String id);
	
	Resource getSensorActorResource();
	boolean setSensorActorResource(Resource resource);
	/** This shall be the highest device resource, typically of type PhysicalElement*/
	default Resource getDeviceResource() {return getSensorActorResource();}
	default boolean setDeviceResource(Resource resource) {return false;}
	
	/** Access to driver functionalities that is not modeled in the OGEMA data model*/
	OGEMADriverPropertyAccess getPropertyAccess();
	
	boolean setDriverService(OGEMADriverPropertyService<Resource> driverService);
	
	/** Register Provider for information
	 * @param provider
	 * @param priority see {@link OGEMAConfigurationProvider#priority()}: The smaller the number the higher the priority.
	 * 			Default is 1000.
	 * @return true if the service is currently used. Note that another service with a higher priority may be registered later.
	 */
	boolean registerInfoProvider(DatapointInfoProvider provider, int priority);
	
	boolean unregisterInfoProvider(DatapointInfoProvider provider);
	
	/** Get time series representation for evalution API V1
	 * @param locale determines label and description of the timeseries, they are equal to {@link #label(OgemaLocale)}(null)
	 * @return null if not timeseries information is availble. If {@link #getTimeSeries()} is non-null 
	 * 		then also a non-null value shall be returned here.*/
	TimeSeriesDataImpl getTimeSeriesDataImpl(OgemaLocale locale);
	
	ReadOnlyTimeSeries getTimeSeries();
	void setTimeSeries(ReadOnlyTimeSeries tseries);
	void setTimeSeries(ReadOnlyTimeSeries tseries, boolean publishViaServlet);
	
	/** Datapoints that are not based on a resource can be registered as virtual sensors. New
	 * values shall be written to the Sensor.reading() resource. This functionality is intended for
	 * debugging only and the sensor resource may be deleted at any time. The sensor will be added to
	 * a default {@link SensorDeviceDpRes} sensor device automatically generated for this purpose.<br>
	 * Note also that this usually only works for datapoints obtained via the default {@link DatapointService}
	 * implementation.<br>
	 * The method can be called several times for the same datapoint just to get the Sensor resource.<br>
	 * Data logging is activated automatically for the Sensor.reading() resource by the
	 * default {@link DatapointService} implementation based on the default
	 * {@link DeviceHandlerProvider#relevantForDefaultLogging(Datapoint)} implementation.
	 * @return sensor resource used for the datapoint. 
	 */
	GenericFloatSensor registerAsVirtualSensor();
	
	/** Like {@link #registerAsVirtualSensor()}, but a {@link SensorDeviceDpRes} will be used that is
	 * identified based on the sensorDeviceName
	 * 
	 * @param sensorDeviceName
	 * @return
	 */
	GenericFloatSensor registerAsVirtualSensor(String sensorDeviceName);
}
