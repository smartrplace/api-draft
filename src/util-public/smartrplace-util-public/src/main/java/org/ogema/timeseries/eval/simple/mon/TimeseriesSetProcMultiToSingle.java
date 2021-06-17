package org.ogema.timeseries.eval.simple.mon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.devicefinder.util.DatapointImpl;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries2;
import org.ogema.timeseries.eval.simple.api.TimeProcPrint;
import org.ogema.timeseries.eval.simple.mon.TimeseriesSetProcSingleToSingle.ProcTsProvider;
import org.smartrplace.tissue.util.logconfig.PerformanceLog;

import de.iwes.util.timer.AbsoluteTimeHelper;

/** The input time series for this provider must be aligned having common time stamps
 * and the time series starting first and ending last shall have no gap in between that occurs not in
 * all input series.<br>
 * Note that this processor evaluates the input time series on calling {@link #getResultSeries(List, DatapointService)} as
 * it has to find out which time series starts first and which ends last. Based on this it builds a
 * temporary input time series inputSingle.
 * Note also that you have to set the label of the resulting datapoint explicitly as the label cannot
 * be directly obtained from a single input datapoint with postfix as for {@link TimeseriesSetProcSingleToSingle}.
 * If you are using a Multi2Single predefined evaluation you still have to set the label afterwards. You should
 * set at least label(null) or label(ENGLISH).*/
public abstract class TimeseriesSetProcMultiToSingle implements TimeseriesSetProcessor {

	public static PerformanceLog tsSingleLog;
	public static PerformanceLog aggregateLog;
	
	protected abstract float aggregateValues(Float[] values, long timestamp, AggregationMode mode);
	
	/** change startTime and endTime of parameter if necessary*/
	protected abstract void alignUpdateIntervalFromSource(DpUpdated updateInterval);
	
	/** Update mode regarding interval propagation and regarding singleInput.<br>
	 * Note that from mode 2 on any change in the input data triggers a recalculation of the output data<br>
	 * !! Note also that this is set for the entire processor, so usually for your instance of {@link TimeseriesSimpleProcUtil} !!
	 * 
	 * 0: Do not generate a result time series if input is empty, no updates
	 *  1: Update only at the end
	 *  2: Update exactly for any input change interval
	 *  3: Update for any input change onwards completely
	 *  4: Update completely if any input has a change 
	 */
	public final int updateMode;
	//public void setUpdateMode(int value) {
	//	this.updateMode = value;
	//}
	
	protected final Integer absoluteTiming;
	
	protected final Long minIntervalForReCalc;

	/** Currently we assume that two input time series cover the entire range and that one of them always covers the start
	 * and the other always covers the end. The two time series may not be available at the time of the result timeseries
	 * creation, but once they are set they are not changed anymore.
	 */
	public static class GetInputSingleResult {
		public DatapointImpl dpIn;
		public ProcTsProvider provider;
		/** Only relevant for standard maximum input timeseries processing*/
		public GetInputSingleMax maxData;
	}
	public static class GetInputSingleMax {
		long firstStart = Long.MAX_VALUE;
		//just a helper to find firstStartTsFinal
		ReadOnlyTimeSeries firstStartTs = null;
		long lastEnd = 0;
		//just a helper to find lastEndTsFinal
		ReadOnlyTimeSeries lastEndTs = null;
		long firstStartTSEnd;
		ReadOnlyTimeSeries firstStartTsFinal;
		ReadOnlyTimeSeries lastEndTsFinal;
	}
	
