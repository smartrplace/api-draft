package org.ogema.devicefinder.api;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.model.sensors.GenericFloatSensor;
import org.ogema.widgets.configuration.service.OGEMAConfigurationProvider;
import org.smartrplace.apps.hw.install.dpres.SensorDeviceDpRes;
import org.smartrplace.util.frontend.servlet.UserServlet;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public interface Datapoint extends DatapointDescAccess, GatewayResource {
	
	public static final String UNKNOWN_ROOM_ID = "noRoomInfo";
	public static final String UNKNOWN_ROOM_NAME = "NRI";

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
		return UNKNOWN_ROOM_NAME;
	};
	
	/** Get the timeseriesID used by the TimeSeriesServlet
	 * @return null if no such timeseriesId has been registered
	 */
	String getTimeSeriesID();
	
	/** Note that the timeseries itself has to be put into the Map {@link UserServlet#knownTS}
	 * separatly in order to be accessible via the timeseriesID
	 * @param id if null the timeseries is registered for the timeseries servlet if this has
	 * 		not been done before and if a time series is available. This is similar to calling
	 * 		{@link #setTimeSeries(ReadOnlyTimeSeries, boolean)} with the second parameter being true
	 * @return true if set ID was set succesfully (returns never false)
	 */
	boolean setTimeSeriesID(String id);
	
	//Resource getSensorActorResource();
	//boolean setSensorActorResource(Resource resource);
	/** This shall be the highest device resource, typically of type PhysicalElement*/
	default DatapointGroup getDeviceResource() {return null;}
	default boolean setDeviceResource(DatapointGroup group) {return false;}
	default boolean setDeviceResource(Resource devResource) {return false;}
	
	/** Access to driver functionalities that is not modeled in the OGEMA data model
	 * @deprecated Setting {@link OGEMADriverPropertyService} for datapoints is not implemented yet.
	 * We currently focus on processing properties on device level. See {@link DatapointService}*/
	OGEMADriverPropertyAccess getPropertyAccess();
	
	/** @deprecated Setting {@link OGEMADriverPropertyService} for datapoints is not implemented yet.
	 * We currently focus on processing properties on device level.*/
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
	/** Add or set timeseries information for the datapoint
	 * 
	 * @param tseries
	 * @param publishViaServlet default is false
	 */
	void setTimeSeries(ReadOnlyTimeSeries tseries, boolean publishViaServlet);
	
	/** Set timeseries to be used for timestamps before the first timestamp of the standard timeseries.
	 * This can be used to provide data of devices replaced that represent the same location
	 * @param replacementTimeseries
	 */
	void setAlternativeTimeSeries(ReadOnlyTimeSeries replacementTimeseries);
	ReadOnlyTimeSeries getAlternativeTimeSeries();

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
	
	boolean setScale(ScalingProvider scale);
	
	/** Some datapoints support a current values besides time series. For datapoints based on SingleValueResource
	 * implementation is simple, otherwise the implementation should be provided via a {@link DatapointInfoProvider}*/
	Float getCurrentValue();
	boolean setCurrentValue(Float value);
	
	Object getParameter(String id);
	void setParameter(String id, Object param);
	
	/** Shall provide a {@link SingleValueResource} object in a server/superior environment*/
	public static final String MIRROR_RESOURCE_PARAM = "MIRROR_RESOURCE";
	/** If true then the MIRROR_RESOURCE is activated and containing the logged data. The remote datapoint does not need
	 * to be processed and displayed in most user applications. Can also be applied to groups.
	 */
	public static final String MIRROR_RESOURCE_ISMASKED_PARAM = "MIRROR_RESOURCE_ISMASKING";
	public static final String HEARTBEAT_STRING_PROVIDER_PARAM = "HEARTBEAT_STRING_PROVIDER";
	
	public static final String ALIAS_MAINMETER_HOURLYCONSUMPTION = "mainMeterTotalConsumptionHourly";
	public static final String ALIAS_MAINMETER_DAILYCONSUMPTION = "mainMeterTotalConsumptionDaily";
	public static final String ALIAS_MAINMETER_MONTHLYCONSUMPTION = "mainMeterTotalConsumptionMonthly";
	public static final String ALIAS_MAINMETER_YEARLYCONSUMPTION = "mainMeterTotalConsumptionYearly";
	
	public static final String ALIAS_QUALITY_DAILY = "qualityDaily";
	public static final String ALIAS_QUALITY_DAILY_GOLD = "qualityDailyGold";

	/** Only relevant for volatile datapoints with a timeseries that is also changed for
	 * the past. Note that we do not send notifications for upates behind the last datapoint. This also means
	 * that we consider the 15 minutes before the last reading to a timeseries to be generally volatile,
	 * no notifications can be expected for this time.<br>
	 * Note: Regarding blocking of time series during recalculations see ProcessedReadOnlyTimeSeries.
	 * @param startTime of interval changed
	 * @param endTime of interval changed
	 */
	void notifyTimeseriesChange(long startTime, long endTime);
	List<DpUpdated> getIntervalsChanged(long since);
	/** Get information when the time series was updated for the last time and the first start time
	 * end last end time of all updates since the time requested. 
	 * 
	 * @param since
	 * @return null if no updates occured since time requested
	 */
	DpUpdated getSingleIntervalChanged(long since);
}
