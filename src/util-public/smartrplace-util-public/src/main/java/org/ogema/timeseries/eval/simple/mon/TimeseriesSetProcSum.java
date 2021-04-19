package org.ogema.timeseries.eval.simple.mon;

import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;

public class TimeseriesSetProcSum extends TimeseriesSetProcMultiToSingle {
	
	public TimeseriesSetProcSum(String label) {
		super(label);
	}
	public TimeseriesSetProcSum(String label, int intervalType) {
		super(label, intervalType);
	}
	public TimeseriesSetProcSum(String label, int intervalType, Integer absoluteTiming) {
		super(label, intervalType, absoluteTiming);
	}
	public TimeseriesSetProcSum(String label, int intervalType, Integer absoluteTiming, Long minIntervalForReCalc) {
		super(label, intervalType, absoluteTiming, minIntervalForReCalc);
	}

	@Override
	protected float aggregateValues(Float[] values, long timestamp, AggregationMode mode) {
		float result = 0;
		for(Float val: values) {
			if((val != null) && (!Float.isNaN(val)))
				result += val;
		}
		return result;
	}
	
	@Override
	protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {}

}
