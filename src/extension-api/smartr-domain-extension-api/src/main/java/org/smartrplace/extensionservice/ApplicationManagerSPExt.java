package org.smartrplace.extensionservice;

import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.Resource;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;

public interface ApplicationManagerSPExt extends ApplicationManagerMinimal {
	public Resource generalData();
	
	/**Get type declaration from extension resource type*/
	public <T extends Resource> ExtensionResourceTypeDeclaration<T> getTypeDeclaration(Class<T> resourceType);

	public OgemaLogger log();
}
