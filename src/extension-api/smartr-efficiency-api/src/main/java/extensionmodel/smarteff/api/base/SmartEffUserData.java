package extensionmodel.smarteff.api.base;

import org.ogema.core.model.ResourceList;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ExtensionUserData;

public interface SmartEffUserData extends SmartEffResource, ExtensionUserData {
	ResourceList<BuildingData> buildingData();
}
