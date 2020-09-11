package org.smartrplace.apps.hw.install.prop;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.array.StringArrayResource;

import de.iwes.util.resource.ResourceHelper;

public class DriverPropertyUtils {
	public static final String RES_NAMES = "propertyNames";	
	public static final String RES_VALUES = "propertyValues";	
	
	public static StringArrayResource[] getPropertyResources(Resource parent, boolean createIfNotExisting) {
		if(createIfNotExisting) {
			StringArrayResource names = parent.getSubResource(RES_NAMES, StringArrayResource.class);
			if(!names.isActive()) {
				names.create().activate(false);
			}
			StringArrayResource values = parent.getSubResource(RES_VALUES, StringArrayResource.class);
			if(!values.isActive()) {
				values.create().activate(false);
			}
			return new StringArrayResource[] {names, values};
		}
		StringArrayResource names = ResourceHelper.getSubResourceIfExisting(parent, RES_NAMES,
				StringArrayResource.class);
		StringArrayResource values = ResourceHelper.getSubResourceIfExisting(parent, RES_VALUES,
				StringArrayResource.class);
		if(names != null && values != null)
			return new StringArrayResource[] {names, values};
		return null;
	}
	
	/** Read property. Note that this does NOT trigger an updateProperty, so no reading on hardware is
	 * triggered
	 * 
	 * @param parent
	 * @param propertyName
	 * @return property value or null if no properties are available for the parent or no property with
	 * 		the name has been saved.
	 */
	public static String getPropertyValue(Resource parent, String propertyName) {
		StringArrayResource[] props = getPropertyResources(parent, false);
		if(props == null)
			return null;
		int idx = 0;
		for(String name: props[0].getValues()) {
			if(propertyName.equals(name)) {
				return props[1].getElementValue(idx);
			}
			idx++;
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Resource> List<T> getResourcesWithProperties(ApplicationManager appMan, Class<T> resourceType) {
		List<T> result = new ArrayList<>();
		List<StringArrayResource> arrays = appMan.getResourceAccess().getResources(StringArrayResource.class);
		for(StringArrayResource arr: arrays) {
			if(!arr.getName().equals(RES_NAMES))
				continue;
			Resource parent = arr.getParent();
			if(parent == null || (!resourceType.isAssignableFrom(parent.getResourceType())))
				continue;
			StringArrayResource values = parent.getSubResource(RES_VALUES, StringArrayResource.class);
			if(!values.isActive()) {
				continue;
			}
			result.add((T) parent);
		}
		return result ;
	}
}
