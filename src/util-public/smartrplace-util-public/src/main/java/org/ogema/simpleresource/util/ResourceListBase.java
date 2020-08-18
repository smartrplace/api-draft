package org.ogema.simpleresource.util;

import java.util.List;

public interface ResourceListBase<T extends ResourceBase> extends ResourceBase {
	int size();
	boolean isEmpty();
	List<T> getAllElements();
	T get(int index);
	
	/** TODO: May be interface extension if resource structure cannot be changed via the interface*/
	T remove(int index);
	T add();
}
