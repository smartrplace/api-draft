package org.ogema.timeseries.eval.simple.mon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.DatapointDesc.ScalingProvider;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.util.BatteryEvalBase;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil.MeterReference;
import org.ogema.tools.resource.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.tissue.util.logconfig.PerformanceLog;
import org.smartrplace.util.frontend.servlet.ServletNumProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;

/** Note: This servlet is currently foreseen to be registered under the path of an implementing application. The
 * {@link TimeseriesBaseServlet} is registered under the path of the monitoringApp.
 * TODO: It could make sense to offer also this servlet under the path of the monitoringApp.
 *
 */
public class TimeSeriesServlet implements ServletPageProvider<TimeSeriesDataImpl> {
	public static PerformanceLog tsServletLog;

	public static final long ACCEPTED_PREVIOUS_VALUE_DISTANCE_FOR_DAY_EVAL = TimeProcUtil.HOUR_MILLIS*12;
	public static final double MILLIJOULE_TO_KWH = 0.001/TimeProcUtil.HOUR_MILLIS;
	public static final float JOULE_TO_KWH = (float) (1.0/TimeProcUtil.HOUR_MILLIS);
	
	Map<String, ReadOnlyTimeSeries> knownSpecialTs = new HashMap<>();
	protected final ApplicationManager appMan;

	public static final Logger log = LoggerFactory.getLogger(TimeSeriesServlet.class);

	/** Distance for last value after which interpolation will not take place */
	public static final long ACCEPTED_PREVIOUS_VALUE_DISTANCE_INTERPOLATED = 2*TimeProcUtil.DAY_MILLIS;

	public static final int MAX_SAMPLE_PER_EVAL = 50000;
	public static final int MIN_SAMPLE_PER_SPLITEVAL = MAX_SAMPLE_PER_EVAL/2;
	public TimeSeriesServlet(ApplicationManager appMan) {
		this.appMan = appMan;
	}

	@Override
	public Map<String, ServletValueProvider> getProviders(TimeSeriesDataImpl object, String user,
			Map<String, String[]> paramMap) {
		Map<String, ServletValueProvider> result = new LinkedHashMap<>();
		float val = specialEvaluation(object.label(null), object.getTimeSeries(), appMan, paramMap);
		ServletValueProvider last24h = new ServletNumProvider(val);
		result.put("last24h", last24h);
		/*if(object.label(null).equals("L24")) {
			float val = getDiffOfLast24h(object.getTimeSeries(), appMan);
			ServletValueProvider last24h = new ServletNumProvider(val);
			result.put("last24h", last24h);
		} else if(object.label(null).equals("I24")) {
			float val = getIntegralOfLast24h(object.getTimeSeries(), appMan);
			ServletValueProvider last24h = new ServletNumProvider(val);
			result.put("last24h", last24h);
		}*/

		return result;
	}

	@Override
	public Collection<TimeSeriesDataImpl> getAllObjects(String user) {
		List<TimeSeriesDataImpl> result = new ArrayList<TimeSeriesDataImpl>();
		for(ReadOnlyTimeSeries know: knownSpecialTs.values()) {
			result.add(new TimeSeriesDataImpl(know, "XXX_", "XXX_", null));
		}
		return result;
	}

	@Override
	public String getObjectId(TimeSeriesDataImpl objIn) {
		ReadOnlyTimeSeries obj = objIn.getTimeSeries();
		if(obj instanceof Schedule)
			return objIn.label(null)+"_S:"+ResourceUtils.getHumanReadableShortName((Schedule)obj);
		else if(obj instanceof RecordedData)
			return objIn.label(null)+"_R:"+((RecordedData)obj).getPath();
		else {
			for(Entry<String, ReadOnlyTimeSeries> e: knownSpecialTs.entrySet()) {
				if(e.getValue() == obj)
					return objIn.label(null)+"_X:"+e.getKey();
			}
		}
		return null;
	}
	
	@Override
	public TimeSeriesDataImpl getObject(String objectIdin) {
		String label = objectIdin.substring(0, 3);
		String objectId = objectIdin.substring(4);
		if(objectId.startsWith("S:"))
			return new TimeSeriesDataImpl(appMan.getResourceAccess().getResource(objectId.substring(2)), label, label, null);
		else if(objectId.startsWith("R:")) {
			SingleValueResource svr = appMan.getResourceAccess().getResource(objectId.substring(2));
			return new TimeSeriesDataImpl(ValueResourceHelper.getRecordedData(svr), label, label, null);
		} else if(objectId.startsWith("X:"))
			return new TimeSeriesDataImpl(knownSpecialTs.get(objectId), label, label, null);
		//use L24_R: as default
		SingleValueResource svr = appMan.getResourceAccess().getResource(objectIdin);
		return new TimeSeriesDataImpl(ValueResourceHelper.getRecordedData(svr), "L24", "L24", null);		
		//hrow new IllegalArgumentException("ObjectId must declare a known type (S:,R: or X:), is:"+objectId);
	}
	
