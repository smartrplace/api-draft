package org.smartrplace.extensionservice;

import org.ogema.core.logging.OgemaLogger;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;

public interface ApplicationManagerSPExt extends ApplicationManagerMinimal {
	public ExtensionResourceType generalData();
	
	/**Get type declaration from extension resource type*/
	public <T extends ExtensionResourceType> ExtensionResourceTypeDeclaration<T> getTypeDeclaration(Class<T> resourceType);

	public OgemaLogger log();
}
