package org.ogema.simpleresource.util;

import java.util.List;

public interface ResourceNonPersistentService {
	List<ResourceBase> getToplevelResources();
	ResourceBase getResource(String location);
		
	/** TODO: May be interface extension if resource structure cannot be changed via the interface*/
	void addResource(ResourceBase resource, String location);
	ResourceBase removeResource(String location);
}
