package org.ogema.devicefinder.util;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.Resource;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointInfo.AggregationMode;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButtonEval.TimeSeriesNameProvider;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class DPUtil {
	public static List<TimeSeriesData> getTSList(List<Datapoint> dpList, OgemaLocale locale) {
		List<TimeSeriesData> result = new ArrayList<>();
		for(Datapoint dp: dpList) {
			TimeSeriesDataImpl ts = dp.getTimeSeriesDataImpl(locale);
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
			boolean useDpService = false;
			if(aggModeProv != null && aggModeProv.getDpService() != null) {
				useDpService = aggModeProv.obtainFromStandardService(tsd.id());
			}
			Datapoint dp = getDP(tsd, useDpService?aggModeProv.getDpService():null);
			if(dp != null) {
				if(nameProvider != null && (tsd instanceof TimeSeriesDataExtendedImpl) &&
						dp.getGaroDataType() != null) {
					dp.setLabel(nameProvider.getShortNameForTypeI(dp.getGaroDataType(),
							(TimeSeriesDataExtendedImpl) tsd), null);
				}
				if(aggModeProv != null) {
					AggregationMode mode = aggModeProv.getMode(tsd.id());
					if(mode != null)
						dp.info().setAggregationMode(mode);	
					DPRoom room = aggModeProv.getRoom(tsd.id());
					if(room != null)
						dp.setRoom(room);
					Resource deviceRes = aggModeProv.getDeviceResource(tsd.id());
					if(deviceRes != null)
						dp.setDeviceResource(deviceRes);
				}
				result.add(dp);
			}
		}
		return result ;
	}
	
	public static Datapoint getDP(TimeSeriesData tsd) {
		return getDP(tsd, null);
	}
	public static Datapoint getDP(TimeSeriesData tsd, DatapointService dpService) {
		if(!(tsd instanceof TimeSeriesDataImpl))
			return null;
		Datapoint dp;
		if(dpService == null)
			dp = new DatapointImpl((TimeSeriesDataImpl)tsd);
		else {
			dp = dpService.getDataPointStandard(tsd.label(null));
			dp.setTimeSeries(((TimeSeriesDataImpl)tsd).getTimeSeries(), true);
		}
		return dp;
	}
	
	public static void copyExistingDataRoomDevice(Datapoint source, Datapoint dest) {
		//if(source.getDeviceResource() != null)
		//	dest.setDeviceResource(source.getDeviceResource());
		if(source.getRoom() != null)
			dest.setRoom(source.getRoom());
		if(source.getDevice() != null)
			dest.setDevice(source.getDevice());
	}
	
	public static void printDatapointsOfType(GaRoDataTypeI type, DatapointService dpService) {
		List<Datapoint> all = dpService.getAllDatapoints();
		for(Datapoint dp: all) {
			if(type == null || dp.getGaroDataType().equals(type)) {
				String message = "  "+dp.label(null)+" : "+dp.getGaroDataType().label(null)+" : "+dp.getLocation();
				System.out.println(message);
			}
		}
	}
}
