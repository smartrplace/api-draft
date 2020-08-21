package org.ogema.simpleresource.util;

import org.ogema.core.model.simple.SingleValueResource;

/** This is an initial draft and not implemented yet*/
public interface SingleValueResourceBase extends ResourceBase {
	/** Get correspoding resource type, e.g. TemperatureResource or BooleanResource*/
	SingleValueResource getValueResourceType();
}
