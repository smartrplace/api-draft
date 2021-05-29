package org.ogema.timeseries.eval.simple.mon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DpUpdateAPI.DpUpdated;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries2;
import org.ogema.timeseries.eval.simple.api.TimeProcPrint;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil.MeterReference;

import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

public class TimeseriesSimpleProcUtil extends TimeseriesSimpleProcUtilBase { 
	public static final int DEFAULT_UPDATE_MODE = 4;
	
	public TimeseriesSimpleProcUtil(ApplicationManager appMan, DatapointService dpService) {
		this(appMan, dpService, DEFAULT_UPDATE_MODE);
	}
	
	public TimeseriesSimpleProcUtil(ApplicationManager appMan, DatapointService dpService, int updateMode) {
		this(appMan, dpService, updateMode, null);
	}

	/** Setup new instance for creating timeseries
	 * 
	 * @param appMan
	 * @param dpService
	 * @param updateMode see {@link TimeseriesSetProcMultiToSingle#updateMode}:
	 * 	 Update mode regarding interval propagation and regarding singleInput.<br>
	 * 	 Note that from mode 2 on any change in the input data triggers a recalculation of the output data<br>
	 * 
	 *  0: Do not generate a result time series if input is empty, no updates
	 *  1: Update only at the end
	 *  2: Update exactly for any input change interval
	 *  3: Update for any input change onwards completely
	 *  4: Update completely if any input has a change 
	 */
	public TimeseriesSimpleProcUtil(ApplicationManager appMan, DatapointService dpService,
			int updateMode, Long minIntervalForReCalc) {
		super(appMan, dpService);
		
		TimeseriesSetProcessor meterProc = new TimeseriesSetProcSingleToSingle("_vm") {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2) {
				MeterReference ref = TimeSeriesServlet.getDefaultMeteringReference(timeSeries, start, appMan); /*new MeterReference();
				ref.referenceMeterValue = 0;
				if(timeSeries instanceof RecordedData) {
					Resource parent = appMan.getResourceAccess().getResource(((RecordedData)timeSeries).getPath());
					if(parent != null) {
						FloatResource refVal = parent.getSubResource("refTimeCounter", FloatResource.class);
						if(refVal.exists())
							ref.referenceMeterValue = refVal.getValue();
					}
				}
				TimeResource refRes = TimeProcUtil.getDefaultMeteringReferenceResource(appMan.getResourceAccess());
				if(!refRes.exists()) {
					refRes.create();
					refRes.setValue(start);
					refRes.activate(false);
				}
				ref.referenceTime = refRes.getValue();*/
				return TimeSeriesServlet.getMeterFromConsumption(timeSeries, start, end, ref, mode);						
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {}
		};
		knownProcessors.put(TimeProcUtil.METER_EVAL, meterProc);
		
		TimeseriesSetProcessor dayProc = new TimeseriesSetProcSingleToSingle("_proTag", AbsoluteTiming.DAY) {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2) {
				List<SampledValue> result = TimeSeriesServlet.getDayValues(timeSeries, start, end, mode,
						newTs2.getInputDp()!=null?newTs2.getInputDp().getScale():null);
				return result;
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
				updateInterval.start = AbsoluteTimeHelper.getIntervalStart(updateInterval.start, AbsoluteTiming.DAY);
				updateInterval.end = AbsoluteTimeHelper.getNextStepTime(updateInterval.end, AbsoluteTiming.DAY)-1;				
			}
		};
		knownProcessors.put(TimeProcUtil.PER_DAY_EVAL, dayProc);
		
