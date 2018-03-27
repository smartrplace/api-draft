package extensionmodel.smarteff.api.base;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;

/** Data that is accessible for all users.*/
public interface SmartEffPriceData extends SmartEffExtensionResourceType {
	FloatResource gasPricePerkWh();
	FloatResource gasPriceBase();
	FloatResource oilPricePerkWh();
	FloatResource oilPriceBase();
	FloatResource electrictiyPricePerkWh();
	FloatResource electrictiyPriceBase();
	
	FloatResource yearlyInterestRate();

	IntegerResource standardRoomNum();
}
