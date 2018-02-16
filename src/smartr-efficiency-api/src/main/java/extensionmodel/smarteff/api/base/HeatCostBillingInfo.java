package extensionmodel.smarteff.api.base;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.TimeResource;
import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;

public interface HeatCostBillingInfo extends SmartEffExtensionResourceType {
	/** 1: gas in m3
	 *  2: oil in l
	 *  3: solid fuel in kg
	 *  3: kWh (input, for heat pump this would be electricity)
	 *  4: kWh heat measured for heat pump
	 */
	IntegerResource unit();
	
	/** Only to be provided if different from current building heat source
	 * see {@link BuildingData#heatSource()}
	 */
	IntegerResource heatSource();

	TimeResource beginningOfBillingPeriod();
	TimeResource endOfBillingPeriod();
	FloatResource billedConsumption();
}
