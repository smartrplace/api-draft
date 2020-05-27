package org.ogema.devicefinder.util;

import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;

public interface AggregationModeProvider {
	AggregationMode getMode(String tsId);
}
