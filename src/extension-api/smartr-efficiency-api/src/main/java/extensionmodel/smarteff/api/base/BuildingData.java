package extensionmodel.smarteff.api.base;

import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;

public interface BuildingData extends SmartEffExtensionResourceType {
	/** 1: single family home (SFH)<br>
	 *  2: multi family home< (MFH)br>
	 *  3: apartment<br>
	 *  4: office building<br>
	 *  5: other commercial building<br>
	 *  10: school<br>
	 *  11: gym<br>
	 *  12: lecture hall, conference center, theater etc.<br>
	 *  20: other
	 */
	IntegerResource typeOfBuilding();
	
	/** 1: owner<br>
	 *  2: tenant<br>
	 *  3: property manager<br>
	 */
	IntegerResource typeOfUser();
	
	/** Only shown when unit type is not SFH*/
	IntegerResource numberOfUnitsInBuilding();
	
	/**Number of rooms in building or unit*/
	IntegerResource roomNum();

	
	/**Usually only country and postal code is used*/
	LocationExtended address();
	
	FloatResource heatedLivingSpace();
	
	/** Only to be provided if different from current building heat source
	 * 1: L gas
	 * 1: H gas
	 * 10: oil
	 * 11: charcoal
	 * 12: lignite
	 * 13: wood pellets
	 * 14: dry wood
	 * 20: district heating
	 * 21: building-internal heat meter
	 * 30: heat pump
	 * 31: night storage heating
	 * 22: direct electric heating
	 */
	IntegerResource heatSource();

	ResourceList<HeatCostBillingInfo> billingInfo();
	
	//TODO: add further elements
}