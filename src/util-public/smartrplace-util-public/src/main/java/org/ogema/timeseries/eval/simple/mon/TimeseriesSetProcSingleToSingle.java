package org.ogema.timeseries.eval.simple.mon;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DpUpdateAPI.DpGap;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries2;
import org.ogema.timeseries.eval.simple.api.TimeProcPrint;

import de.iwes.util.timer.AbsoluteTimeHelper;

public abstract class TimeseriesSetProcSingleToSingle implements TimeseriesSetProcessor {
	protected final Integer absoluteTiming;
	/** Perform calculation on a certain input series.
	 * 
	 * @param timeSeries input time series
	 * @param start
	 * @param end
	 * @param mode
	 * @param newTs2 This series will contain the result time series, but also has the reference to
	 * 		the input datapoint that can be accessed with {@link ProcessedReadOnlyTimeSeries2#getInputDp()}
	 * @return
	 */
	protected abstract List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start,
			long end, AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2);
	
	/** change startTime and endTime of parameter if necessary*/
	protected abstract void alignUpdateIntervalFromSource(DpUpdated updateInterval);
		
	//protected TimeSeriesNameProvider nameProvider() {return null;}
	//protected abstract AggregationMode getMode(String tsLabel);
	protected final String labelPostfix;
	
	/** Return true if informatoin relevant for the labelling has been added*/
	protected boolean addDatapointInfo(Datapoint tsdi) {
		return false;
	}
	
	public TimeseriesSetProcSingleToSingle(String labelPostfix) {
		this(labelPostfix, null);
	}
	public TimeseriesSetProcSingleToSingle(String labelPostfix, Integer absoluteTiming) {
		this.labelPostfix = labelPostfix;
		this.absoluteTiming = absoluteTiming;
	}
	@Override
	public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService) {
		List<Datapoint> result = new ArrayList<>();
		for(Datapoint tsdi: input) {
			String location = ProcessedReadOnlyTimeSeries2.getDpLocation(tsdi, labelPostfix);
			ProcTsProvider provider = new ProcTsProvider() {
				
				@Override
				public ProcessedReadOnlyTimeSeries2 getTimeseries(Datapoint newtsdi) {
					return new ProcessedReadOnlyTimeSeries2(tsdi) {
						@Override
						protected List<SampledValue> getResultValues(ReadOnlyTimeSeries timeSeries, long start,
								long end, AggregationMode mode) {
SampledValue sv = timeSeries.getPreviousValue(Long.MAX_VALUE);
if(Boolean.getBoolean("evaldebug")) System.out.println("Calculate in "+dpLabel()+" lastInput:"+((sv!=null)?TimeProcPrint.getFullTime(sv.getTimestamp()):"no sv"));
							return calculateValues(timeSeries, start, end, mode, this);						
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
							TimeseriesSetProcSingleToSingle.this.alignUpdateIntervalFromSource(updateInterval);
						}
						@Override
						protected List<DpGap> getIntervalsToUpdate(long startTime, long endTime) {
							if(absoluteTiming == null)
								return super.getIntervalsToUpdate(startTime, endTime);
							List<DpGap> result = new ArrayList<>(super.getIntervalsToUpdate(startTime, endTime));
							ReadOnlyTimeSeries ts = tsdi.getTimeSeries();
							Long lastTsInSource = getTimeStampInSourceInternal(false);
							SampledValue sv;
							if(lastTsInSource != null) {
								sv = ts.getNextValue(lastTsInSource+1);
							} else {
								sv = ts.getPreviousValue(Long.MAX_VALUE);
							}
							if(sv == null || (lastTsInSource != null && sv.getTimestamp() <= lastTsInSource)) {
if(Boolean.getBoolean("evaldebug")) System.out.println("last/new val in "+dpLabel()+" is no update:"+((sv!=null)?TimeProcPrint.getFullTime(sv.getTimestamp()):"no sv")+" last:"+((lastTsInSource!=null)?TimeProcPrint.getFullTime(lastTsInSource):"no last"));
								return result;
							}
							DpGap inResult = new DpGap();
							inResult.start = AbsoluteTimeHelper.getIntervalStart(sv.getTimestamp(), absoluteTiming);
							inResult.end = sv.getTimestamp();
							//List<DpGap> result = Arrays.asList(new DpGap[] {inResult});
							result.add(inResult);
if(Boolean.getBoolean("evaldebug")) System.out.println("new val in "+dpLabel()+" at:"+TimeProcPrint.getFullTime(sv.getTimestamp()));
							return result ;			
							
						}
					};
				}
			};
			Datapoint newtsdi = getOrUpdateTsDp(location, provider , dpService);
			result.add(newtsdi);
		}
		return result;
	}

	public interface ProcTsProvider {
		ProcessedReadOnlyTimeSeries2 getTimeseries(Datapoint newtsdi);
	}
	
	/**
	 * 
	 * @param resultLocation result location, usually generated based on tsdi location and postfix
	 * @param tsdi data point used as input
	 * @param provider
	 * @param dpService
	 * @return
	 */
	public static Datapoint getOrUpdateTsDp(String resultLocation, ProcTsProvider provider, DatapointService dpService) {
		ProcessedReadOnlyTimeSeries2 newTs2 = null;
		Datapoint newtsdi = null;
		if(dpService != null) {
			//String location = ProcessedReadOnlyTimeSeries2.getDpLocation(tsdi, labelPostfix);
			newtsdi = dpService.getDataPointStandard(resultLocation);
			ReadOnlyTimeSeries dpts = newtsdi.getTimeSeries();
			if((dpts != null) && (dpts instanceof ProcessedReadOnlyTimeSeries2))
				newTs2 = (ProcessedReadOnlyTimeSeries2) dpts; 
		} else
			throw new IllegalStateException("Operation without DatapointService not supported anymore!");
		if(newTs2 == null) {
			newTs2 = provider.getTimeseries(newtsdi);
			newtsdi = newTs2.getResultSeriesDP(dpService, resultLocation);
		}
		return newtsdi;
	}
}
