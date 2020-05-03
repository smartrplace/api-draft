package org.ogema.devicefinder.api;

import java.util.List;

import org.ogema.core.model.ValueResource;

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
	 * @param resourceLocation
	 * @return
	 */
	Datapoint getDataPointStandard(ValueResource valRes);
	Datapoint getDataPointAsIs(ValueResource valRes);
	
	/** We need some kind of filtering, but initially this is up to the application*/
	List<Datapoint> getAllDatapoints();
}
