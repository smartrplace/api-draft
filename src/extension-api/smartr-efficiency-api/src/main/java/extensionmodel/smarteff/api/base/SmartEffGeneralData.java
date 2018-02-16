package extensionmodel.smarteff.api.base;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;

/** Data that is accessible for all users.*/
public interface SmartEffGeneralData extends SmartEffExtensionResourceType {
	FloatResource defaultGasPricePerkWh();
	FloatResource defaultGasPriceBase();
	FloatResource defaultOilPricePerkWh();
	FloatResource defaultOilPriceBase();
	FloatResource defaultElectrictiyPricePerkWh();
	FloatResource defaultElectrictiyPriceBase();
	
	FloatResource defaultYearlyInterestRate();

	IntegerResource standardRoomNum();
}