	/** Overwrite if the leading input single time series shall not cover the maximum range of input
	 * data, but e.g. only the areas where all input time series have values
	 * @param input
	 * @param dpService
	 * @return
	 */
	protected GetInputSingleResult getInputSingle(List<Datapoint> input, DatapointService dpService) {
		GetInputSingleResult result = new GetInputSingleResult();
		result.maxData = updateParametersInputSingle(null, input, dpService);
		if(result.maxData == null) {
			return null;
		}

		ProcessedReadOnlyTimeSeries inputSingle = new ProcessedReadOnlyTimeSeries(InterpolationMode.NONE, absoluteTiming, minIntervalForReCalc) {
			protected long lastGetValues = 0;
			
			@Override
			protected List<SampledValue> updateValues(long start, long end) {
if(Boolean.getBoolean("evaldebug")) System.out.println("updateValues(2) for  "+dpLabel()+" "+TimeProcPrint.getFullTime(start)+" : "+TimeProcPrint.getFullTime(end));
				//TODO: Now we stop this search as soon as we found the timeseries once. With volatile
				//timeseries we might have to update this later on again.
				if(result.maxData.firstStartTs == null)
					result.maxData = updateParametersInputSingle(null, input, dpService);
				if(result.maxData.firstStartTs == null)
					return Collections.emptyList();
				GetInputSingleMax maxData = result.maxData;
				if(end <= maxData.firstStartTSEnd) {
if(Boolean.getBoolean("evaldebug")) System.out.println("Ret updateValues  "+dpLabel()+" based on firstStartTs:"+TimeProcPrint.getTimeseriesName(maxData.firstStartTsFinal, true));
					return maxData.firstStartTsFinal.getValues(start, end);
				}
				if(start >= maxData.firstStartTSEnd) {
if(Boolean.getBoolean("evaldebug")) System.out.println("Ret updateValues  "+dpLabel()+" based on lastEndTs:"+TimeProcPrint.getTimeseriesName(maxData.lastEndTsFinal, true));
					return maxData.lastEndTsFinal.getValues(start, end);
				}
				List<SampledValue> resultLoc = new ArrayList<>(maxData.firstStartTsFinal.getValues(start, maxData.firstStartTSEnd));
				resultLoc.addAll(maxData.lastEndTsFinal.getValues(maxData.firstStartTSEnd, end));
if(Boolean.getBoolean("evaldebug")) System.out.println("Ret updateValues  "+dpLabel()+" based on firstStartTs:"+TimeProcPrint.getTimeseriesName(maxData.firstStartTsFinal, true)+" and lastEndTs:"+TimeProcPrint.getTimeseriesName(maxData.lastEndTsFinal, true));
				return resultLoc;
			}
			
			@Override
			public List<SampledValue> getValues(long startTime, long endTime) {
				long now = getCurrentTime();
				if(updateMode == 4) {
					for(Datapoint dp: input) {
						DpUpdated changed = dp.getSingleIntervalChanged(lastGetValues);
						if(changed != null) {
							DpUpdated all = DatapointImpl.getAllInterval(changed.updateTime);
							addIntervalToUpdate(all);
							result.maxData = updateParametersInputSingle(null, input, dpService);
							break;
						}
					}
				}
				else if(updateMode >= 2) {
					boolean foundChange = true;
					for(Datapoint dp: input) {
						DpUpdated changed = dp.getSingleIntervalChanged(lastGetValues);
						if(changed != null) {
							if((updateMode == 3) && (changed.end < now))
								changed.end = now;
							addIntervalToUpdate(changed);
							foundChange = true;
						}
					}
					if(foundChange)
						result.maxData = updateParametersInputSingle(null, input, dpService);
				}
				lastGetValues = now;
				return super.getValues(startTime, endTime);
			} 
			
			@Override
			protected String dpLabel() {
				return "InpSingleLead_"+((resultSeriesStore!=null)?resultSeriesStore.dpLabel():"?");
			}
			
			@Override
			protected long getCurrentTime() {
				return dpService.getFrameworkTime();
			}
		};
		
		String tsSingleResLoc = resultLoction(input)+"_SgInp";
		result.dpIn = (DatapointImpl) dpService.getDataPointStandard(tsSingleResLoc); //new DatapointImpl(inputSingle, resultLoction(input));
		result.dpIn.setTimeSeries(inputSingle);
		result.provider = null; //use default
		return result;
	}
	protected GetInputSingleMax updateParametersInputSingle(GetInputSingleMax result,
			List<Datapoint> input, DatapointService dpService) {
		if(result == null)
			result = new GetInputSingleMax();
		for(Datapoint tsd: input) {
			ReadOnlyTimeSeries ts = tsd.getTimeSeries();
			SampledValue svStart = ts.getNextValue(0);
			SampledValue svEnd = ts.getPreviousValue(Long.MAX_VALUE);
			if((svStart != null) && (svStart.getTimestamp() < result.firstStart)) {
				result.firstStart = svStart.getTimestamp();
				result.firstStartTs = ts;
			}
			if((svEnd != null) && (svEnd.getTimestamp() > result.lastEnd)) {
				result.lastEnd = svEnd.getTimestamp();
				result.lastEndTs = ts;
			}
		}
		result.firstStartTsFinal = result.firstStartTs;
		result.lastEndTsFinal = result.lastEndTs;

		if(result.firstStartTs == null) {
			if(updateMode == 0)
				return null;
			return result; //return null //return Collections.emptyList()
		}
		//TODO: We assume that startTS and endTS overlap. If not we might have to use more timeseries intermediately
		result.firstStartTSEnd = result.firstStartTs.getPreviousValue(Long.MAX_VALUE).getTimestamp();
		return result;
	}
	
	protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {};
	//protected abstract AggregationMode getMode(String tsLabel);
	protected String resultLabel() {return label;}
	public String resultLoction(List<Datapoint> input) {
		return resultLoction(input, this.getClass().getName());
	}

	public static String resultLoction(List<Datapoint> input, String className) {
		JSONObject json = new JSONObject();
		JSONArray args = new JSONArray();
		for(Datapoint inp:input) {
			args.put(inp.getLocation());
		}
		json.put(className, args);
		String location = json.toString();
		return location;
	}
	
	protected final String label;
	//protected final int intervalType2;
	private final long TEST_SHIFT;
	
