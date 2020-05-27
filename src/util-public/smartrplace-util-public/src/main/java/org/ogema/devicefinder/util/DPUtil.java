package org.ogema.devicefinder.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;

public class DPUtil {
	public static List<TimeSeriesData> getTSList(List<Datapoint> dpList) {
		List<TimeSeriesData> result = new ArrayList<>();
		for(Datapoint dp: dpList) {
			TimeSeriesDataImpl ts = dp.getTimeSeriesDataImpl();
			if(ts != null)
				result.add(ts);
		}
		return result ;
	}
	
	public static List<Datapoint> getDPList(List<TimeSeriesData> dpList, AggregationModeProvider aggModeProv) {
		return getDPList(dpList, null, aggModeProv);
	}
	public static List<Datapoint> getDPList(List<TimeSeriesData> dpList, TimeSeriesNameProvider nameProvider,
			AggregationModeProvider aggModeProv) {
		List<Datapoint> result = new ArrayList<>();
		for(TimeSeriesData tsd: dpList) {
			Datapoint dp = getDP(tsd);
			if(dp != null) {
				if(nameProvider != null && (tsd instanceof TimeSeriesDataExtendedImpl) &&
						dp.getGaroDataType() != null) {
					dp.setLabel(nameProvider.getShortNameForTypeI(dp.getGaroDataType(),
							(TimeSeriesDataExtendedImpl) tsd));
				}
				if(aggModeProv != null) {
					AggregationMode mode = aggModeProv.getMode(tsd.id());
					if(mode != null)
						dp.info().setAggregationMode(mode);					
				}
				result.add(dp);
			}
		}
		return result ;
	}
	
	public static Datapoint getDP(TimeSeriesData tsd) {
		if(!(tsd instanceof TimeSeriesDataImpl))
			return null;
		DatapointImpl dp = new DatapointImpl((TimeSeriesDataImpl)tsd);
		return dp;
	}
}
