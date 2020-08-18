package org.ogema.simpleresource.util;

import org.ogema.core.model.simple.SingleValueResource;

public interface SingleValueResourceBase extends ResourceBase {
	/** Get correspoding resource type, e.g. TemperatureResource or BooleanResource*/
	SingleValueResource getValueResourceType();
}
