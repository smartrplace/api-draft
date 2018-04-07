package extensionmodel.smarteff.api.base;

import org.smartrplace.efficiency.api.base.SmartEffResource;

/** Data that is accessible for all users.*/
public interface SmartEffGeneralData extends SmartEffResource {
	SmartEffPriceData defaultPriceData();
	BuildingData defaultBuildingData();
}
