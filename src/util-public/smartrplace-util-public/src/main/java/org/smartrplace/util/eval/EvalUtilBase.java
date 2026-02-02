package org.smartrplace.util.eval;

import java.util.List;

import org.ogema.core.channelmanager.measurements.Quality;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;

public class EvalUtilBase {
	/** Evaluation goes back from current time and checks whether the value is larger then threshold. The number of
	 * minutes gone back from now is the result of this method.
	 * @param max go back at most this many minutes.
	 * @return minutes passed since timeseries values were bigger than threshold. Returns -1 if no value larger then threshold is found
	 */
	public static float minutesSinceLarger(ReadOnlyTimeSeries ts, float threshold, int max) {
		return minutesSinceLarger(ts, threshold, max, System.currentTimeMillis());
	}
	public static float minutesSinceLarger(ReadOnlyTimeSeries ts, float threshold, int max, long start) {
		List<SampledValue> l = ts.getValues(start - (((long)max) * 60 * 1000), start);
		for (int i = l.size()-1; i > -1; i--) {
			SampledValue sv = l.get(i);
			if (sv.getQuality() == Quality.BAD) {
				continue;
			}
			if (sv.getValue().getFloatValue() > threshold) {
				return (float) ((start - sv.getTimestamp()) / 60000d);
			}
		}
		return -1;
	}
	
	public static long firstLargerTimeBackwardsSafe(ReadOnlyTimeSeries ts, float threshold, int maxMinutes, long start) {
		float minutesBack = minutesSinceLarger(ts, threshold, maxMinutes, start);
		if(minutesBack < 0)
			return -1;
		return (start - (long)(minutesBack*TimeProcUtil.MINUTE_MILLIS));
	}
	
}
