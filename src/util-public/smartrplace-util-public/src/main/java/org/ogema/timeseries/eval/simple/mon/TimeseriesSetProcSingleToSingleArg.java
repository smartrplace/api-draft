package org.ogema.timeseries.eval.simple.mon;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries2;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries3;

@Deprecated
public abstract class TimeseriesSetProcSingleToSingleArg<T> extends TimeseriesSetProcSingleToSingle implements TimeseriesSetProcessorArg<T> {
	protected abstract List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start,
			long end, AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2, T param);

	@Override
	protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
			AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2) {
		throw new UnsupportedOperationException("Only variant with parameter object allowed!");

	}

	public TimeseriesSetProcSingleToSingleArg(String labelPostfix) {
		super(labelPostfix);
	}
	public TimeseriesSetProcSingleToSingleArg(String labelPostfix, Integer absoluteTiming) {
		super(labelPostfix, absoluteTiming);
	}

	@Override
	public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService, T params) {
		List<Datapoint> result = new ArrayList<>();
		for(Datapoint tsdi: input) {
			String location = ProcessedReadOnlyTimeSeries3.getDpLocation(tsdi, labelPostfix);
			ProcTsProvider provider = new ProcTsProvider() {
				
				@Override
				public ProcessedReadOnlyTimeSeries2 getTimeseries(Datapoint newtsdi) {
					return new ProcessedReadOnlyTimeSeries2(tsdi) {
						@Override
						protected List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start,
								long end, AggregationMode mode) {
							return calculateValues(timeSeries, start, end, mode, this, params);						
						}
						@Override
						protected String getLabelPostfix() {
							return labelPostfix;
						}
						
						@Override
						protected long getCurrentTime() {
							return dpService.getFrameworkTime();
						}
						
						@Override
						protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
							TimeseriesSetProcSingleToSingleArg.this.alignUpdateIntervalFromSource(updateInterval);
						}
					};
				}
			};
			Datapoint newtsdi = getOrUpdateTsDp(location, provider , dpService);
			result.add(newtsdi);
		}
		return result;
	}
}
