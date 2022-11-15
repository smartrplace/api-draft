package org.ogema.timeseries.eval.simple.mon;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.AggregationModeProvider;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries;
import org.smartrplace.gateway.device.MemoryTimeseriesPST;
import org.smartrplace.tissue.util.logconfig.PerformanceLog;
import org.smartrplace.tissue.util.logconfig.PerformanceLog.GwSubResProvider;

import de.iwes.timeseries.eval.api.TimeSeriesData;

public interface TimeseriesSimpleProcUtilBase {

	@SuppressWarnings("deprecation")
	/** Also used for TimeseriesSetProcMultiToSingle3.tsSingleLog, but no direct access possible at this position*/
	public static void initStaticLogs(ApplicationManager appMan) {
		TimeseriesSetProcMultiToSingle.tsSingleLog = PerformanceLog.getInstance(true, appMan, TimeseriesSetProcMultiToSingle.class.getName()+"_TSI",
				new GwSubResProvider() {
					
					@Override
					public FloatResource getEventResource(MemoryTimeseriesPST device) {
						return device.pstMultiToSingleEvents();
					}
					
					@Override
					public FloatResource getCounterResource(MemoryTimeseriesPST device) {
						return device.pstMultiToSingleCounter();
					}
				});
		/** Also used for TimeseriesSetProcMultiToSingle3.aggregateLog, but no direct access possible at this position*/
		TimeseriesSetProcMultiToSingle.aggregateLog = PerformanceLog.getInstance(true, appMan, TimeseriesSetProcMultiToSingle.class.getName()+"_AGG",
				new GwSubResProvider() {
					
					@Override
					public FloatResource getEventResource(MemoryTimeseriesPST device) {
						return device.pstMultiToSingleAggregations();
					}
					@Override
					public FloatResource getCounterResource(MemoryTimeseriesPST device) {
						return device.pstMultiToSingleAggregationsCounter();
					}
				});

		ProcessedReadOnlyTimeSeries.lockLog = PerformanceLog.getInstance(true, appMan, ProcessedReadOnlyTimeSeries.class.getName()+"_LCK",
				new GwSubResProvider() {
					
					@Override
					public FloatResource getEventResource(MemoryTimeseriesPST device) {
						return device.pstBlockingSingeEvents();
					}
					
					@Override
					public FloatResource getCounterResource(MemoryTimeseriesPST device) {
						return device.pstBlockingCounter();
					}
				});
		ProcessedReadOnlyTimeSeries.subTsBuildLog = PerformanceLog.getInstance(true, appMan, ProcessedReadOnlyTimeSeries.class.getName()+"_SUB",
				new GwSubResProvider() {
					
					@Override
					public FloatResource getEventResource(MemoryTimeseriesPST device) {
						return device.pstSubTsBuild();
					}
					
					@Override
					public FloatResource getCounterResource(MemoryTimeseriesPST device) {
						return device.pstSubTsBuildCounter();
					}
				});
		/*ProcessedReadOnlyTimeSeries2.uv2Log = PerformanceLog.getInstance(true, appMan, ProcessedReadOnlyTimeSeries2.class.getName()+"_UV2",
				new GwSubResProvider() {
					
					@Override
					public FloatResource getEventResource(MemoryTimeseriesPST device) {
						return device.pstUpdateValuesPS2();
					}
					
					@Override
					public FloatResource getCounterResource(MemoryTimeseriesPST device) {
						return device.pstUpdateValuesPS2Counter();
					}
				});*/
		/*TimeSeriesServlet.tsServletLog = PerformanceLog.getInstance(true, appMan, TimeSeriesServlet.class.getName()+"_TSS",
				new GwSubResProvider() {
					
					@Override
					public FloatResource getEventResource(MemoryTimeseriesPST device) {
						return device.pstTSServlet();
					}
					
					@Override
					public FloatResource getCounterResource(MemoryTimeseriesPST device) {
						return device.pstTSServletCounter();
					}
				});*/
	}
	public List<Datapoint> process(String tsProcessRequest, List<Datapoint> input);
	
	/** Regarding calculation notes see {@link TimeseriesSetProcMultiToSingle}
	 * 
	 * @param tsProcessRequest
	 * @param input
	 * @return
	 */
	public Datapoint processMultiToSingle(String tsProcessRequest, List<Datapoint> input);
	
	public Datapoint processSingle(String tsProcessRequest, Datapoint dp);
	
	/** Variant of {@link #processSingle(String, Datapoint)} allowing to pass an object containing parameters
	 * that shall be processed
	 * @param tsProcessRequest must reference to a TimeseriesSetProcessorArg (taking argument object)
	 * @param dp
	 * @param params
	 * @return
	 */
	public <T> Datapoint processSingle(String tsProcessRequest, Datapoint dp, T params);

	/** For processing of two or more aligned time series that are not interchangeable like difference, division,...*/
	public Datapoint processArgs(String tsProcessRequest, Datapoint... dp);

	public List<Datapoint> processSingleToMulti(String tsProcessRequest, Datapoint dp);

	public List<TimeSeriesData> processTSD(String tsProcessRequest, List<TimeSeriesData> input,
			TimeSeriesNameProvider nameProvider, AggregationModeProvider aggProv);
}