	public static float getIntegralOfLast24h(ReadOnlyTimeSeries ts, ApplicationManager appMan) {
		long now = appMan.getFrameworkTime();
		return (float) (integrateSimple(ts, now-TimeProcUtil.DAY_MILLIS, now)*MILLIJOULE_TO_KWH); // /TimeProcUtil.HOUR_MILLIS);
		//return (float) (TimeSeriesUtils.integrate(ts, now-TimeProcUtil.DAY_MILLIS, now)/TimeProcUtil.HOUR_MILLIS);
	}
	/** This method is only applicable for AggregationMode.Meter2Meter*/
	public static float getDiffOfLast24h(ReadOnlyTimeSeries ts, ApplicationManager appMan) {
		return getDiffOfLast24h(ts, Boolean.getBoolean("org.smartrplace.app.monbase.dointerpolate"), appMan);
	}
	/** This method is only applicable for AggregationMode.Meter2Meter*/
	public static float getDiffOfLast24h(ReadOnlyTimeSeries ts, boolean interpolate, ApplicationManager appMan) {
		long now = appMan.getFrameworkTime();
		long start = now-TimeProcUtil.DAY_MILLIS;
		SampledValue startval = ts.getPreviousValue(start);
		if(startval == null || start - startval.getTimestamp() > ACCEPTED_PREVIOUS_VALUE_DISTANCE_FOR_DAY_EVAL) {
			return -1;
		}
		SampledValue endval = ts.getPreviousValue(now);
		if(endval == null)
			return -1;
			//return Float.NaN;
		try {
		return endval.getValue().getFloatValue() - startval.getValue().getFloatValue();
		} catch(NullPointerException e) {
			e.printStackTrace();
			return -2;
		}
	}
	
	/** This method is only applicable for AggregationMode.Meter2Meter*/
	public static float getDiffForDay(long timeStamp, ReadOnlyTimeSeries ts) {
		return getDiffForDay(timeStamp, ts, Boolean.getBoolean("org.smartrplace.app.monbase.dointerpolate"));
	}
	/** This method is only applicable for AggregationMode.Meter2Meter*/
	public static float getDiffForDay(long timeStamp, ReadOnlyTimeSeries ts,
			boolean interpolate) {
		return getDiffForDay(timeStamp, ts, interpolate, AbsoluteTiming.DAY);
	}
	public static float getDiffForDay(long timeStamp, ReadOnlyTimeSeries ts,
			boolean interpolate, int intervalType) {
		long start = AbsoluteTimeHelper.getIntervalStart(timeStamp, intervalType);
		long end = start + AbsoluteTimeHelper.getStandardInterval(intervalType); //TimeProcUtil.DAY_MILLIS;
		final float startFloat;
		final float endFloat;
		float startFloat1 = -1;
		float endFloat1 = -1;
		if(interpolate) {
			startFloat1 = TimeProcUtil.getInterpolatedValue(ts, start);
			endFloat1 = TimeProcUtil.getInterpolatedValue(ts, end);			
		}
		SampledValue startval = ts.getPreviousValue(start);
		long startDistance = startval==null?Long.MAX_VALUE:(start - startval.getTimestamp());
		long specificAcceptedDistance = AbsoluteTimeHelper.getStandardInterval(intervalType)/2;
		if((!interpolate) || Float.isNaN(startFloat1) || Float.isNaN(endFloat1) ||
				(!allowInterpolation(startDistance, intervalType, specificAcceptedDistance))) {
			if(startval == null || (startDistance > specificAcceptedDistance)) { //ACCEPTED_PREVIOUS_VALUE_DISTANCE_FOR_DAY_EVAL)) {
				return Float.NaN; //-1
			}
			SampledValue endval = ts.getPreviousValue(end);
			if(endval == null)
				return Float.NaN;
				//return Float.NaN;
			try {
				startFloat = startval.getValue().getFloatValue();
				endFloat = endval.getValue().getFloatValue();
			} catch(NullPointerException e) {
				e.printStackTrace();
				return -2;
			}
		} else {
			startFloat = startFloat1;
			endFloat = endFloat1;
		}
			
		return endFloat - startFloat;
	}
	
	public static boolean allowInterpolation(long lastValDistance, int intervalType, long specificAcceptedDistance) {
		if(lastValDistance < ACCEPTED_PREVIOUS_VALUE_DISTANCE_INTERPOLATED)
			return true;
		if(lastValDistance < specificAcceptedDistance)
			return true;
		return false;
	}
	
