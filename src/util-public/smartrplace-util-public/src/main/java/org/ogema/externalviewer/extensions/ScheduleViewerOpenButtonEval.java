/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ogema.externalviewer.extensions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.util.DeviceTableBase;
import org.smartrplace.util.format.StringListFormatUtils;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.TimeSeriesDataOffline;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationBuilder;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilter;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilterExtended;

public abstract class ScheduleViewerOpenButtonEval extends ScheduleViewerOpenButton {
	private static final long serialVersionUID = 1L;
	protected TimeSeriesNameProvider nameProvider = new TimeSeriesNameProvider() {};
	
	protected abstract List<TimeSeriesData> getTimeseries(long start, long end, OgemaHttpRequest req);
	/** This method is only required for generating filter name. If not available
	 * just return an empty String or any name of the time series set
	 */
	protected abstract String getEvaluationProviderId(OgemaHttpRequest req);
	protected abstract IntervalConfiguration getITVConfiguration(OgemaHttpRequest req);
	boolean generateGraphImmediately() { return false;}
	
	protected ScheduleViewerConfiguration getViewerConfiguration(long startTime, long endTime,
			List<Collection<TimeSeriesFilter>> programs) {
		final ScheduleViewerConfiguration viewerConfiguration =
			ScheduleViewerConfigurationBuilder.newBuilder().setPrograms(programs).
			setStartTime(startTime).setEndTime(endTime).setShowManipulator(true).
			setShowIndividualConfigBtn(false).setShowPlotTypeSelector(true).build();
		return viewerConfiguration;
	}

	
	public ScheduleViewerOpenButtonEval(WidgetPage<?> page, String widgetId, String text,
			String scheduleViewerProviderId,
			DefaultScheduleViewerConfigurationProviderExtended scheduleViewerProviderInstance) {
		super(page, widgetId, text,
			scheduleViewerProviderId,
			scheduleViewerProviderInstance);
	}
	public ScheduleViewerOpenButtonEval(OgemaWidget parent, String widgetId, String text,
			String scheduleViewerProviderId,
			DefaultScheduleViewerConfigurationProviderExtended scheduleViewerProviderInstance,
			OgemaHttpRequest req) {
		super(parent, widgetId, text,
			scheduleViewerProviderId,
			scheduleViewerProviderInstance, req);
	}

	public void setNameProvider(TimeSeriesNameProvider nameProvider) {
		this.nameProvider = nameProvider;
	}

	@Override
	public void onPrePOST(String data, OgemaHttpRequest req) {
		//final GaRoSingleEvalProvider eval = selectProvider.getSelectedItem(req);
		
		final long startTime;
		final long endTime;
		IntervalConfiguration itv = getITVConfiguration(req);
		if(itv.multiStart == null || itv.multiStart.length > 0) {
			startTime = itv.start;
			endTime = itv.end;
		} else {
			startTime = itv.multiStart[0];
			endTime = itv.multiEnd[itv.multiStart.length-1];
		}

		List<TimeSeriesData> input = getTimeseries(startTime, endTime, req);
		TimeSeriesWithFilters filteringResult = getTimeSeriesWithFilters(input, "Filter for "+getEvaluationProviderId(req), nameProvider);
		@SuppressWarnings({ "unchecked", "rawtypes" })
		List<TimeSeriesFilter> programsInner = (List)filteringResult.filters;
		//List<ReadOnlyTimeSeries> result = new ArrayList<>();

		List<Collection<TimeSeriesFilter>> programs = new ArrayList<>();
		programs.add(programsInner);
		
		final ScheduleViewerConfiguration viewerConfiguration = getViewerConfiguration(
				startTime, endTime, programs);
		//		ScheduleViewerConfigurationBuilder.newBuilder().setPrograms(programs).
		//		setStartTime(startTime).setEndTime(endTime).setShowManipulator(true).build();
		
		String ci = addConfig(new DefaultDedicatedTSSessionConfiguration(filteringResult.timeSeries, viewerConfiguration,
				generateGraphImmediately()));
		setConfigId(ci, req);
	}
	
