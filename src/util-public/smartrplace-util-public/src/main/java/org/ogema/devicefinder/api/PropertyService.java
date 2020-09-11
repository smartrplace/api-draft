package org.ogema.devicefinder.api;

import org.ogema.core.model.Resource;

public interface PropertyService {
	
	/** Get semantic property value
	 * 
	 * @param anchorResource this typically is a device resource or a SingleValueResource representing
	 * 		a datapoint for which a property is provided. The property must fit {@link PropType#accessLevel}
	 * @param propType The semantic property type requested
	 * @param successHandler If the property reading from the hardware involves a blocking process
	 * 		then just the property value from the respective propertyValues StringArrayResource is returned.
	 * 		If no such value is available then null is returned. If null no read operation to the hardware is
	 * 		started, otherwise a read operation is started. The property can be read once more when the listner
	 * 		is called.<br>
	 * 		For non-blocking readings the return value does not depend whether a successHandler is provided.
	 * @param arguments a list of String arguments which is only relevant if such argument(s) are specified
	 * 		for the semantic property type.
	 * @return see successHandler
	 */
	default Float getPropertyValue(Resource anchorResource, PropType propType,
			DriverPropertySuccessHandler<?> successHandler,
			String... arguments) {
		if(propType.type == String.class)
			return null;
		String sval = getProperty(anchorResource, propType, successHandler, arguments);
		if(sval == null)
			return null;
		return Float.parseFloat(sval);		
	};
	
	/** This method shall trigger a write operation to the hardware independently whether
	 * a successHandler is provided or not
	 * @param anchorResource
	 * @param propType
	 * @param value
	 * @param successHandler
	 * @param argument
	 */
	default void setPropertyValue(Resource anchorResource, PropType propType, float value,
			DriverPropertySuccessHandler<?> successHandler,
			String... argument) {
		if(propType.type == String.class)
			return;
		String sval = ""+value;
		setProperty(anchorResource, propType, sval, successHandler, argument);		
	}
	
	/** See {@link #getPropertyValue(Resource, PropType, DriverPropertySuccessHandler, String...)}
	 * This method should be callable on all properties including value and String properties
	 * @param anchorResource
	 * @param propType
	 * @param successHandler
	 * @param arguments
	 * @return
	 */
	String getProperty(Resource anchorResource, PropType propType,
			DriverPropertySuccessHandler<?> successHandler,
			String... arguments);
	void setProperty(Resource anchorResource, PropType propType, String value,
			DriverPropertySuccessHandler<?> successHandler,
			String... argument);
}
