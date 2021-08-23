package org.ogema.timeseries.eval.simple.api;

import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.util.DPRoomImpl;
import org.smartrplace.tissue.util.resource.ResourceHelperSP;

import de.iwes.util.resource.ValueResourceHelper;

public class TimeProcUtil {
	public static final long MINUTE_MILLIS = 60000;
	public static final long HOUR_MILLIS = 60*60000;
	public static final long DAY_MILLIS = 24*HOUR_MILLIS;
	public static final long YEAR_MILLIS = (long)(365.25*DAY_MILLIS);

	public static final String PER_DAY_EVAL = "DAY";
	public static final String PER_DAY_SUFFIX = "_proTag";
	public static final String PER_HOUR_EVAL = "HOUR";
	public static final String PER_15M_EVAL = "15Min";
	public static final String PER_15M_SUFFIX = "_per15Min";
	public static final String PER_MINUTE_EVAL = "MINUTE";
	public static final String PER_MINUTE_SUFFIX = "_perMinute";
	public static final String PER_HOUR_SUFFIX = "_proStunde";
	public static final String PER_MONTH_EVAL = "MONTH";
	public static final String PER_MONTH_SUFFIX = "_perMonth";
	public static final String PER_YEAR_EVAL = "YEAR";
	public static final String PER_YEAR_SUFFIX = "_perYear";
	public static final String SUM_PER_DAY_EVAL = "SUM_PER_DAY";
	public static final String SUM_PER_DAY_SUFFIX = "_total_sum";
	public static final String SUM_PER_HOUR_EVAL = "SUM_PER_HOUR";
	public static final String SUM_PER_HOUR_SUFFIX = "_total_sum_hour";
	public static final String SUM_PER_15M_EVAL = "SUM_PER_15MIN";
	public static final String SUM_PER_15M_SUFFIX = "_total_sum_15min";
	public static final String SUM_PER_MINUTE_EVAL = "SUM_PER_MINUTE";
	public static final String SUM_PER_MINUTE_SUFFIX = "_total_sum_minute";
	public static final String SUM_PER_MONTH_EVAL = "SUM_PER_MONTH";
	public static final String SUM_PER_MONTH_SUFFIX = "_total_sum_month";
	public static final String SUM_PER_YEAR_EVAL = "SUM_PER_YEAR";
	public static final String SUM_PER_YEAR_SUFFIX = "_total_sum_year";
	public static final String SUM_PER_DAY_PER_ROOM_EVAL = "DAY_PER_ROOM";
	public static final String SUM_PER_DAY_PER_ROOM_SUFFIX = "_sum";

	public static final String METER_EVAL = "METER";
	public static final String METER_SUFFIX = "_vm";
	
	/** Known suffixes from applications*/
	public static final String ALARM_GAP_SUFFIX = "_gap";
	public static final String ALARM_OUTVALUE_SUFFIX = "_outvalue";
	public static final String ALARM_SETPREACT_SUFFIX = "_setpreact";
	public static final String ALARM_VALCHANGED_SUFFIX = "_valchanged";
	public static final String BASE_GRADIENT_SUFFIX = "_gradient";
	
	public static TimeResource getDefaultMeteringReferenceResource(ResourceAccess resAcc ) {
		TimeResource refRes = null;
		if(!Boolean.getBoolean("org.ogema.timeseries.eval.simple.api.suppress_legacy_meteringreference"))
			refRes = ResourceHelperSP.getSubResource(null,
				"offlineEvaluationControlConfig/energyEvaluationInterval/initialTest/start",
				TimeResource.class, resAcc);
		if(refRes == null) {
			refRes = ResourceHelperSP.getSubResource(null, "offlineEvaluationControlConfig/defaultMeteringReference",
					TimeResource.class, resAcc);
		}
		return refRes;
	}
	public static boolean initDefaultMeteringReferenceResource(long referenceTime, boolean forceUpdate, ResourceAccess resAcc) {
		TimeResource ref = getDefaultMeteringReferenceResource(resAcc);
		if((!ref.isActive()) || forceUpdate) {
			ValueResourceHelper.setCreate(ref, referenceTime);
			return true;
		}
		return false;
	}
	
	
	public static final DPRoom unknownRoom = new DPRoomImpl(Datapoint.UNKNOWN_ROOM_ID,
			Datapoint.UNKNOWN_ROOM_NAME);
	

