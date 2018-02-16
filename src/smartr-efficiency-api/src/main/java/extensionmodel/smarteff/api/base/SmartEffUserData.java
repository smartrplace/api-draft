package extensionmodel.smarteff.api.base;

import org.ogema.core.model.ResourceList;
import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;

public interface SmartEffUserData extends SmartEffExtensionResourceType {
	ResourceList<BuildingData> buildings();
}