	public static String getDeviceShortIdPlus(String location) {
		location = DeviceTableBase.makeDeviceToplevel(location);
		if(location.toLowerCase().startsWith("homematic")) {
			return ":HM"+getDeviceShortId(location);
		}
		return getDeviceShortId(location);
	}
	public static String getDeviceShortId(String location) {
		location = DeviceTableBase.makeDeviceToplevel(location);
		String[] parts = location.split("/");
		//if(parts.length < 3) return "?S?";
		if(!(parts[0].toLowerCase().startsWith("homematic"))) {
			String result = getLastCharsWithDigitsPreferred(parts[0], 4);
			if(result.matches(".*\\d.*"))
				return result;
			result = getLastCharsWithDigitsPreferred(parts[parts.length-1], 4);
			if(result.matches(".*\\d.*"))
				return result;
			return "";
			//return "?X?";
		}
		if(parts.length < 2)
			return getLastCharsWithDigitsPreferred(parts[0], 4);
		if(!parts[1].equals("devices")) return "?Y?";
		if(parts.length == 2)
			return getLastCharsWithDigitsPreferred(parts[1], 4);
		if(parts[2].length() < 5) return parts[2];
		return getLastCharsWithDigitsPreferred(parts[2], 4);	
	}

	public static String getLastCharsWithDigitsPreferred(String str, int num) {
		if(str.isEmpty())
			return str;
		String baseResult;
		if(str.length() >= num)
			baseResult = str.substring(str.length()-num);
		else
			baseResult = str;
		char last = baseResult.charAt(baseResult.length()-1);
		if(!acceptCharAsDigitPreferred(last))
			return baseResult;
		for(int i=1; i<baseResult.length(); i++) {
			last = baseResult.charAt(baseResult.length()-1-i);
			if(!acceptCharAsDigitPreferred(last))
				return baseResult.substring(baseResult.length()-i);
		}
		return baseResult;
	}
	
	protected static boolean acceptCharAsDigitPreferred(char last) {
		//return (last>='0' && last <='9');
		return (last <='9') || (last >= 'A' && last <= 'F') || (last >= 'a' && last <= 'f');
	}
	
	public static int getNumberById(String str) {
		if(str.length() < 4)
			return 9999;
		try {
			return Integer.parseInt(str.substring(str.length()-4));
		} catch(NumberFormatException e) {
			return 9998;
		}
	}
	
	public static class TimeSeriesWithFilters {
		public List<ReadOnlyTimeSeries> timeSeries = new ArrayList<>();
		public List<TimeSeriesFilterExtended> filters;
	}
	
	public static interface TimeSeriesNameProvider {
		default String getShortNameForTypeI(GaRoDataTypeI dataType, TimeSeriesDataExtendedImpl tse) {
			String inputLabel = dataType.label(null).replace("Measurement", "");
			String location = tse.label(null);
			if((tse.getIds().size() > 1) && tse.getIds().get(1).equals(GaRoMultiEvalDataProvider.BUILDING_OVERALL_ROOM_ID))
				return StringListFormatUtils.getStringFromList(null, tse.getIds().get(0), getDeviceShortId(location), inputLabel);
			else
				return StringListFormatUtils.getStringFromList(tse.getIds(), getDeviceShortId(location), inputLabel);
			
		}
		default String getLongNameForTypeI(GaRoDataTypeI dataType, TimeSeriesDataExtendedImpl tse) {
			String inputLabel = dataType.label(null).replace("Measurement", "");
			return StringListFormatUtils.getStringFromList(tse.getIds(), tse.label(null), inputLabel);
		};
		
		default String getShortNameBase(TimeSeriesDataExtendedImpl tse) {
			if(tse.getIds() == null) {
				return tse.label(null);
			} else {
				return StringListFormatUtils.getStringFromList(tse.getIds());
			}
		}

		default String getLongNameBase(TimeSeriesDataExtendedImpl tse) {
			if(tse.getIds() == null) {
				return StringListFormatUtils.getStringFromList(null, "NoGw", tse.label(null));
			} else {
				return StringListFormatUtils.getStringFromList(tse.getIds(), tse.label(null));
			}
		}
		
		/** If tse1 is more important return +1, if tse2 is more important return -1*/
		default int compareInput(String shortName1, String shortName2) {
			return 0;
		}
	}
	
