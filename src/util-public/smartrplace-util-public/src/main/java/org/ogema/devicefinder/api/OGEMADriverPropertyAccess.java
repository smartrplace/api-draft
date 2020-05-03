package org.ogema.devicefinder.api;

import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.OGEMADriverPropertyService.AccessAvailability;
import org.ogema.model.prototypes.PhysicalElement;

public interface OGEMADriverPropertyAccess {
	
	/** Request that a certain property shall be updated	 * 
	 * @param propertyId id of property as listed in a StringArrayResource named propertyNames below
	 * 		the dataPointResource. The values shall be in propertyValues 
	 */
	public void updateProperty(String propertyId);
	
	/** Request that a certain property shall be written
	 * @param propertyId see {@link #updateProperty(PhysicalElement, String, OgemaLogger)}
	 * @param value to write. If the value cannot be converted to the destination format a NumberFormatException
	 *  		or a NullPointerException shall be thrown
	 */
	public void writeProperty(String propertyId, String value);

	/** Request that the StringArrayResources named propertyNames and propertyValues are created and filled
	 * with all available values. If the resources exists already they shall be updated
	 */
	public void updateProperties();
	
	/** Get base resource type of dataPointResurce. If different types are used return the
	 * most basic common type. It may be better to offer seperate services for very different
	 * dataPointResource types.
	 * @return resource type
	 */
	public Class<? extends Resource> getDataPointResourceType();
	
	/** Get access characteristics of a property
	 * 
	 * @param propertyId see {@link #updateProperty(PhysicalElement, String, OgemaLogger)}
	 * @return
	 */
	public AccessAvailability getReadWriteType(String propertyName);
}