package org.ogema.timeseries.eval.simple.mon;

import java.util.List;

import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;

public interface TimeseriesSetProcessorArg<T> extends TimeseriesSetProcessor {
	@Override
	default List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService) {
		throw new UnsupportedOperationException("Only supports getResultSeries with Argument Object");
	}
	List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService, T params);
	
	Class<T> getParamClass();
}
