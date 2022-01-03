package org.ogema.devicefinder.api;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.devicefinder.api.OGEMADriverPropertyService.AccessAvailability;
import org.ogema.internationalization.util.LocaleHelper;
import org.ogema.model.prototypes.PhysicalElement;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.template.LabelledItem;

/** This is kind of an extended {@link GaRoDataType} generic description for datapoints
 * including special parameters/properties<br>
 * For properties see also [Occu JS Code](https://github.com/eq-3/occu/blob/8cb51174c2bc8c4b33df50a96b82c90e8092f79c/WebUI/www/config/easymodes/MASTER_LANG/HEATINGTHERMOSTATE_2ND_GEN.js)
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
	 *  2: PARTY-MODE
	 *  3: BOOST-MODE
	 */
	public static final PropType THERMOSTAT_OPERATION_MODE = new PropType("ThermostatOperationMode", Arrays.asList(
			new PropUsage[] {PropUsage.THERMOSTAT}), AccessAvailability.WRITE,
			Integer.class);
	
	/** 
	 *  0 : Inaktiv
     *  1 : Auto-Modus (default)
     *  2 : Auto + Manu-Modus
     *  3 : Auto + Party-Modus
     *  4 : Aktiv
	 */
	public static final PropType THERMOSTAT_WINDOWOPEN_MODE = new PropType("ThermostatWindowOpenMode", Arrays.asList(
			new PropUsage[] {PropUsage.THERMOSTAT}), AccessAvailability.WRITE,
			Integer.class);

	/** In Celsius*/
	public static final PropType THERMOSTAT_WINDOWOPEN_TEMPERATURE = new PropType("ThermostatWindowOpenTemperature", Arrays.asList(
			new PropUsage[] {PropUsage.THERMOSTAT}), AccessAvailability.WRITE,
			Float.class);

	/** integer in minutes*/
	public static final PropType THERMOSTAT_WINDOWOPEN_MINUTES = new PropType("ThermostatWindowOpenMode", Arrays.asList(
			new PropUsage[] {PropUsage.THERMOSTAT}), AccessAvailability.WRITE,
			Integer.class);

	/** integer in minutes*/
	public static final PropType THERMOSTAT_BOOST_MINUTES = new PropType("ThermostatBoostMinutes", Arrays.asList(
			new PropUsage[] {PropUsage.THERMOSTAT}), AccessAvailability.WRITE,
			Integer.class);

	/** valve position in per cent (0..100)*/
	public static final PropType THERMOSTAT_BOOST_POSITION= new PropType("ThermostatBoostPosition", Arrays.asList(
			new PropUsage[] {PropUsage.THERMOSTAT}), AccessAvailability.WRITE,
			Integer.class);

	public static final PropType BUTTON_LOCK = new PropType("ButtonLock", Arrays.asList(
			new PropUsage[] {PropUsage.HOMEMATIC}), AccessAvailability.WRITE,
			Boolean.class);

	/** maximum valve position, e.g. for hydraulic levelling, 0...100*/
	public static final PropType THERMOSTAT_VALVE_MAXPOSITION = new PropType("ThermostatValveMaxposition", Arrays.asList(
			new PropUsage[] {PropUsage.THERMOSTAT}), AccessAvailability.WRITE,
			Integer.class);

	public static final PropType CURRENT_SENSOR_VALUE = new PropType("CurrentSensorValue", Arrays.asList(
			new PropUsage[] {PropUsage.HOMEMATIC}), AccessAvailability.READ,
			Float.class);

	public static final PropType DEVICE_ERROR = new PropType("DeviceError", Arrays.asList(
			new PropUsage[] {PropUsage.HOMEMATIC}), AccessAvailability.READ,
			Integer.class);

	public static final PropType TRANSMIT_TRY_MAX = new PropType("TransmitTryMax", Arrays.asList(
			new PropUsage[] {PropUsage.HOMEMATIC}), AccessAvailability.WRITE,
			Integer.class);

	public static final PropType EXPECT_AES = new PropType("ExpectAES", Arrays.asList(
			new PropUsage[] {PropUsage.HOMEMATIC}), AccessAvailability.WRITE,
			Boolean.class);

	public static final PropType PEER_NEEDS_BURST = new PropType("PeerNeedsBurst", Arrays.asList(
			new PropUsage[] {PropUsage.HOMEMATIC}), AccessAvailability.WRITE,
			Boolean.class);

	public static final PropType BURST_RX = new PropType("BurstRx", Arrays.asList(
			new PropUsage[] {PropUsage.HOMEMATIC}), AccessAvailability.WRITE,
			Boolean.class);

	public static final PropType LOCAL_RESET_DISABLE = new PropType("LocalResetDisable", Arrays.asList(
			new PropUsage[] {PropUsage.HOMEMATIC}), AccessAvailability.WRITE,
			Boolean.class);

	public static final PropType TRANSMIT_DEV_TRY_MAX = new PropType("TransmitDevTryMax", Arrays.asList(
			new PropUsage[] {PropUsage.HOMEMATIC}), AccessAvailability.WRITE,
			Integer.class);

	public static final List<PropType> STD_PROPS = Arrays.asList(new PropType[] {ENCRYPTION_ENABLED, THERMOSTAT_OPERATION_MODE,
			CURRENT_SENSOR_VALUE, DEVICE_ERROR, TRANSMIT_TRY_MAX, TRANSMIT_DEV_TRY_MAX, EXPECT_AES,
			PEER_NEEDS_BURST, LOCAL_RESET_DISABLE});

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
	
	@SuppressWarnings("unchecked")
	public static ResourceList<SingleValueResource> getHmParamMaster(PhysicalElement hmDevice) {
		return hmDevice.getSubResource("HmParametersMaster", ResourceList.class);
	}
	
	public static SingleValueResource getHmParam(PhysicalElement hmDevice, PropType type, boolean feedback) {
		ResourceList<SingleValueResource> master = getHmParamMaster(hmDevice);
		if(type == PropType.THERMOSTAT_WINDOWOPEN_MINUTES)
			return getSubInt(master, "TEMPERATUREFALL_WINDOW_OPEN_TIME_PERIOD", feedback);
		else if(type == PropType.THERMOSTAT_WINDOWOPEN_MODE)
			return getSubInt(master, "TEMPERATUREFALL_MODUS", feedback);
		else if(type == PropType.THERMOSTAT_WINDOWOPEN_TEMPERATURE)
			return getSubFloat(master, "TEMPERATURE_WINDOW_OPEN", feedback);
		else if(type == PropType.THERMOSTAT_VALVE_MAXPOSITION)
			return getSubFloat(master, "VALVE_MAXIMUM_POSITION", feedback);
		else if(type == PropType.BUTTON_LOCK)
			return ResourceHelper.getSubResourceOfSibbling(hmDevice,
					"org.ogema.drivers.homematic.xmlrpc.hl.types.HmMaintenance", "globalButtonLock", BooleanResource.class);
		else
			throw new IllegalStateException("PropType not supported as resource:"+type.id);
	}

	public static final List<PropType> PROPS_RESOURCES = Arrays.asList(new PropType[] {
			PropType.THERMOSTAT_WINDOWOPEN_MODE, PropType.THERMOSTAT_WINDOWOPEN_TEMPERATURE, PropType.THERMOSTAT_WINDOWOPEN_MINUTES,
			PropType.THERMOSTAT_VALVE_MAXPOSITION});

	public static long[] lastHmParamUpdate(PhysicalElement hmDevice) {
		long first = Long.MAX_VALUE;
		long last = 0;
		boolean found = false;
		for(PropType prop: PROPS_RESOURCES) {
			SingleValueResource res = getHmParam(hmDevice, prop, true);
			if(res != null && res.exists()) {
				long lw = res.getLastUpdateTime();
				if(lw < first)
					first = lw;
				if(lw > last)
					last = lw;
				found = true;
			}
		}
		if(!found)
			return new long[] {-1, -1};
		return new long[] {first, last};
	}
	
	public static boolean triggerHmUpdate(PhysicalElement hmDevice) {
		ResourceList<SingleValueResource> master = getHmParamMaster(hmDevice);
		BooleanResource update = master.getSubResource("update", BooleanResource.class);
		return update.setValue(true);
	}
	
	protected static SingleValueResource getSubInt(ResourceList<SingleValueResource> master, String baseName, boolean feedback) {
		return master.getSubResource(baseName+(feedback?"_FEEDBACK":""), IntegerResource.class);
	}
	
	protected static SingleValueResource getSubFloat(ResourceList<SingleValueResource> master, String baseName, boolean feedback) {
		return master.getSubResource(baseName+(feedback?"_FEEDBACK":""), FloatResource.class);
	}
}
