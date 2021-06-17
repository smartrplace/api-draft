package org.ogema.timeseries.eval.simple.mon3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.AggregationModeProvider;
import org.ogema.devicefinder.util.DPUtil;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSimpleProcUtilBase;

import de.iwes.timeseries.eval.api.TimeSeriesData;

public abstract class TimeseriesSimpleProcUtilBase3 extends TimeseriesSimpleProcUtilBase {
	protected final Map<String, TimeseriesSetProcessor3> knownProcessors3 = new HashMap<>();
	public TimeseriesSetProcessor3 getProcessor3(String procID) {
		return knownProcessors3.get(procID);
	}
	
	public TimeseriesSimpleProcUtilBase3(ApplicationManager appMan, DatapointService dpService) {
		super(appMan, dpService);
	}
	
	@Override
	public List<Datapoint> process(String tsProcessRequest, List<Datapoint> input) {
		TimeseriesSetProcessor3 proc = knownProcessors3.get(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		return proc.getResultSeries(input, true, dpService);
	}
	
	/** Regarding calculation notes see {@link TimeseriesSetProcMultiToSingle3}
	 * 
	 * @param tsProcessRequest
	 * @param input
	 * @return
	 */
	@Override
	public Datapoint processMultiToSingle(String tsProcessRequest, List<Datapoint> input) {
		TimeseriesSetProcessor3 proc = knownProcessors3.get(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		List<Datapoint> resultTs = proc.getResultSeries(input, true, dpService);
		if(resultTs != null && !resultTs.isEmpty())
			return resultTs.get(0);
		return null;
	}
	
	@Override
	public Datapoint processSingle(String tsProcessRequest, Datapoint dp) {
		TimeseriesSetProcessor3 proc = getProcessor3(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(new Datapoint[] {dp}), true, dpService);
		if(resultTs != null && !resultTs.isEmpty())
			return resultTs.get(0);
		return null;
	}
	
	/** Variant of {@link #processSingle(String, Datapoint)} allowing to pass an object containing parameters
	 * that shall be processed
	 * @param tsProcessRequest must reference to a TimeseriesSetProcessorArg (taking argument object)
	 * @param dp
	 * @param params
	 * @return
	 */
	//TODO: Add support
	/*public <T> Datapoint processSingle(String tsProcessRequest, Datapoint dp, T params) {
		TimeseriesSetProcessor3 procRaw = getProcessor3(tsProcessRequest);
		if(procRaw == null || (!(procRaw instanceof TimeseriesSetProcessorArg)))
			throw new IllegalArgumentException("Unknown or unfitting timeseries processor: "+tsProcessRequest);
		@SuppressWarnings("unchecked")
		TimeseriesSetProcessorArg<T> proc = (TimeseriesSetProcessorArg<T>) procRaw;
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(new Datapoint[] {dp}), true, dpService, params);
		if(resultTs != null && !resultTs.isEmpty())
			return resultTs.get(0);
		return null;
	}*/

	/** For processing of two or more aligned time series that are not interchangeable like difference, division,...*/
	@Override
	public Datapoint processArgs(String tsProcessRequest, Datapoint... dp) {
		TimeseriesSetProcessor3 proc = getProcessor3(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(dp), true, dpService);
		if(resultTs != null && !resultTs.isEmpty())
			return resultTs.get(0);
		return null;
	}

	@Override
	public List<Datapoint> processSingleToMulti(String tsProcessRequest, Datapoint dp) {
		List<Datapoint> result = new ArrayList<>();
		TimeseriesSetProcessor3 proc = getProcessor3(tsProcessRequest);
		if(proc == null)
			throw new IllegalArgumentException("Unknown timeseries processor: "+tsProcessRequest);
		List<Datapoint> resultTs = proc.getResultSeries(Arrays.asList(new Datapoint[] {dp}), true, dpService);
		if(resultTs != null && !resultTs.isEmpty())
			result.addAll(resultTs);
		
		return result;
	}

	@Deprecated //Should not be used anymore
	public List<TimeSeriesData> processTSD(String tsProcessRequest, List<TimeSeriesData> input,
			TimeSeriesNameProvider nameProvider, AggregationModeProvider aggProv) {
		return DPUtil.getTSList(process(tsProcessRequest, DPUtil.getDPList(input, nameProvider, aggProv)), null);
	}
}
