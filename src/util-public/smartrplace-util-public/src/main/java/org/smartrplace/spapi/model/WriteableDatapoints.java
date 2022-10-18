package org.smartrplace.spapi.model;

import org.ogema.core.model.ResourceList;
import org.ogema.model.prototypes.Data;

public interface WriteableDatapoints extends Data {
	ResourceList<WriteableDatapoint> datapoints();
}