	/** This method is only applicable for AggregationMode.Meter2Meter*/
	public static float getDiffForDayOrLast24(long timeStamp, ReadOnlyTimeSeries ts,
			ApplicationManager appMan) {
		return getDiffForDayOrLast24(timeStamp, ts, Boolean.getBoolean("org.smartrplace.app.monbase.dointerpolate"), appMan);
	}
	/** This method is only applicable for AggregationMode.Meter2Meter*/
	public static float getDiffForDayOrLast24(long timeStamp, ReadOnlyTimeSeries ts,
			boolean interpolate, ApplicationManager appMan) {
		long now = appMan.getFrameworkTime();
		long start = AbsoluteTimeHelper.getIntervalStart(timeStamp, AbsoluteTiming.DAY);		
		long end = start + TimeProcUtil.DAY_MILLIS;
		if(now>=start && now<=end)
			return getDiffOfLast24h(ts, interpolate, appMan);
		else
			return getDiffForDay(timeStamp, ts, interpolate);
	}
	
	public static float specialEvaluation(String label, ReadOnlyTimeSeries timeSeries, ApplicationManager appMan,
			Map<String, String[]> paramMap) {
		if(label.equals("L24")) {
			String[] timearr = paramMap.get("time");
			long ts;
			if(timearr == null)
				ts = appMan.getFrameworkTime();
			else
				ts = Long.parseLong(timearr[0]);
			return getDiffForDayOrLast24(ts, timeSeries, appMan);
		} else	if(label.equals("D24")) {
			long ts = Long.parseLong(paramMap.get("time")[0]);
			return getDiffForDay(ts, timeSeries);
		} else if(label.equals("I24")) {
			return getIntegralOfLast24h(timeSeries, appMan);
		}
		return Float.NaN;
	}
	
	public static class Power2MeterPrevValues {
		Float value;
		long timestamp;
	}
	
