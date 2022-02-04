package org.smartrplace.apps.heatcontrol.extensionapi;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.transaction.ResourceTransaction;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.TemperatureSensor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.util.virtualdevice.HmCentralManager;
import org.smartrplace.util.virtualdevice.SetpointControlManager;
import org.smartrplace.util.virtualdevice.SetpointControlManager.SetpointControlType;
import org.smartrplace.util.virtualdevice.ThermostatAirconDefaultManager;

public class ThermostatPattern extends ResourcePattern<Thermostat> { 
	
	public final TemperatureResource setPoint = model.temperatureSensor().settings().setpoint();

	public final Room room = model.location().room();
	
	@Existence(required = CreateMode.OPTIONAL)
	public final TemperatureSensor tempSensor = model.temperatureSensor();

	@Existence(required = CreateMode.OPTIONAL)
	public final TemperatureResource tempSens = model.temperatureSensor().reading();
	
	@Existence(required = CreateMode.OPTIONAL) 
	public final TemperatureResource setPointFB = model.temperatureSensor().deviceFeedback().setpoint();
	
	/**
	 * Values: 
	 * <ul>
	 * 	<li>0: inactive
	 *  <li>1: Auto mode
	 *  <li>2: Auto + Manu mode
	 *  <li>3: Auto + Party mode
	 *  <li>4: active
	 * </ul>
	 */
	@Existence(required = CreateMode.OPTIONAL) 
	public final IntegerResource windowRecognitionMode = model.getSubResource("HmParametersMaster", ResourceList.class)
																.getSubResource("TEMPERATUREFALL_MODUS", IntegerResource.class);
	
	/**
	 * If the thermostat is a Homematic device, an association will be created at Homematic level
	 */
	@Existence(required = CreateMode.OPTIONAL)
	public final TemperatureSensor associatedTempSensor = model.getSubResource("linkedTempSens", TemperatureSensor.class);
	
	// FIXME avoid non-resource fields in pattern
	// FIXME this is a room property, not a thermostat property -> the thermostat location may change indeed
//	public RoomTemperatureSetting rts;

	@Existence(required = CreateMode.OPTIONAL)
	public	IntegerResource setManualMode = model.getSubResource("controlMode", IntegerResource.class);

	/** keep feedbackValues and setPoint values from last 20 seconds to determine values
	 * not relevant for manual setting*/
	//public List<KnownValue> knownValues = new ArrayList<>();
	public float currentSetpointBeforeOffset;
	public ResourceValueListener<TemperatureResource> feedbackListener = null;
	public boolean receivedFirstFBValue = false;
	public Boolean lastBangBangState = null;
	
	/**
	 * Constructor for the access pattern. This constructor is invoked by the framework. Must be public
	 */
	public ThermostatPattern(Resource device) {
		super(device);
	}
	
	/**
	 * Custom acceptance check
	 */
//	@Override
//	public boolean accept() {
//		Room room = RoomHelper.getResourceLocationRoom(model);
//		if(room == null) return false;
//		
//		return true;
//	}
	
	public boolean isHomematic() {
		Resource parent = ResourceUtils.getParentLevelsAbove(model, 3);
		return (parent != null && parent.getResourceType().getSimpleName().equals("HmLogicInterface"));
	}
	
	public boolean deactivateHomematicWindowOpenRecognition(ResourceAccess ra) {
		if (!isHomematic())
			return false;
		int mode;
		if (windowRecognitionMode.isActive()) {
			mode = windowRecognitionMode.getValue();
			if (mode == 0 || mode == 1)
				return false;
		}
		ResourceTransaction trans = ra.createResourceTransaction();
		trans.create(windowRecognitionMode);
		trans.activate(windowRecognitionMode);
		trans.activate(windowRecognitionMode.getParent());
		// remains active in auto mode
		trans.setInteger(windowRecognitionMode, 1);
		trans.commit();
		windowRecognitionMode.<ResourceList<?>> getParent().setElementType(SingleValueResource.class);
		return true;
	}
	
	private SetpointControlManager<TemperatureResource> setpMan = null;
	public SetpointControlManager<TemperatureResource> getSetpMan() {
		return setpMan;
	}
	public SetpointControlManager<TemperatureResource> getSetpMan(ApplicationManagerPlus appManPlus) {
		if(setpMan == null) {
			SetpointControlType type = SetpointControlManager.getControlType(setPoint);
			if(type == SetpointControlType.HmThermostat)
				setpMan = HmCentralManager.getInstance(appManPlus);
			else
				setpMan = ThermostatAirconDefaultManager.getInstance(appManPlus);
		}
		return setpMan;
	}
}