	/*public TimeseriesSetProcMultiToSingle(String resultlabel) {
		this(resultlabel, AbsoluteTiming.DAY);
	}
	public TimeseriesSetProcMultiToSingle(String resultlabel, int intervalType) {
		this(resultlabel, intervalType, null);
	}*/
	/**
	 * 
	 * @param resultlabel
	 * @param intervalType relevant for test shift only
	 * @param absoluteTiming if set then knownEnd will be reset to beginning of interval always
	 */
	/*public TimeseriesSetProcMultiToSingle(String resultlabel, int intervalType, Integer absoluteTiming) {
		this(resultlabel, intervalType, absoluteTiming, null);
	}
	private TimeseriesSetProcMultiToSingle(String resultlabel, int intervalType, Integer absoluteTiming,
			Long minIntervalForReCalc) {
		this(resultlabel, intervalType, absoluteTiming, minIntervalForReCalc, TimeseriesSimpleProcUtil.DEFAULT_UPDATE_MODE);
	}*/
	public TimeseriesSetProcMultiToSingle(String resultlabel, int intervalType, Integer absoluteTiming,
			Long minIntervalForReCalc, int updateMode) {
		this.label = resultlabel;
		//this.intervalType = intervalType;
		this.absoluteTiming = absoluteTiming;
		this.updateMode = updateMode;
		this.minIntervalForReCalc = minIntervalForReCalc;
		TEST_SHIFT = (long) (0.9*AbsoluteTimeHelper.getStandardInterval(intervalType)); //TimeProcUtil.DAY_MILLIS-2*TimeProcUtil.HOUR_MILLIS;
	}

	protected ProcessedReadOnlyTimeSeries2 resultSeriesStore = null;
	@Override
	public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();

		GetInputSingleResult inputSingle = getInputSingle(input, dpService);
		if(inputSingle == null)
			return Collections.emptyList();
		if(inputSingle.provider == null) inputSingle.provider = new ProcTsProvider() {
			
			@Override
			public ProcessedReadOnlyTimeSeries2 getTimeseries(Datapoint newtsdi) {
				resultSeriesStore = new ProcessedReadOnlyTimeSeries2(inputSingle.dpIn, absoluteTiming, minIntervalForReCalc) {
					@Override
					protected List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start,
							long end, AggregationMode mode) {
long startOfAgg = dpService.getFrameworkTime();
if(Boolean.getBoolean("evaldebug")) System.out.println("Starting aggregation for "+getShortId());
						long startOfCalc = dpService.getFrameworkTime();
						
						Float[] values = new Float[input.size()];
						List<SampledValue> resultLoc = new ArrayList<>();
						List<SampledValue> vals = getTSDI().getTimeSeries().getValues(start, end);
long endOfAgg1 =  dpService.getFrameworkTime();
if(aggregateLog != null) aggregateLog.logEvent(endOfAgg1-startOfAgg, "Calculation of AG1 "+getShortId()+" took");
						for(SampledValue svalTs: vals) {
							long timestamp = svalTs.getTimestamp();
							int idx = 0;
							for(Datapoint dpLoc: input) {
								SampledValue svLoc = dpLoc.getTimeSeries().getValue(timestamp);
								if(svLoc == null) {
									values[idx] = null;
									List<SampledValue> svTest = dpLoc.getTimeSeries().getValues(timestamp-TEST_SHIFT, timestamp+TEST_SHIFT);
									if(svTest != null && (!svTest.isEmpty())) {
										System.out.println("  !!! Warning: Input time steps not aligned for:"+idx+" in "+label);
									}
								} else
									values[idx] = svLoc.getValue().getFloatValue();
								idx++;
							}
long startOfAgg2 = dpService.getFrameworkTime();
							float val = aggregateValues(values, timestamp, mode);
long endOfAgg =  dpService.getFrameworkTime();
if(aggregateLog != null) aggregateLog.logEvent(endOfAgg-startOfAgg2, "Calculation of AG2 "+getShortId()+" took");
if(aggregateLog != null) aggregateLog.logEvent(endOfAgg-startOfAgg, "Calculation of AGG "+getShortId()+" took");
							resultLoc.add(new SampledValue(new FloatValue(val), timestamp, Quality.GOOD));
						}
						debugCalculationResult(input, resultLoc);
						
						long endOfCalc =  dpService.getFrameworkTime();
//TODO: These values could be logged to check evaluation performance
if(tsSingleLog != null) tsSingleLog.logEvent((endOfCalc-startOfCalc), "Calculation of TSI "+getShortId()+" took");

						return resultLoc;
					}
					
					@Override
					protected long getCurrentTime() {
						return dpService.getFrameworkTime();
					}
					
					@Override
					protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
						TimeseriesSetProcMultiToSingle.this.alignUpdateIntervalFromSource(updateInterval);
					}
				};
				return resultSeriesStore;
			}
		};

		String location = resultLoction(input); //getinputSingle.dpIn.getLocation()+"";
		Datapoint newtsdi = TimeseriesSetProcSingleToSingle.getOrUpdateTsDp(location, inputSingle.provider, dpService);
		String label = resultLabel();
		if(label != null)
			newtsdi.setLabelDefault(label);
		result.add(newtsdi);
		return result;
	}

	public static String getResultDpLocation(List<Datapoint> input, String className) {
		return resultLoction(input, className)+"_SgInp";		
	}
}
