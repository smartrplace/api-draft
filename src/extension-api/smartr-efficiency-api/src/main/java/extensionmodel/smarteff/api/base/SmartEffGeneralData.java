package extensionmodel.smarteff.api.base;

import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;

/** Data that is accessible for all users.*/
public interface SmartEffGeneralData extends SmartEffExtensionResourceType {
	SmartEffPriceData defaultPriceData();
	BuildingData defaultBuildingData();
}