	public static MeterReference getDefaultMeteringReference(ReadOnlyTimeSeries timeSeries, Long defaultTime, ApplicationManager appMan) {
		MeterReference ref = new MeterReference();
		ref.referenceMeterValue = 0;
		if(timeSeries instanceof RecordedData) {
			Resource parent = appMan.getResourceAccess().getResource(((RecordedData)timeSeries).getPath());
			if(parent != null) {
				FloatResource refVal = parent.getSubResource("refTimeCounter", FloatResource.class);
				if(refVal.exists())
					ref.referenceMeterValue = refVal.getValue();
				else {
					Resource device = ResourceHelper.getFirstParentOfType(parent, "org.smartrplace.iotawatt.ogema.resources.IotaWattElectricityConnection");
					if(device != null) {
						refVal = device.getSubResource("refTimeCounter", FloatResource.class);
						if(refVal.exists())
							ref.referenceMeterValue = refVal.getValue();
					}
				}
			}
		}
		TimeResource refRes = TimeProcUtil.getDefaultMeteringReferenceResource(appMan.getResourceAccess());
		if(!refRes.exists()) {
			refRes.create();
			if(defaultTime == null)
				defaultTime = Long.getLong("org.ogema.timeseries.eval.simple.api.meteringreferencetime");
			if(defaultTime != null)
				refRes.setValue(defaultTime);
			else
				refRes.setValue(appMan.getFrameworkTime());
			refRes.activate(false);
		}
		ref.referenceTime = refRes.getValue();
		return ref;
	}
	/** Calculate a virtual meter series that has the same counter value at a reference point as another
	 * real meter so that the further consumption trend can be compared directly
	 * Note: Currently this method only supports interpolation
	 * TODO: param resultSerie especially for mode Power2Meter we would like to get a reference on a preexisting result
	 * 		series to avoid a recalculation all the time. For now we re-calculate every time. If the value of the reference
	 * 		time changes existing data is still not recalculated. The datapoint service app has to be restarted for this.
	 * TODO: For Power2Meter we should generate a warning regarding value gaps, for now we just use the last value for the entire
	 * 		interval until the next value is available.
	 * @return
	 */
	public static List<SampledValue> getMeterFromConsumption(ReadOnlyTimeSeries timeSeries, long start, long end,
			MeterReference ref, AggregationMode mode) {
			//ReadOnlyTimeSeries resultSeries) {
		List<SampledValue> result = new ArrayList<>();
		final double myRefValue;
		final double delta;
		Power2MeterPrevValues prevVal = null;
		if(mode == AggregationMode.Power2Meter)
			prevVal = new Power2MeterPrevValues();
		if(mode == AggregationMode.Consumption2Meter || mode == AggregationMode.Power2Meter) {
			long startLoc;
			long endLoc;
			if(ref.referenceTime > start) {
				if(mode == AggregationMode.Power2Meter) {
					SampledValue sv = timeSeries.getNextValue(0);
					if(sv == null)
						return Collections.emptyList();
					startLoc = start = sv.getTimestamp();
				} else
					startLoc = start;
				endLoc = ref.referenceTime;
			} else {
				startLoc = ref.referenceTime;
				endLoc = start;			
			}
			double counter = aggregateValuesForMeter(timeSeries, mode, startLoc, endLoc, prevVal, null, 0);
			if(ref.referenceTime > start)
				myRefValue = counter;
			else
				myRefValue = -counter;
			delta = ref.referenceMeterValue - myRefValue;
			if(mode == AggregationMode.Power2Meter && (ref.referenceTime > start))
				prevVal = new Power2MeterPrevValues();
			aggregateValuesForMeter(timeSeries, mode, start, end, prevVal, result, delta);
			return result;
		} else {
			myRefValue = TimeProcUtil.getInterpolatedValue(timeSeries, ref.referenceTime);
		}
		if(Double.isNaN(myRefValue))
			return Collections.emptyList();
		delta = ref.referenceMeterValue - myRefValue;
		double counter = 0;
		List<SampledValue> tsInput = timeSeries.getValues(start, end);
		for(SampledValue sv: tsInput) {
			counter = sv.getValue().getFloatValue();
			result.add(new SampledValue(new FloatValue((float) (counter+delta)), sv.getTimestamp(), sv.getQuality()));
		}
		return result;
	}
	public static List<SampledValue> getDayValues(ReadOnlyTimeSeries timeSeries, long start, long end,
			AggregationMode mode, ScalingProvider scale) {
		return getDayValues(timeSeries, start, end, mode, scale,
				Boolean.getBoolean("org.smartrplace.app.monbase.dointerpolate"), AbsoluteTiming.DAY);
	}
	public static List<SampledValue> getDayValues(ReadOnlyTimeSeries timeSeries, long start, long end,
			AggregationMode mode, ScalingProvider scale, boolean interpolate, int intervalType) {
		//if(mode == AggregationMode.Power2Meter)
		//	throw new UnsupportedClassVersionError("Power2Meter not supported yet");
		long nextDayStart = AbsoluteTimeHelper.getIntervalStart(start, intervalType);
		/*float prevCounter;
		switch(mode) {
		case Meter2Meter:
			prevCounter = getInterpolatedValue(timeSeries, startDay);
			break;
		default:
			prevCounter = 0;
		}*/
		List<SampledValue> result = new ArrayList<>();
//long startCalc = System.currentTimeMillis();
		while(nextDayStart <= end) {
			long startCurrentDay = nextDayStart;
			nextDayStart = AbsoluteTimeHelper.addIntervalsFromAlignedTime(nextDayStart, 1, intervalType);
			float newDayVal;
			if(mode == AggregationMode.Meter2Meter) {
				newDayVal = getDiffForDay(startCurrentDay, timeSeries, interpolate, intervalType);
			} else {
				float newCounter = TimeProcUtil.getInterpolatedValue(timeSeries, startCurrentDay);
				switch(mode) {
				//case Meter2Meter:
				//	newDayVal = newCounter - prevCounter;
				//	prevCounter = newCounter;
				//	break;
				case Power2Meter:
					//TODO: not really tested => integrate from kW*(milliseconds) to kWh
					//newDayVal = (float) (TimeSeriesUtils.integrate(
					newDayVal = (float) (integrateSimple(
							timeSeries, startCurrentDay, nextDayStart)*MILLIJOULE_TO_KWH); // /TimeProcUtil.HOUR_MILLIS);
					break;
				case Consumption2Meter:
					double counter = 0; //getPartialConsumptionValue(timeSeries, startCurrentDay, true);
					List<SampledValue> svList = timeSeries.getValues(startCurrentDay, nextDayStart);
					for(SampledValue sv: svList) {
						counter += sv.getValue().getFloatValue();
					}
					//TODO: correct start / end value usage
					//counter += getPartialConsumptionValue(timeSeries, end, false);
					newDayVal = (float) counter;
					break;
				default:
					newDayVal = newCounter;
				}
			}
			if(scale != null)
				result.add(new SampledValue(new FloatValue(scale.getStdVal(newDayVal, startCurrentDay)), startCurrentDay, Quality.GOOD));
			else
				result.add(new SampledValue(new FloatValue(newDayVal), startCurrentDay, Quality.GOOD));
//long endCalc = System.currentTimeMillis();
//if(tsServletLog != null) tsServletLog.logEvent(endCalc-startCalc, "Calculation of DAY+"+StringFormatHelper.getDateInLocalTimeZone(startCurrentDay)+"  took");
//startCalc = endCalc;
		}
		return result;
	}
	
