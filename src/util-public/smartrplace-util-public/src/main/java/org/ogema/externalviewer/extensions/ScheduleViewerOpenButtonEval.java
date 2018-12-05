package org.ogema.externalviewer.extensions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.externalviewer.extensions.DefaultDedicatedTSSessionConfiguration;
import org.ogema.externalviewer.extensions.DefaultScheduleViewerConfigurationProviderExtended;
import org.ogema.externalviewer.extensions.DefaultTimeSeriesFilterExtended;
import org.ogema.externalviewer.extensions.ScheduleViewerOpenButton;
import org.smartrplace.util.format.StringListFormatUtils;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.TimeSeriesDataOffline;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationBuilder;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilter;

public abstract class ScheduleViewerOpenButtonEval extends ScheduleViewerOpenButton {
	private static final long serialVersionUID = 1L;

	protected abstract List<TimeSeriesData> getTimeseries(OgemaHttpRequest req);
	protected abstract String getEvaluationProviderId(OgemaHttpRequest req);
	protected abstract IntervalConfiguration getITVConfiguration(OgemaHttpRequest req);
	
	public ScheduleViewerOpenButtonEval(WidgetPage<?> page, String widgetId, String text,
			String scheduleViewerProviderId,
			DefaultScheduleViewerConfigurationProviderExtended scheduleViewerProviderInstance) {
		super(page, widgetId, text,
			scheduleViewerProviderId,
			scheduleViewerProviderInstance);
	}
	
	@Override
	public void onPrePOST(String data, OgemaHttpRequest req) {
		//final GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
		
		List<TimeSeriesData> input = getTimeseries(req);
		
		ReadOnlyTimeSeries timeSeries;
		List<ReadOnlyTimeSeries> result = new ArrayList<>();
		Map<String, String> shortNamesRD = new HashMap<String, String>();
		Map<String, String> longNamesRD = new HashMap<String, String>();
		Map<ReadOnlyTimeSeries, String> shortNames = new HashMap<ReadOnlyTimeSeries, String>();
		Map<ReadOnlyTimeSeries, String> longNames = new HashMap<ReadOnlyTimeSeries, String>();
		Map<ReadOnlyTimeSeries, Class<?>> types = new HashMap<>();
		Map<String, Class<?>> typesRD = new HashMap<>();
		for (TimeSeriesData tsdBase : input) {
			if(!(tsdBase instanceof TimeSeriesDataOffline)) throw new IllegalStateException("getStartAndEndTime only works on TimeSeriesData input!");
			TimeSeriesDataOffline tsd = (TimeSeriesDataOffline) tsdBase;
			timeSeries = tsd.getTimeSeries();
			String tsId;
			String shortName;
			String longName;
			if(timeSeries instanceof RecordedData) {
				tsId = ((RecordedData)timeSeries).getPath();
			} else tsId = null;
			GaRoDataTypeI dataType = null;
			if(tsd instanceof TimeSeriesDataExtendedImpl) {
				TimeSeriesDataExtendedImpl tse = (TimeSeriesDataExtendedImpl)tsd;
				if(tse.type instanceof GaRoDataTypeI) {
					if(tse.getIds().size() > 1) {
						String gwId = tse.getIds().get(0);
						String prop = System.getProperty("org.ogema.evaluationofflinecontrol.scheduleviewer.expert.sensorsToFilterOut."+gwId);
						if(prop != null) {
							List<String> sensorsToFilterOut = Arrays.asList(prop.split(","));
							String shortId = tse.getProperty("deviceName");
							if(shortId != null)
								if(sensorsToFilterOut.contains(shortId)) continue;
						}
					}
					String location = tsd.label(null);
					if(tsId == null) tsId = location;
					dataType = (GaRoDataTypeI)tse.type;
					String inputLabel = dataType.label(null).replace("Measurement", "");
					if((tse.getIds().size() > 1) && tse.getIds().get(1).equals(GaRoMultiEvalDataProvider.BUILDING_OVERALL_ROOM_ID))
						shortName = StringListFormatUtils.getStringFromList(null, tse.getIds().get(0), getDeviceShortId(location), inputLabel);
					else
						shortName = StringListFormatUtils.getStringFromList(tse.getIds(), getDeviceShortId(location), inputLabel);
					longName = StringListFormatUtils.getStringFromList(tse.getIds(), tsd.label(null), inputLabel);
				} else {
					shortName = StringListFormatUtils.getStringFromList(tse.getIds());
					longName = StringListFormatUtils.getStringFromList(tse.getIds(), tsd.label(null));
				}
			} else {
				shortName = tsd.label(null);
				longName = tsd.description(null);
			}
			if(tsId == null) {
				shortNames.put(timeSeries, shortName);
				longNames.put(timeSeries, longName);
				if(dataType != null) types.put(timeSeries, dataType.representingResourceType());
			} else {
				shortNamesRD.put(tsId, shortName);
				longNamesRD.put(tsId, longName);						
				if(dataType != null) typesRD.put(tsId, dataType.representingResourceType());
			}
			if(timeSeries != null) result.add(timeSeries);
		}
		List<Collection<TimeSeriesFilter>> programs = new ArrayList<>();
		List<TimeSeriesFilter> programsInner = new ArrayList<>();
		programsInner.add(new DefaultTimeSeriesFilterExtended("Filter for "+getEvaluationProviderId(req), shortNames, longNames,
				shortNamesRD, longNamesRD, types, typesRD));
		programs.add(programsInner);
		
		//String config = selectConfig.getSelectedLabel(req);
		IntervalConfiguration itv = getITVConfiguration(req);
		final long startTime;
		final long endTime;
		if(itv.multiStart == null || itv.multiStart.length > 0) {
			startTime = itv.start;
			endTime = itv.end;
		} else {
			startTime = itv.multiStart[0];
			endTime = itv.multiEnd[itv.multiStart.length-1];
		}
		
		final ScheduleViewerConfiguration viewerConfiguration =
				ScheduleViewerConfigurationBuilder.newBuilder().setPrograms(programs).
				setStartTime(startTime).setEndTime(endTime).build();
		
		String ci = addConfig(new DefaultDedicatedTSSessionConfiguration(result, viewerConfiguration));
		setConfigId(ci, req);
	}
	
	public static String getDeviceShortId(String location) {
		String[] parts = location.split("/");
		if(parts.length < 3) return "?S?";
		if(!(parts[0].toLowerCase().equals("homematic") ||
				parts[0].toLowerCase().equals("homematicip")))
			return "?X?";
		if(!parts[1].equals("devices")) return "?Y?";
		if(parts[2].length() < 5) return parts[2];
		return parts[2].substring(parts[2].length()-4);	
	}

}
