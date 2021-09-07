package org.ogema.timeseries.eval.simple.api;

import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;

public interface TimeseriesUpdateListener {
	void updated(List<SampledValue> newVals);
}