	/** For Consumption2Meter time series get energy consumption from timestamp to the end of the
	 * interval within timestamp or from the start of the interval until timestamp<br>
	 * Note that the interval is defined by the values available in the timeseries, so this is
	 * mainly intended for manual timeseries e.g. with one value per day
	 * @param timeseries
	 * @param timestamp
	 * @param getConsumptionTowardsEnd if true the energy consumption from timestamp to the end of
	 * the current interval is returned, otherwise the energy consumption from the start of the current
	 * interval until timestamp
	 * @return
	 */
	protected static float getPartialConsumptionValue(ReadOnlyTimeSeries timeseries, long timestamp,
			boolean getConsumptionTowardsEnd) {
		SampledValue sv = timeseries.getValue(timestamp);
		if(sv != null)
			return 0;
		SampledValue svBefore = timeseries.getPreviousValue(timestamp);
		SampledValue svNext = timeseries.getNextValue(timestamp);
		if(svBefore == null || svNext == null)
			return 0;
		// Part of consumption represented by the value at the end of the interval that is used until timestamp
		if(svBefore.getTimestamp() == svNext.getTimestamp()) {
			System.out.println("This should neven occur as we tested for a value on the exact timestamp before!");
			return 0;
		}
		float partialVal = (float) TimeProcUtil.interpolateTsStep(svBefore.getTimestamp(), svNext.getTimestamp(),
				timestamp,
				svNext.getValue().getFloatValue());
		if(getConsumptionTowardsEnd)
			return svNext.getValue().getFloatValue() - partialVal;
		else
			return partialVal;
	}
	
	/**
	 * 
	 * @param internalVals
	 * @param sv
	 * @param evalStart required for initial step. The first value will be integrated not from its timstamp, but
	 * 		from the start of the evaluation as the real value before the first value usually is not in the
	 * 		integration
	 * @return
	 */
	protected static double getPowerStep(Power2MeterPrevValues internalVals, SampledValue sv, long evalStart) {
		double result;
		if(internalVals.value != null) {
			result = (internalVals.value * (sv.getTimestamp() - internalVals.timestamp)) * MILLIJOULE_TO_KWH;
			internalVals.timestamp = sv.getTimestamp();
		} else {
			result = 0;
			internalVals.timestamp = evalStart;
		}
		internalVals.value = sv.getValue().getFloatValue();
		return result;
	}
	protected static double getFinalPowerStep(Power2MeterPrevValues internalVals, long evalEnd) {
		double result;
		if(internalVals.value != null) {
			result = (internalVals.value * (evalEnd - internalVals.timestamp)) * MILLIJOULE_TO_KWH;
		} else {
			result = 0;
		}
		return result;		
	}
	
	protected static double aggregateValuesForMeter(ReadOnlyTimeSeries timeSeries, AggregationMode mode,
			long startLoc, long endLoc, Power2MeterPrevValues prevVal,
			 List<SampledValue> result, double delta) {
		double counter;
		if(mode == AggregationMode.Consumption2Meter) {
			counter = getPartialConsumptionValue(timeSeries, startLoc, true);
			if(Double.isNaN(counter))
				log.warn("NAN for agg from"+timeSeries.toString()+" at "+StringFormatHelper.getFullTimeDateInLocalTimeZone(startLoc));
		} else {
			SampledValue svBefore = timeSeries.getPreviousValue(startLoc);
			SampledValue svFirst = timeSeries.getNextValue(startLoc);
			long firstTs;
			if(svFirst == null || svFirst.getTimestamp() > endLoc)
				firstTs = endLoc;
			else
				firstTs = svFirst.getTimestamp();
			if(svBefore != null && (!Float.isNaN(svBefore.getValue().getFloatValue())))
				counter = svBefore.getValue().getFloatValue()*(firstTs - startLoc) * MILLIJOULE_TO_KWH;
			else
				counter = 0;
		}
		final List<SampledValue> svList;
		svList = timeSeries.getValues(startLoc, endLoc);
if(timeSeries instanceof RecordedData) {
	RecordedData rec = (RecordedData) timeSeries;
	log.error("Read from "+rec.getPath()+" values:"+svList.size());
} else
	log.error("Read from no-RecordedData values:"+svList.size()+" : "+timeSeries.toString());
log.error("From "+StringFormatHelper.getFullTimeDateInLocalTimeZone(startLoc)+" to "+StringFormatHelper.getFullTimeDateInLocalTimeZone(endLoc));
		for(SampledValue sv: svList) {
			if(Float.isNaN(sv.getValue().getFloatValue()))
				continue;
			if(mode == AggregationMode.Power2Meter)
				counter += getPowerStep(prevVal, sv, startLoc);
			else
				counter += sv.getValue().getFloatValue();
			if(result != null) {
				result.add(new SampledValue(new FloatValue((float) (counter+delta)), sv.getTimestamp(), sv.getQuality()));
			}
			if(Double.isNaN(counter))
				log.warn("NAN for agg from"+timeSeries.toString()+" at "+StringFormatHelper.getFullTimeDateInLocalTimeZone(startLoc));
		}
		if(mode == AggregationMode.Power2Meter) {
			SampledValue firstValueBehind = timeSeries.getNextValue(endLoc);
			if(firstValueBehind != null)
				counter += getFinalPowerStep(prevVal, endLoc);
			if(result != null)
				result.add(new SampledValue(new FloatValue((float) (counter+delta)), prevVal.timestamp, Quality.GOOD));
			if(Double.isNaN(counter))
				log.warn("NAN for agg from"+timeSeries.toString()+" at "+StringFormatHelper.getFullTimeDateInLocalTimeZone(startLoc));
		} else if(result == null)
			counter += getPartialConsumptionValue(timeSeries, endLoc, false);
		return counter;
	}
	
