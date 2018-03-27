package extensionmodel.smarteff.api.base;

import org.ogema.core.model.ResourceList;
import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;
import org.smartrplace.extensionservice.ExtensionUserData;

public interface SmartEffUserData extends SmartEffExtensionResourceType, ExtensionUserData {
	ResourceList<BuildingData> buildings();
}
