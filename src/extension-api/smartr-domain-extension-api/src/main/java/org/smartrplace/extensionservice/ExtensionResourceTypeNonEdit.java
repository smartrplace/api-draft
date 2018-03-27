package org.smartrplace.extensionservice;

import org.ogema.core.model.simple.StringResource;
import org.ogema.model.prototypes.Data;

public interface ExtensionResourceTypeNonEdit extends Data {
	/** User name with OGEMA login*/
	StringResource ogemaUserName();
	/** Overwrite this with the respectie editable resource type*/
	ExtensionResourceType editableData();
}