	/**
	 * @param timeseries
	 * @param start
	 * @param end
	 * @return
	 * 		integral, with time measured in ms
	 */
	public static double integrateSimple(ReadOnlyTimeSeries timeseries, long start, long end) {
		double sum = 0;
		
		Power2MeterPrevValues prevVal = new Power2MeterPrevValues();
		sum += aggregateValuesForMeter(timeseries, AggregationMode.Power2Meter, start, end, prevVal , null, 0);
		return sum;
	}

	protected static long getEndBelowMaxSample(ReadOnlyTimeSeries timeSeries, long start, long curEnd, int curSize) {
		if(curSize < MAX_SAMPLE_PER_EVAL)
			return curEnd;
		double factor = ((double)MAX_SAMPLE_PER_EVAL)/curSize;
		if(factor > 0.5)
			factor = 0.5;
		long newEnd = (long) (factor*(curEnd-start))+start;
		int newSize = timeSeries.size(start, newEnd);
		while((newSize < MIN_SAMPLE_PER_SPLITEVAL) && (factor < 0.5)) {
			factor *= 2;
			newEnd = (long) (factor*(curEnd-start))+start;
			newSize = timeSeries.size(start, newEnd);
		}
		return getEndBelowMaxSample(timeSeries, start, newEnd, newSize);
	}
	
	/** Return gap durations in minutes as FloatValues*/
	/*public static List<SampledValue> getGaps(ReadOnlyTimeSeries timeSeries, long start, long end, long maxGapSize,
			Integer absoluteTiming) {
		int svNum = timeSeries.size(start, end+1);
		if(svNum > MAX_SAMPLE_PER_EVAL) {
			List<SampledValue> result = new ArrayList<>();
			long partEnd = getEndBelowMaxSample(timeSeries, start, end, svNum);
			long partStart = start;
			while(partEnd < end) {
				List<SampledValue> input = timeSeries.getValues(partStart, partEnd+1);
				result.addAll(getGaps(input, partStart, partEnd, maxGapSize));
				if(absoluteTiming != null) {
					partStart = AbsoluteTimeHelper.getIntervalStart(partEnd, absoluteTiming);
				} else
					partStart = partEnd;
				partEnd = getEndBelowMaxSample(timeSeries, partStart, end, svNum);
			}
		}
		List<SampledValue> input = timeSeries.getValues(start, end+1);
		return getGaps(input, start, end, maxGapSize);
	}
	public static List<SampledValue> getGaps(ReadOnlyTimeSeries timeSeries, long start, long end, long maxGapSize) {
		return getGaps(timeSeries, start, end, maxGapSize, AbsoluteTiming.WEEK);
	}
	public static List<SampledValue> getGaps(List<SampledValue> input, long start, long end, long maxGapSize) {*/
	public static List<SampledValue> getGaps(ReadOnlyTimeSeries timeSeries, long start, long end, long maxGapSize) {
		List<SampledValue> result = new ArrayList<>();
		SampledValue lastVal = null;
if(Boolean.getBoolean("evaldebug2")) System.out.println("Processing "+timeSeries.size(start, end)+" input timestamps in GAPs");
		if(timeSeries.isEmpty() && ((end-start) > maxGapSize)) {
			result.add(new SampledValue(new FloatValue((float)((double)(end-start)/TimeProcUtil.MINUTE_MILLIS)), end, Quality.GOOD));
			return result;
		}
		Iterator<SampledValue> it = timeSeries.iterator(start, end);
		while(it.hasNext()) {
			SampledValue sv = it.next();
		//for(SampledValue sv: input) {
			if(lastVal == null)
				lastVal = sv;
			else {
				long gap = sv.getTimestamp() - lastVal.getTimestamp();
				if(gap > maxGapSize)
					result.add(new SampledValue(new FloatValue((float)((double)gap/TimeProcUtil.MINUTE_MILLIS)), sv.getTimestamp(), Quality.GOOD));
				lastVal = sv;
			}
		}
		return result;
	}

