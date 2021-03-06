package org.ogema.devicefinder.api;

import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.model.sensors.Sensor;

import de.iwes.widgets.template.LabelledItem;

/** A driver can offer an reference to its {@link OGEMADriverPropertyService} for a device. The properties
 * shall be stored in sub resources below the sensor or the value resource for which properties are provided.
 * If the properties are stored per device then the resources can be placed below the device resource. The
 * properties shall be stored via StringArrayResources named propertyNames and propertyValues where
 * propertyNames holds the property names and propertyValues holds the values for each property at the same
 * index as the respective property name. See BACnet driver for example.
 * 
 * @author dnestle
 *
 * @param <T> for most drivers T can be {@link SingleValueResource} or {@link ValueResource}
 * 		or {@link Sensor} or the device type extending {@link PhysicalElement}
 */
public interface OGEMADriverPropertyService<T extends Resource> extends LabelledItem {
	
	/** Request that a certain property shall be updated	 * 
	 * @param dataPointResource dataPoint collecting resource e.g. {@link BACnetSubdevice}
	 * @param propertyId id of property as listed in a StringArrayResource named propertyNames below
	 * 		the dataPointResource. The values shall be in propertyValues 
	 * @param logger may be null if no logging required
	 */
	default public void updateProperty(T dataPointResource, String propertyId, OgemaLogger logger) {
		updateProperty(dataPointResource, propertyId, logger, null);
	};
	
	/** Like {@link #updateProperty(Resource, String, OgemaLogger)}
	 * 
	 * @param dataPointResource
	 * @param propertyId
	 * @param logger
	 * @param successHandler if non-null the method
	 * 		{@link DriverPropertySuccessHandler#operationFinished(Resource, String, boolean, String)}
	 * 		shall be called when the read-operation on the hardware is finished
	 */
	public void updateProperty(T dataPointResource, String propertyId, OgemaLogger logger,
			DriverPropertySuccessHandler<T> successHandler);
	
	/** Request that a certain property shall be written
	 * @param dataPointResource see {@link #updateProperty(PhysicalElement, String, OgemaLogger)}
	 * @param propertyId see {@link #updateProperty(PhysicalElement, String, OgemaLogger)}
	 * @param logger may be null if no logging required
	 * @param value to write. If the value cannot be converted to the destination format a NumberFormatException
	 *  		or a NullPointerException shall be thrown
	 */
	default public void writeProperty(T dataPointResource, String propertyId, OgemaLogger logger,
			String value) {
		writeProperty(dataPointResource, propertyId, logger, value, null);
	}

	/** Like {@link #writeProperty(Resource, String, OgemaLogger, String)}
	 * 
	 * @param dataPointResource
	 * @param propertyId
	 * @param logger
	 * @param value
	 * @param successHandler if non-null the method
	 * 		{@link DriverPropertySuccessHandler#operationFinished(Resource, String, boolean, String)}
	 * 		shall be called when the read-operation on the hardware is finished
	 */
	public void writeProperty(T dataPointResource, String propertyId, OgemaLogger logger, String value,
			DriverPropertySuccessHandler<T> successHandler);

	/** Request that the StringArrayResources named propertyNames and propertyValues are created and filled
	 * with all available values. If the resources exists already they shall be updated
	 * @param dataPointResource see {@link #updateProperty(PhysicalElement, String, OgemaLogger)}
	 * @param logger may be null if no logging required
	 */
	default public void updateProperties(T dataPointResource, OgemaLogger logger) {
		updateProperties(dataPointResource, logger, null);
	}
	
	/** Like {@link #updateProperties(Resource, OgemaLogger)}
	 * 
	 * @param dataPointResource
	 * @param logger
	 * @param successHandler if non-null the method
	 * 		{@link DriverPropertySuccessHandler#operationFinished(Resource, String, boolean, String)}
	 * 		shall be called when the read-operation on the hardware is finished
	 */
	public void updateProperties(T dataPointResource, OgemaLogger logger,
			DriverPropertySuccessHandler<T> successHandler);
	
	/** Get base resource type of dataPointResurce. If different types are used return the
	 * most basic common type. It may be better to offer seperate services for very different
	 * dataPointResource types.
	 * @return resource type
	 */
	public Class<T> getDataPointResourceType();
	
	/** Note that this may have to be extended in the future*/
	public enum AccessAvailability {
		READ,
		/** WRITE implies READ, see also WRITE_ONLY if READ is not supported*/
		WRITE,
		/** Optional not yet supported in the Bacnet driver. If a property is declared for an objectType it should at least be 
		 * accessible as READ
		 */
		OPTIONAL,
		WRITE_ONLY,
		UNKNOWN
	}
	
	/** Get access characteristics of a property
	 * 
	 * @param dataPointResource see {@link #updateProperty(PhysicalElement, String, OgemaLogger)}
	 * @param propertyId see {@link #updateProperty(PhysicalElement, String, OgemaLogger)}
	 * @return
	 */
	public AccessAvailability getReadWriteType(T dataPointResource, String propertyId);
}