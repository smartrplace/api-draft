package org.ogema.devicefinder.api;

import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.devicefinder.util.DeviceHandlerBase.PropAccessDataHm;
import org.smartrplace.apps.hw.install.prop.DriverPropertyUtils;

public interface PropertyService {
	
	/** Get semantic property value
	 * 
	 * @param devDataResource this typically is a device resource or a SingleValueResource representing
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
	default Float getPropertyValue(Resource devDataResource, PropType propType,
			DriverPropertySuccessHandler<?> successHandler,
			String... arguments) {
		if(propType.type == String.class)
			return null;
		String sval = getProperty(devDataResource, propType, successHandler, arguments);
		if(sval == null)
			return null;
		return Float.parseFloat(sval);		
	};
	
	/** Read from the StringArrayResource values relevant for a semantic property*/
	default String getPropertyCached(Resource devDataResource, PropType propType) {
		PropAccessDataHm access = getPropAccess(devDataResource, propType);
		if(access == null)
			return null;
		return DriverPropertyUtils.getPropertyValue(access.anchorRes, access.propId);
	}
	
	/** This method shall trigger a write operation to the hardware independently whether
	 * a successHandler is provided or not
	 * @param devDataResource
	 * @param propType
	 * @param value
	 * @param successHandler
	 * @param argument
	 */
	default void setPropertyValue(Resource devDataResource, PropType propType, float value,
			DriverPropertySuccessHandler<?> successHandler,
			String... argument) {
		if(propType.type == String.class)
			return;
		String sval = ""+value;
		setProperty(devDataResource, propType, sval, successHandler, argument);		
	}
	
	/** Get semantic property value<br> 
	 * See {@link #getPropertyValue(Resource, PropType, DriverPropertySuccessHandler, String...)}
	 * This method should be callable on all properties including value and String properties
	 * @param devDataResource
	 * @param propType
	 * @param successHandler
	 * @param arguments
	 * @return
	 */
	String getProperty(Resource devDataResource, PropType propType,
			DriverPropertySuccessHandler<?> successHandler,
			String... arguments);
	void setProperty(Resource devDataResource, PropType propType, String value,
			DriverPropertySuccessHandler<?> successHandler,
			String... argument);
	
	/** Provide access data for reading from StringArrayResource values*/
	PropAccessDataHm getPropAccess(Resource devDataResource, PropType propType);
	
	/** Get all semantic properties suppported*/
	List<PropType> getPropTypesSupported();
}