	public static List<SampledValue> getOutValues(ReadOnlyTimeSeries timeSeries, long start, long end, float lowerLimit,
			float upperLimit, long maxOutTime) {
		List<SampledValue> input = timeSeries.getValues(start, end+1);
		List<SampledValue> result = new ArrayList<>();
		long lastValidTime = -1;
		boolean hasViolation = false;
		for(SampledValue sv: input) {
			float val = sv.getValue().getFloatValue();
			if(lastValidTime < 0) {
				if(isViolated(val, lowerLimit, upperLimit)) {
						lastValidTime = start;
						hasViolation = true;
				} else
					lastValidTime = sv.getTimestamp();
			} else {
				if(isViolated(val, lowerLimit, upperLimit)) {
					if(!hasViolation)
						hasViolation = true;
				} else {
					if(hasViolation) {
						//Violation ends
						hasViolation = false;
						long gap = sv.getTimestamp() - lastValidTime;
						result.add(new SampledValue(new FloatValue((float)((double)gap/TimeProcUtil.MINUTE_MILLIS)), sv.getTimestamp(), Quality.GOOD));
					}
					lastValidTime = sv.getTimestamp();
				}
			}
		}
		if(hasViolation) {
			//Violation ends
			long gap = end - lastValidTime;
			result.add(new SampledValue(new FloatValue((float)((double)gap/TimeProcUtil.MINUTE_MILLIS)), end, Quality.GOOD));
		}
		return result;
	}
	
	public static boolean isViolated(float value, float lower, float upper) {
		if(value < lower) return true;
		if(value > upper) return true;
		return false;
	}	
	
	/** Return setpoint reaction gap durations in minutes as FloatValues*/
	public static List<SampledValue> getSensReact(ReadOnlyTimeSeries setpReq, ReadOnlyTimeSeries setpFb, long start, long end, long maxReactTime) {
		List<SampledValue> req = setpReq.getValues(start, end+1);
		//List<SampledValue> fb = setpFb.getValues(start, end+1);
		List<SampledValue> result = new ArrayList<>();
		//SampledValue lastVal = null;
		//if(input.isEmpty() && ((end-start) > maxGapSize)) {
		//	result.add(new SampledValue(new FloatValue((float)((double)(end-start)/TimeProcUtil.MINUTE_MILLIS)), end, Quality.GOOD));
		//	return result;
		//}
		int idxNext = 1;
		int idx = 0;
		NextReqValResult nextDiffResult = null;
		for(SampledValue sv: req) {
			SampledValue fbVal = setpFb.getNextValue(sv.getTimestamp());
			nextDiffResult = getNextReqDiffVal(sv, req, idx);
			while((fbVal != null) && (Math.abs(sv.getValue().getFloatValue() - fbVal.getValue().getFloatValue()) > 0.1f) &&
					((nextDiffResult.svNext == null) || (fbVal.getTimestamp() < nextDiffResult.svNext.getTimestamp())) ) {
				fbVal = setpFb.getNextValue(fbVal.getTimestamp()+1);
			}
			if(fbVal == null) {
				long gap = end - sv.getTimestamp();
				if(gap > maxReactTime)
					result.add(new SampledValue(new FloatValue((float)((double)gap/TimeProcUtil.MINUTE_MILLIS)), sv.getTimestamp(), Quality.GOOD));
				break;
			}
			long gap = fbVal.getTimestamp() - sv.getTimestamp();
			if(gap > maxReactTime)
				result.add(new SampledValue(new FloatValue((float)((double)gap/TimeProcUtil.MINUTE_MILLIS)), sv.getTimestamp(), Quality.GOOD));
			idx++;
			if(idxNext >= idx)
				idxNext++;
		}
		return result;
	}

	protected static class NextReqValResult {
		SampledValue svNext;
		int idx;
	}
	/** Get next setpoint request with a different value requested. From this point on we accept any feedback as we cannot 
	 * expect to receive the original value anymore
	 * @param sv
	 * @param req
	 * @param idx
	 * @return
	 */
	protected static NextReqValResult getNextReqDiffVal(SampledValue sv, List<SampledValue> req, int idx) {
		NextReqValResult result = new NextReqValResult();
		result.idx = idx+1;
		while(result.idx < req.size()) {
			result.svNext = req.get(result.idx);
			if(Math.abs(sv.getValue().getFloatValue() - result.svNext.getValue().getFloatValue()) > 0.1f) {
				return result;
			}
		}
		result.svNext = null;
		return result ;
	}
	