		TimeseriesSetProcessor hourProc = new TimeseriesSetProcSingleToSingle("_proStunde", AbsoluteTiming.HOUR) {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2) {
				List<SampledValue> result = TimeSeriesServlet.getDayValues(timeSeries, start, end, mode,
						newTs2.getInputDp()!=null?newTs2.getInputDp().getScale():null, false, AbsoluteTiming.HOUR);
				return result;
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
				updateInterval.start = AbsoluteTimeHelper.getIntervalStart(updateInterval.start, AbsoluteTiming.HOUR);
				updateInterval.end = AbsoluteTimeHelper.getNextStepTime(updateInterval.end, AbsoluteTiming.HOUR)-1;				
			}
		};
		knownProcessors.put(TimeProcUtil.PER_HOUR_EVAL, hourProc);
		
		TimeseriesSetProcessor min15Proc = new TimeseriesSetProcSingleToSingle("_per15min", AbsoluteTiming.FIFTEEN_MINUTE) {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2) {
				List<SampledValue> result = TimeSeriesServlet.getDayValues(timeSeries, start, end, mode,
						newTs2.getInputDp()!=null?newTs2.getInputDp().getScale():null, false, AbsoluteTiming.FIFTEEN_MINUTE);
				return result;
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
				updateInterval.start = AbsoluteTimeHelper.getIntervalStart(updateInterval.start, AbsoluteTiming.FIFTEEN_MINUTE);
				updateInterval.end = AbsoluteTimeHelper.getNextStepTime(updateInterval.end, AbsoluteTiming.FIFTEEN_MINUTE)-1;				
			}
		};
		knownProcessors.put(TimeProcUtil.PER_15M_EVAL, min15Proc);

		TimeseriesSetProcessor minuteProc = new TimeseriesSetProcSingleToSingle(TimeProcUtil.PER_MINUTE_SUFFIX, AbsoluteTiming.FIFTEEN_MINUTE) {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2) {
				List<SampledValue> result = TimeSeriesServlet.getDayValues(timeSeries, start, end, mode,
						newTs2.getInputDp()!=null?newTs2.getInputDp().getScale():null, false, AbsoluteTiming.MINUTE);
				return result;
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
				updateInterval.start = AbsoluteTimeHelper.getIntervalStart(updateInterval.start, AbsoluteTiming.MINUTE);
				updateInterval.end = AbsoluteTimeHelper.getNextStepTime(updateInterval.end, AbsoluteTiming.MINUTE)-1;				
			}
		};
		knownProcessors.put(TimeProcUtil.PER_MINUTE_EVAL, minuteProc);
		
		TimeseriesSetProcessor monthProc = new TimeseriesSetProcSingleToSingle("_perMonth", AbsoluteTiming.MONTH) {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2) {
				List<SampledValue> result = TimeSeriesServlet.getDayValues(timeSeries, start, end, mode,
						newTs2.getInputDp()!=null?newTs2.getInputDp().getScale():null, false, AbsoluteTiming.MONTH);
				return result;
			}

			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
				updateInterval.start = AbsoluteTimeHelper.getIntervalStart(updateInterval.start, AbsoluteTiming.MONTH);
				updateInterval.end = AbsoluteTimeHelper.getNextStepTime(updateInterval.end, AbsoluteTiming.MONTH)-1;				
			}
		};
		knownProcessors.put(TimeProcUtil.PER_MONTH_EVAL, monthProc);
		
		TimeseriesSetProcessor yearProc = new TimeseriesSetProcSingleToSingle("_perYear", AbsoluteTiming.YEAR) {
			
			@Override
			protected List<SampledValue> calculateValues(ReadOnlyTimeSeries timeSeries, long start, long end,
					AggregationMode mode, ProcessedReadOnlyTimeSeries2 newTs2) {
				List<SampledValue> result = TimeSeriesServlet.getDayValues(timeSeries, start, end, mode,
						newTs2.getInputDp()!=null?newTs2.getInputDp().getScale():null, false, AbsoluteTiming.YEAR);
				return result;
			}
			
			@Override
			protected void alignUpdateIntervalFromSource(DpUpdated updateInterval) {
				updateInterval.start = AbsoluteTimeHelper.getIntervalStart(updateInterval.start, AbsoluteTiming.YEAR);
				updateInterval.end = AbsoluteTimeHelper.getNextStepTime(updateInterval.end, AbsoluteTiming.YEAR)-1;				
			}
		};
		knownProcessors.put(TimeProcUtil.PER_YEAR_EVAL, yearProc);

		TimeseriesSetProcessor sumProc = new TimeseriesSetProcessor() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService) {
if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "IN(0):Dayproc", 1, null, null);
				List<Datapoint> result1 = dayProc.getResultSeries(input, dpService);
				TimeseriesSetProcSum sumProc = new TimeseriesSetProcSum("total_sum", AbsoluteTiming.DAY,
						(updateMode>0)?AbsoluteTiming.DAY:null, minIntervalForReCalc, updateMode) {
					@Override
					protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "--RT-OUT/IN(2):Dayproc", 1, null, null);
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printFirstElements(resultLoc, "--RT-OUT(1):Total_Sum");
					}
				};
				//sumProc.updateMode = updateMode;
				List<Datapoint> result = sumProc.getResultSeries(result1, dpService);
				return result;
			}
		};
		knownProcessors.put(TimeProcUtil.SUM_PER_DAY_EVAL, sumProc);

		TimeseriesSetProcessor sumProcHour = new TimeseriesSetProcessor() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService) {
if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "IN(0):Hourproc", 1, null, null);
				List<Datapoint> result1 = hourProc.getResultSeries(input, dpService);
				TimeseriesSetProcSum sumProc = new TimeseriesSetProcSum("total_sum_hour", AbsoluteTiming.HOUR,
						(updateMode>0)?AbsoluteTiming.HOUR:null, minIntervalForReCalc, updateMode) {
					@Override
					protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "--RT-OUT/IN(2):Hourproc", 1, null, null);
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printFirstElements(resultLoc, "--RT-OUT(1):Total_Sum_hour");
					}
				};
				List<Datapoint> result = sumProc.getResultSeries(result1, dpService);
				//sumProc.setUpdateMode(updateMode);
				return result;
			}
		};
		knownProcessors.put(TimeProcUtil.SUM_PER_HOUR_EVAL, sumProcHour);

		TimeseriesSetProcessor sumProc15Min = new TimeseriesSetProcessor() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService) {
if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "IN(0):15Minproc", 1, null, null);
				List<Datapoint> result1 = min15Proc.getResultSeries(input, dpService);
				TimeseriesSetProcSum sumProc = new TimeseriesSetProcSum("total_sum_15min", AbsoluteTiming.FIFTEEN_MINUTE,
						(updateMode>0)?AbsoluteTiming.FIFTEEN_MINUTE:null, minIntervalForReCalc, updateMode) {
					@Override
					protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "--RT-OUT/IN(2):15Minproc", 1, null, null);
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printFirstElements(resultLoc, "--RT-OUT(1):Total_Sum_15min");
					}
				};
				List<Datapoint> result = sumProc.getResultSeries(result1, dpService);
				//sumProc.updateMode = updateMode;
				return result;
			}
		};
		knownProcessors.put(TimeProcUtil.SUM_PER_15M_EVAL, sumProc15Min);
		
		TimeseriesSetProcessor sumProcMinute = new TimeseriesSetProcessor() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService) {
if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "IN(0):MinuteProc", 1, null, null);
				List<Datapoint> result1 = minuteProc.getResultSeries(input, dpService);
				TimeseriesSetProcSum sumProc = new TimeseriesSetProcSum("total_sum_minute", AbsoluteTiming.MINUTE,
						(updateMode>0)?AbsoluteTiming.MINUTE:null, minIntervalForReCalc, updateMode) {
					@Override
					protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "--RT-OUT/IN(2):MinuteProc", 1, null, null);
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printFirstElements(resultLoc, "--RT-OUT(1):Total_Sum_minute");
					}
				};
				List<Datapoint> result = sumProc.getResultSeries(result1, dpService);
				//sumProc.updateMode = updateMode;
				return result;
			}
		};
		knownProcessors.put(TimeProcUtil.SUM_PER_MINUTE_EVAL, sumProcMinute);

		TimeseriesSetProcessor sumProcMonth = new TimeseriesSetProcessor() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService) {
if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "IN(0):Monthproc", 1, null, null);
				List<Datapoint> result1 = monthProc.getResultSeries(input, dpService);
				TimeseriesSetProcSum sumProc = new TimeseriesSetProcSum("total_sum_month", AbsoluteTiming.MONTH,
						(updateMode>0)?AbsoluteTiming.MONTH:null, minIntervalForReCalc, updateMode) {
					@Override
					protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "--RT-OUT/IN(2):Monthproc", 1, null, null);
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printFirstElements(resultLoc, "--RT-OUT(1):Total_Sum_month");
					}
				};
				List<Datapoint> result = sumProc.getResultSeries(result1, dpService);
				//sumProc.updateMode = updateMode;
				return result;
			}
		};
		knownProcessors.put(TimeProcUtil.SUM_PER_MONTH_EVAL, sumProcMonth);

		TimeseriesSetProcessor sumProcYear = new TimeseriesSetProcessor() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService) {
if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "IN(0):Yearproc", 1, null, null);
				List<Datapoint> result1 = yearProc.getResultSeries(input, dpService);
				TimeseriesSetProcSum sumProc = new TimeseriesSetProcSum("total_sum_year", AbsoluteTiming.YEAR,
						(updateMode>0)?AbsoluteTiming.YEAR:null, minIntervalForReCalc, updateMode) {
					@Override
					protected void debugCalculationResult(List<Datapoint> input, List<SampledValue> resultLoc) {
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printTimeSeriesSet(input, "--RT-OUT/IN(2):Yearproc", 1, null, null);
						if(Boolean.getBoolean("evaldebug")) TimeProcPrint.printFirstElements(resultLoc, "--RT-OUT(1):Total_Sum_year");
					}
				};
				List<Datapoint> result = sumProc.getResultSeries(result1, dpService);
				//sumProc.updateMode = updateMode;
				return result;
			}
		};
		knownProcessors.put(TimeProcUtil.SUM_PER_YEAR_EVAL, sumProcYear);

		TimeseriesSetProcessor dayPerRoomProc = new TimeseriesSetProcessor() {
			
			@Override
			public List<Datapoint> getResultSeries(List<Datapoint> input, DatapointService dpService) {
				List<Datapoint> result1 = dayProc.getResultSeries(input, dpService);
				List<Datapoint> result = new ArrayList<>();
				// RoomID -> Timeseries in the room
				Map<String, List<Datapoint>> sortedbyRoom = new HashMap<>();
				for(Datapoint tsd: result1) {
					Datapoint dp = dpService.getDataPointAsIs(tsd.getLocation());
					String label;
					if(dp.getRoom() != null)
						label = dp.getRoom().label(null);
					else
						label = "noRoom";
					List<Datapoint> roomList = sortedbyRoom.get(label);
					if(roomList == null) {
						roomList = new ArrayList<>();
						sortedbyRoom.put(label, roomList);
					}
					roomList.add(tsd);
				}
				for(Entry<String, List<Datapoint>> roomData: sortedbyRoom.entrySet()) {
					TimeseriesSetProcSum sumProc = new TimeseriesSetProcSum(roomData.getKey()+"_sum", AbsoluteTiming.DAY,
							(updateMode>0)?AbsoluteTiming.DAY:null, minIntervalForReCalc, updateMode);
					//sumProc.updateMode = updateMode;
					List<Datapoint> resultLoc = sumProc.getResultSeries(roomData.getValue(), dpService);
					if(!roomData.getValue().isEmpty()) {
						DPRoom room = roomData.getValue().get(0).getRoom();
						for(Datapoint dpLoc: resultLoc) {
							if(room != null)
								dpLoc.setRoom(room);
							else
								dpLoc.setRoom(TimeProcUtil.unknownRoom);
						}
					}
					result.addAll(resultLoc);
				}
				return result;
			}
		};

		knownProcessors.put(TimeProcUtil.SUM_PER_DAY_PER_ROOM_EVAL, dayPerRoomProc);
	}
}
