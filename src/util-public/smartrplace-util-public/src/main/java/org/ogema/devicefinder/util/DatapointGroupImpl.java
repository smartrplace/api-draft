package org.ogema.devicefinder.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class DatapointGroupImpl implements DatapointGroup {
	protected final String id;
	protected String type;
	protected Map<OgemaLocale, String> labels = new HashMap<>();
	protected List<Datapoint> datapoints = new ArrayList<>();
	protected Set<String> charts = new HashSet<>();
	
	public DatapointGroupImpl(String id) {
		this.id = id;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String label(OgemaLocale locale) {
		return labels.get(locale);
	}

	@Override
	public void setLabel(OgemaLocale locale, String label) {
		labels.put(locale, label);
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public boolean setType(String type) {
		this.type = type;
		return true;
	}

	@Override
	public List<Datapoint> getAllDatapoints() {
		return datapoints;
	}

	@Override
	public boolean addDatapoint(Datapoint dp) {
		return this.datapoints.add(dp);
	}

	@Override
	public void addAll(Collection<Datapoint> datapoints) {
		this.datapoints.addAll(datapoints);
	}

	@Override
	public boolean registerAsChart(String cofigurationPage) {
		charts.add(cofigurationPage);
		return true;
	}

	@Override
	public List<String> getChartConfigPagesRegistered() {
		return new ArrayList<String>(charts);
	}

}