	public static List<SampledValue> getValueChanges(ReadOnlyTimeSeries timeSeries, long start, long end,
			Float minChange, boolean addFirstNonNaNValue) {
		List<SampledValue> input = timeSeries.getValues(start, end+1);
		List<SampledValue> result = new ArrayList<>();
		Float lastVal = null;
		for(SampledValue sv: input) {
			if(lastVal == null || Float.isNaN(lastVal)) {
				lastVal = sv.getValue().getFloatValue();
				if(addFirstNonNaNValue && (!Float.isNaN(lastVal)))
					result.add(sv);
			} else {
				float newVal = sv.getValue().getFloatValue();
				if(Float.isNaN(newVal))
					continue;
				if(newVal != lastVal && (minChange == null || (Math.abs(newVal - lastVal) > minChange))) {
					result.add(sv);
				}
				lastVal = newVal;
			}
		}
		return result;

	}

	public static class BatteryEvalResult {
		public List<SampledValue> remainingLifeTime = new ArrayList<>();
		public List<SampledValue> voltageMinimal = new ArrayList<>();
	}
	/** Estimate remaining life time of a battery each time the voltage drops permanently. A new battery is indicated by
	 * a value of -10
	 * @param timeSeries batteryVoltage measurements for a device
	 * @param start
	 * @param end
	 * @param addFirstNonNaNValue
	 * @return
	 */
	public static BatteryEvalResult getBatteryRemainingLifetimeEstimation(ReadOnlyTimeSeries timeSeries, long start, long end,
			boolean addFirstNonNaNValue) {
		//List<SampledValue> input = timeSeries.getValues(start, end+1);
		BatteryEvalResult result = new BatteryEvalResult();
		SampledValue lastUpSv = null;
		if(Boolean.getBoolean("evaldebug2")) System.out.println("Processing "+timeSeries.size(start, end)+" input timestamps in BATtery");
		Iterator<SampledValue> it = timeSeries.iterator(start, end);
		while(it.hasNext()) {
			SampledValue sv = it.next();
		//for(SampledValue sv: input) {
			if(lastUpSv == null) {
				lastUpSv = sv;
			} else if(Float.isNaN(lastUpSv.getValue().getFloatValue())) {
				lastUpSv = sv;
			} else {
				float lastUpVal = lastUpSv.getValue().getFloatValue();
				float newVal = sv.getValue().getFloatValue();
				if(Float.isNaN(newVal))
					continue;
				if(newVal == lastUpVal) {
					lastUpSv = sv;
					continue;
				} else if(newVal > lastUpVal) {
					if((lastUpVal >= 2.9f && (newVal - lastUpVal > 0.15f))
							|| (lastUpVal >= 2.6f && (newVal - lastUpVal > 0.25f))
							|| (newVal - lastUpVal > 0.35f)) {
						SampledValue resultsv = new SampledValue(new FloatValue(-10), sv.getTimestamp(), Quality.GOOD);
						result.remainingLifeTime.add(resultsv);
						result.voltageMinimal.add(new SampledValue(new FloatValue(lastUpVal), lastUpSv.getTimestamp(), Quality.GOOD));
						result.voltageMinimal.add(new SampledValue(new FloatValue(newVal), sv.getTimestamp(), Quality.GOOD));
						lastUpSv = sv;
					}
				} else { //smaller
					boolean isEndOfLevel;
					if(lastUpVal - newVal > 0.15f)
						isEndOfLevel = true;
					else {
						long dist = sv.getTimestamp() - lastUpSv.getTimestamp();
						isEndOfLevel = (dist > 7*TimeProcUtil.DAY_MILLIS);
					}
					if(isEndOfLevel) {
						float days = (float) (((double)BatteryEvalBase.getRemainingLifeTimeEstimation(lastUpVal))/TimeProcUtil.DAY_MILLIS);
						SampledValue resultsv = new SampledValue(new FloatValue(days), sv.getTimestamp(), Quality.GOOD);
						result.remainingLifeTime.add(resultsv);
						result.voltageMinimal.add(new SampledValue(new FloatValue(lastUpVal), lastUpSv.getTimestamp(), Quality.GOOD));
						result.voltageMinimal.add(new SampledValue(new FloatValue(newVal), sv.getTimestamp(), Quality.GOOD));
						lastUpSv = sv;
					}
				}
			}
		}
		return result;

	}

}