	/** Reference for meter building from power and Consumption2Meter values*/
	public static class MeterReference {
		public long referenceTime;
		public float referenceMeterValue;
	}
	
	protected static double interpolateTsValue(long start, long end, long ts, float valStart, float valEnd) {
		return valStart+interpolateTsStep(start, end, ts, valEnd-valStart);
	}
	public static double interpolateTsStep(long start, long end, long ts, float deltaVal) {
		return (((double)(ts-start))/(end-start))*deltaVal;
	}
	public static float getInterpolatedValue(ReadOnlyTimeSeries timeseries, long timestamp) {
		SampledValue sv = timeseries.getValue(timestamp);
		if(sv != null)
			return sv.getValue().getFloatValue();
		SampledValue svBefore = timeseries.getPreviousValue(timestamp);
		SampledValue svNext = timeseries.getNextValue(timestamp);
		if(svBefore == null || svNext == null)
			return Float.NaN;
		return (float) interpolateTsValue(svBefore.getTimestamp(), svNext.getTimestamp(),
				timestamp,
				svBefore.getValue().getFloatValue(), svNext.getValue().getFloatValue());
	}
	public static float getInterpolatedOrAvailableValue(ReadOnlyTimeSeries timeseries, long timestamp) {
		SampledValue sv = timeseries.getValue(timestamp);
		if(sv != null)
			return sv.getValue().getFloatValue();
		SampledValue svBefore = timeseries.getPreviousValue(timestamp);
		SampledValue svNext = timeseries.getNextValue(timestamp);
		if(svBefore == null || svNext == null) {
			if(svBefore != null)
				return svBefore.getValue().getFloatValue();
			if(svNext != null)
				return svNext.getValue().getFloatValue();
			return Float.NaN;
		}
		return (float) interpolateTsValue(svBefore.getTimestamp(), svNext.getTimestamp(),
				timestamp,
				svBefore.getValue().getFloatValue(), svNext.getValue().getFloatValue());
	}
	
	public static Integer getAbsoluteTiming(List<Datapoint> input) {
		for(Datapoint dp: input) {
			ReadOnlyTimeSeries ts = dp.getTimeSeries();
			if(ts != null && (ts instanceof ProcessedReadOnlyTimeSeries)) {
				ProcessedReadOnlyTimeSeries pts = (ProcessedReadOnlyTimeSeries) ts;
				if(pts.absoluteTiming != null)
					return pts.absoluteTiming;
			}
		}
		return null;
	}
	
	public static Float getMaxValue(Datapoint dp, long start, long end) {
		if(dp == null)
			return null;
		ReadOnlyTimeSeries ts = dp.getTimeSeries();
		if(ts == null)
			return null;
		return getMaxValue(ts, start, end);
	}
	public static Float getMaxValue(ReadOnlyTimeSeries ts, long start, long end) {
		List<SampledValue> svs = ts.getValues(start, end);
		Float max = null;
		for(SampledValue sv: svs) {
			float val = sv.getValue().getFloatValue();
			if(Float.isNaN(val))
				continue;	
			if(max == null || val > max)
				max = val;
		}
		return max;
	}
	
	public static SampledValue getFirstValueInLimit(ReadOnlyTimeSeries ts, long start, long end, float min, float max) {
		List<SampledValue> svs = ts.getValues(start, end);
		for(SampledValue sv: svs) {
			float val = sv.getValue().getFloatValue();
			if((min <= val) && (val <= max))
				return sv;	
		}
		return null;
	}

	public static boolean checkConsistency(List<SampledValue> values, String label) {
		long prevTime = 0;
		int idx = 0;
		for(SampledValue val: values) {
			if(val.getTimestamp() <= prevTime) {
				System.out.println("  !!!!! WARNING:: timeseries order error at idx:"+idx+" for "+(label!=null?label:"(unknown)"));
				return false;
			}
			prevTime = val.getTimestamp();
			idx++;
		}
		return true;
	}
}