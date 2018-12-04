package org.ogema.externalviewer.extensions;

import java.util.HashMap;
import java.util.Map;

import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.recordeddata.RecordedData;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.tools.resource.util.ResourceUtils;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilterExtended;
import de.iwes.widgets.reswidget.scheduleviewer.utils.ScheduleViewerUtil;

public class DefaultTimeSeriesFilterExtended implements TimeSeriesFilterExtended {
	private final String id;
	private final String label;
	private final Map<ReadOnlyTimeSeries, String> shortName;
	private final Map<ReadOnlyTimeSeries, String> longName;
	private final Map<ReadOnlyTimeSeries, Class<?>> typeMap;
	private final Map<String, String> shortNameRD;
	private final Map<String, String> longNameRD;
	private final Map<String, Class<?>> typeMapRD;
	
	public DefaultTimeSeriesFilterExtended(String label, Map<ReadOnlyTimeSeries, String> shortName,
			Map<ReadOnlyTimeSeries, String> longName,
			Map<String, String> shortNameRD,
			Map<String, String> longNameRD) {
		this(ResourceUtils.getValidResourceName(label), label, shortName, longName, shortNameRD,
				longNameRD);
	}
	public DefaultTimeSeriesFilterExtended(String id, String label, Map<ReadOnlyTimeSeries, String> shortName,
			Map<ReadOnlyTimeSeries, String> longName,
			Map<String, String> shortNameRD,
			Map<String, String> longNameRD) {
		this(id, label, shortName, longName, shortNameRD, longNameRD, null, null);
	}
	public DefaultTimeSeriesFilterExtended(String label,
			Map<ReadOnlyTimeSeries, String> shortName,
			Map<ReadOnlyTimeSeries, String> longName,
			Map<String, String> shortNameRD,
			Map<String, String> longNameRD,
			Map<ReadOnlyTimeSeries, Class<?>> typeMap,
			Map<String, Class<?>> typeMapRD) {
		this(ResourceUtils.getValidResourceName(label), label, shortName, longName, shortNameRD,
				longNameRD, typeMap, typeMapRD);
	}
	public DefaultTimeSeriesFilterExtended(String id, String label,
			Map<ReadOnlyTimeSeries, String> shortName,
			Map<ReadOnlyTimeSeries, String> longName,
			Map<String, String> shortNameRD,
			Map<String, String> longNameRD,
			Map<ReadOnlyTimeSeries, Class<?>> typeMap,
			Map<String, Class<?>> typeMapRD) {
		this.id = id;
		this.label = label;
		if(shortName == null)
			this.shortName = new HashMap<>();
		else
			this.shortName = shortName;
		if(longName == null)
			this.longName = new HashMap<>();
		else
			this.longName = longName;
		if(shortNameRD == null) this.shortNameRD = new HashMap<String, String>();
		else this.shortNameRD = shortNameRD;
		if(longNameRD == null) this.longNameRD = new HashMap<String, String>();
		else this.longNameRD = longNameRD;
		if(typeMap == null) this.typeMap = new HashMap<ReadOnlyTimeSeries, Class<?>>();
		else this.typeMap = typeMap;
		if(typeMapRD == null) this.typeMapRD = new HashMap<String, Class<?>>();
		else this.typeMapRD = typeMapRD;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String label(OgemaLocale locale) {
		return label;
	}

	@Override
	public boolean accept(ReadOnlyTimeSeries schedule) {
		return true;
	}

	@Override
	public String shortName(ReadOnlyTimeSeries schedule) {
		String name;
		if(schedule instanceof RecordedData) {
			RecordedData rd = (RecordedData)schedule;
			name = shortNameRD.get(rd.getPath());
		} else
			name = shortName.get(schedule);
		if(name == null) return ScheduleViewerUtil.getScheduleShortName(schedule, null);
		return name;
	}

	@Override
	public String longName(ReadOnlyTimeSeries schedule) {
		String name;
		if(schedule instanceof RecordedData) {
			RecordedData rd = (RecordedData)schedule;
			name = longNameRD.get(rd.getPath());
		} else
			name = longName.get(schedule);
		if(name == null) return ScheduleViewerUtil.getScheduleLongName(schedule, "nodev-info", null);
		return name;
	}
	@Override
	public Class<?> type(ReadOnlyTimeSeries schedule) {
		Class<?> type;
		if(schedule instanceof RecordedData) {
			RecordedData rd = (RecordedData)schedule;
			type = typeMapRD.get(rd.getPath());
		} else
			type = typeMap.get(schedule);
		if(type == null) return FloatResource.class; //ScheduleViewerUtil.getScheduleLongName(schedule, "nodev-info", null);
		return type;
	}
}
