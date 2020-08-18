package org.ogema.simpleresource.util;

import java.util.List;

public interface ResourceBase {
	String getName();
	String getLocation();
	/** Path equals location*/
	String getPath();
	ResourceBase getParent();
	List<ResourceBase> getSubResources();
	
	/** TODO: May be interface extension if resource structure cannot be changed via the interface*/
	void addElement(String newSubResourceName, ResourceBase newSubResource);
	ResourceBase removeElement(String subResourceName);
}
