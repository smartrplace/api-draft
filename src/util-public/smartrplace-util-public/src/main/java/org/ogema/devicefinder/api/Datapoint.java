package org.ogema.devicefinder.api;

import org.ogema.core.model.Resource;
import org.ogema.widgets.configuration.service.OGEMAConfigurationProvider;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public interface Datapoint extends DatapointDescAccess, GatewayResource {
	
	/** The method label shall always return a non-null String. The label usually is a combination of
	 * the type, the room and the sub-room information. Also the gateway information should be included if
	 * not on the local gateway.<br>
	 * TODO: Currently this is also implemented in TimeSeriesNameProviderImpl
	 */
	@Override
	default String label(OgemaLocale locale) {
		String stdLabel = getRoomName(locale);
		String subRoom = getSubRoomLocation(locale, null);
		if(subRoom != null)
			stdLabel += "-"+subRoom;
		stdLabel += "-"+getTypeName(locale);
		if(isLocal()) {
			String gwId = getGatewayId();
			return gwId +"::"+stdLabel;
		}
		return stdLabel;
	};
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
}
