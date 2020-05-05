package org.ogema.devicefinder.api;

import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.model.prototypes.PhysicalElement;

public interface OGEMADriverPropertyService<T extends Resource> {
	
	/** Request that a certain property shall be updated	 * 
	 * @param dataPointResource dataPoint collecting resource e.g. {@link BACnetSubdevice}
	 * @param propertyId id of property as listed in a StringArrayResource named propertyNames below
	 * 		the dataPointResource. The values shall be in propertyValues 
	 * @param logger may be null if no logging required
	 */
	public void updateProperty(T dataPointResource, String propertyId, OgemaLogger logger);
	
	/** Request that a certain property shall be written
	 * @param dataPointResource see {@link #updateProperty(PhysicalElement, String, OgemaLogger)}
	 * @param propertyId see {@link #updateProperty(PhysicalElement, String, OgemaLogger)}
	 * @param logger may be null if no logging required
	 * @param value to write. If the value cannot be converted to the destination format a NumberFormatException
	 *  		or a NullPointerException shall be thrown
	 */
	public void writeProperty(T dataPointResource, String propertyId, OgemaLogger logger, String value);

	/** Request that the StringArrayResources named propertyNames and propertyValues are created and filled
	 * with all available values. If the resources exists already they shall be updated
	 * @param dataPointResource see {@link #updateProperty(PhysicalElement, String, OgemaLogger)}
	 * @param logger may be null if no logging required
	 */
	public void updateProperties(T dataPointResource, OgemaLogger logger);
	
	/** Get base resource type of dataPointResurce. If different types are used return the
	 * most basic common type. It may be better to offer seperate services for very different
	 * dataPointResource types.
	 * @return resource type
	 */
	public Class<T> getDataPointResourceType();
	
	/** Note that this may have to be extended in the future*/
	public enum AccessAvailability {
		READ,
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
	public AccessAvailability getReadWriteType(T dataPointResource, String propertyName);
}