	public static TimeSeriesWithFilters getTimeSeriesWithFilters(List<TimeSeriesData> input, String filterName) {
		return getTimeSeriesWithFilters(input, filterName, new TimeSeriesNameProvider() {});
	}
	@SuppressWarnings("unchecked")
	public static TimeSeriesWithFilters getTimeSeriesWithFilters(List<TimeSeriesData> input,
			String filterName, TimeSeriesNameProvider nameProvider) {
		ReadOnlyTimeSeries timeSeries;
		//List<ReadOnlyTimeSeries> result = new ArrayList<>();
		TimeSeriesWithFilters result = new TimeSeriesWithFilters();
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
			final String tsId;
			String shortName;
			String longName;
			if(timeSeries instanceof RecordedData) {
				tsId = ((RecordedData)timeSeries).getPath();
			} else tsId = null;
			
			GaRoDataTypeI dataType = null;
			Class<? extends Resource> type = null;
			if(tsd instanceof TimeSeriesDataExtendedImpl) {
				TimeSeriesDataExtendedImpl tse = (TimeSeriesDataExtendedImpl)tsd;
				if(tse.type instanceof GaRoDataTypeI) {
					dataType = (GaRoDataTypeI) tse.type;
					if(tse.getIds() != null && tse.getIds().size() > 1) {
						String gwId = tse.getIds().get(0);
						String prop = System.getProperty("org.ogema.evaluationofflinecontrol.scheduleviewer.expert.sensorsToFilterOut."+gwId);
						if(prop != null) {
							List<String> sensorsToFilterOut = Arrays.asList(prop.split(","));
							if((!sensorsToFilterOut.isEmpty())&&(sensorsToFilterOut.get(0).length()>4)) {
								boolean found = false;
								for(String toRemove: sensorsToFilterOut) {
									if(tse.id().contains(toRemove)) {
										found = true;
										break;
									}
								}
								if(found)
									continue;
							} else {
								String shortId = tse.getProperty("deviceName");
								if(shortId != null) {
									if(sensorsToFilterOut.contains(shortId))
										continue;
								}
							}
						}
					}
					if(nameProvider != null ) {
						shortName = nameProvider.getShortNameForTypeI(dataType, tse);
						longName = nameProvider.getLongNameForTypeI(dataType, tse);
					} else {
						shortName = tsd.label(null);
						longName = tsd.description(null);										
					}
				} else {
					if(tse.type instanceof Class)
						type = (Class<? extends Resource>)tse.type;
					if(nameProvider != null) {
						shortName = nameProvider.getShortNameBase(tse);
						longName = nameProvider.getLongNameBase(tse);
					} else {
						shortName = tsd.label(null);
						longName = tsd.description(null);										
					}
				}
			} else {
				shortName = tsd.label(null);
				longName = tsd.description(null);
			}
			
			//avoid overwriting of more significant names
			if(tsId == null) {
				if(shortNames.containsKey(timeSeries) && nameProvider != null) {
					if(nameProvider.compareInput(shortNames.get(timeSeries), shortName) > 0)
						continue;
				}
			} else {
				if(shortNamesRD.containsKey(tsId) && nameProvider != null) {
					if(nameProvider.compareInput(shortNamesRD.get(tsId), shortName) > 0)
						continue;
				}
			}
			if(tsId == null) {
				shortNames.put(timeSeries, shortName);
				longNames.put(timeSeries, longName);
				if(dataType != null) types.put(timeSeries, dataType.representingResourceType());
				else if(type != null) types.put(timeSeries, type);
			} else {
				shortNamesRD.put(tsId, shortName);
				longNamesRD.put(tsId, longName);						
				if(dataType != null) typesRD.put(tsId, dataType.representingResourceType());
				else if(type != null) typesRD.put(tsId, type);
			}
			if(timeSeries != null) result.timeSeries.add(timeSeries);
		}
		List<TimeSeriesFilterExtended> programsInner = new ArrayList<>();
		programsInner.add(new DefaultTimeSeriesFilterExtended(filterName, shortNames, longNames,
				shortNamesRD, longNamesRD, types, typesRD));
		result.filters = programsInner;
		return result;
	}
}
