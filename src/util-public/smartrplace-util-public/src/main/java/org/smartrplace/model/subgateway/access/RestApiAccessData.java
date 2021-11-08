package org.smartrplace.model.subgateway.access;

import org.ogema.core.model.ResourceList;
import org.ogema.model.prototypes.Data;

@Deprecated
public interface RestApiAccessData extends Data {
	ResourceList<RestApiAccessGateway> gatewayData();
}
