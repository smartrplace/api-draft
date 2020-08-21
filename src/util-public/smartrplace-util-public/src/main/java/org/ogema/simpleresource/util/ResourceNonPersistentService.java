package org.ogema.simpleresource.util;

import java.util.List;

/** This is an initial draft and not implemented yet*/
public interface ResourceNonPersistentService {
	List<ResourceBase> getToplevelResources();
	ResourceBase getResource(String location);
		
	/** TODO: May be interface extension if resource structure cannot be changed via the interface*/
	void addResource(ResourceBase resource, String location);
	ResourceBase removeResource(String location);
}
