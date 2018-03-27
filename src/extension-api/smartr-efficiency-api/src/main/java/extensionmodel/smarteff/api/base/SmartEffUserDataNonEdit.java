package extensionmodel.smarteff.api.base;

import org.smartrplace.extensionservice.ExtensionResourceTypeNonEdit;

public interface SmartEffUserDataNonEdit extends ExtensionResourceTypeNonEdit {
	@Override
	SmartEffUserData editableData();
}
