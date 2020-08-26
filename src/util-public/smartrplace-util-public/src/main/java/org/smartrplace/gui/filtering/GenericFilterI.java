package org.smartrplace.gui.filtering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public interface GenericFilterI<A> {
	boolean isInSelection(A object, OgemaHttpRequest req);
	
	/** Overwrite this to improve efficiency, the behaviour should not change*/
	default List<A> getFiltered(Collection<A> result2, OgemaHttpRequest req) {
		List<A> result = new ArrayList<>();
		for(A obj: result2) {
			if(isInSelection(obj, req))
				result.add(obj);
		}
		return result;
	}
}
