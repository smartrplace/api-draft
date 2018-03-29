package org.smartrplace.extensionservice;

import java.util.Collection;

import org.ogema.core.application.Application.AppStopReason;

/** To be registered as OSGi service by the extension module. Replacement for the Application service
 * registered by normal OGEMA applications.
 * @param <T> base ExtensionResouceType of the domain which is extended here. So this should be the same
 * for all extension modules of the domain
 */
public interface ExtensionService<T extends ExtensionResourceType>  {
	/**Id of service. If null the full class name shall be used as id*/
	default String id() {
		return null;
	}
	
	void start(ApplicationManagerSPExt appManExt);
	void stop(AppStopReason reason);
	
	/**Each object in the list should only implement a single inherited interface of ExtensionCapability*/
	Collection<ExtensionCapability> getCapabilities();
	
	/**Declaration of resource types defined by the extension module*/
	Collection<ExtensionResourceTypeDeclaration<? extends T>> resourcesDefined();
}
