package org.ogema.devicefinder.api;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.devicefinder.api.OGEMADriverPropertyService.AccessAvailability;
import org.ogema.internationalization.util.LocaleHelper;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.template.LabelledItem;

/** This is kind of an extended {@link GaRoDataType} generic description for datapoints
 * including special parameters/properties
 * 
 * @author dnestle
 *
 */
public class PropType implements LabelledItem {
	public static final PropType ENCRYPTION_ENABLED = new PropType("EncryptionEnabled", Arrays.asList(
			new PropUsage[] {PropUsage.HOMEMATIC}), AccessAvailability.WRITE,
			Boolean.class);
	/** 0: Auto
	 *  1: Manual
	 *  ?: Boost
	 */
	public static final PropType THERMOSTAT_OPERATION_MODE = new PropType("ThermostatOperationMode", Arrays.asList(
			new PropUsage[] {PropUsage.THERMOSTAT}), AccessAvailability.WRITE,
			Integer.class);
	
	public static final List<PropType> STD_PROPS = Arrays.asList(new PropType[] {ENCRYPTION_ENABLED, THERMOSTAT_OPERATION_MODE});
	public static enum PropUsage {
		THERMOSTAT,
		TEMPERATURE_SENSOR,
		HUMIDITY_SENSOR,
		HOMEMATIC,
		BACNET,
		WMBUS
		//TODO: Extend this on demand
	};

	public static enum PropAccessLevel {
		DATAPOINT,
		DEVICE,
		ROOM
	}
	
    public final Map<OgemaLocale, String> description;
    public final String id;
    /** Relevant status code */
    public final List<PropUsage> relevantDevices;
    public final AccessAvailability access;
    /** Usually either a Number class or String*/
    public final Class<?> type;
    /** Access level*/
    public final PropAccessLevel accessLevel;
    public final GaRoDataType gaRoType;
    
    PropType(String id) {
    	this(id, AccessAvailability.READ);
    }
    PropType(String id, AccessAvailability access) {
    	this(id, null, access, null);
    }
    PropType(String id, List<PropUsage> relevantDevices, AccessAvailability access) {
    	this(id, relevantDevices, access, Float.class, (Map<OgemaLocale, String>)null,
    			PropAccessLevel.DEVICE);
    }
    PropType(String id, List<PropUsage> relevantDevices, AccessAvailability access, Class<?> type) {
    	this(id, relevantDevices, access, type, (Map<OgemaLocale, String>)null,
    			PropAccessLevel.DEVICE);
    }
    PropType(String id, List<PropUsage> relevantDevices, AccessAvailability access,
    		Class<?> type, String description) {
    	this(id, relevantDevices, access, type, LocaleHelper.getLabelMap(description),
    			PropAccessLevel.DEVICE);
    }
    PropType(String id, List<PropUsage> relevantDevices, AccessAvailability access,
    		Class<?> type, Map<OgemaLocale, String> description,
    		PropAccessLevel accessLevel) {
    	this.id = id;
    	if(description == null)
    		this.description = LocaleHelper.getLabelMap(id);
    	else
    		this.description = description;
    	this.access = access;
    	if(!(Number.class.isAssignableFrom(type) || Boolean.class.isAssignableFrom(type) || String.class.isAssignableFrom(type)))
    		throw new IllegalStateException("Only Number, Boolean and String supported, found"+type);
    	this.type = type;
    	this.relevantDevices = relevantDevices;
    	this.accessLevel = accessLevel;
    	this.gaRoType = null;
    }
    
    /**
     * 
     * @param gType
     * @param relevantDevices should be defined for {@link GaRoDataType} in the future directly, then argument would not be required anymore
     * @param access should be defined for {@link GaRoDataType} in the future directly, then argument would not be required anymore
     * @param description should be defined for {@link GaRoDataType} in the future directly, then argument would not be required anymore
     * @param accessLevel should be defined for {@link GaRoDataType} in the future directly, then argument would not be required anymore
     */
    PropType(GaRoDataType gType, List<PropUsage> relevantDevices, AccessAvailability access,
    		Map<OgemaLocale, String> description,
    		PropAccessLevel accessLevel) {
    	this.id =gType.id();
    	if(description == null)
    		//TODO: We should copy all locale labels here
    		this.description = LocaleHelper.getLabelMap(gType.description(null));
    	else
    		this.description = description;
    	this.access = access;
    	if(FloatResource.class.isAssignableFrom(gType.representingResourceType())) {
        	this.type = Float.class;    		
    	} else if(IntegerResource.class.isAssignableFrom(gType.representingResourceType())) {
        	this.type = Integer.class;    		
    	} else if(BooleanResource.class.isAssignableFrom(gType.representingResourceType())) {
        	this.type = Boolean.class;    		
    	} else if(TimeResource.class.isAssignableFrom(gType.representingResourceType())) {
        	this.type = Long.class;    		
    	} else if(StringResource.class.isAssignableFrom(gType.representingResourceType())) {
        	this.type = String.class;    		
    	} else
    		throw new IllegalStateException("Unsupported GaRo Type in:"+gType.id()+" : "+gType.representingResourceType());
    	this.relevantDevices = relevantDevices;
    	this.accessLevel = accessLevel;
    	this.gaRoType = null;
     }

    
	@Override
	public String id() {
		return id;
	}
	@Override
	public String label(OgemaLocale locale) {
		return LocaleHelper.getLabel(description, locale);
	}
}
