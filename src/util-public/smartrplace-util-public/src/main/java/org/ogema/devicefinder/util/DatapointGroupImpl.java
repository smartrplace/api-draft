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
	protected Map<String, DatapointGroup> subGroups = new HashMap<>();
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
		synchronized(this) {
			for(Datapoint dpknown: this.datapoints) {
				if(dpknown.id().equals(dp.id()))
					return false;
			}
			return this.datapoints.add(dp);
		}
	}

	@Override
	public void addAll(Collection<Datapoint> datapoints) {
		for(Datapoint dp: datapoints)
			addDatapoint(dp);
		//this.datapoints.addAll(datapoints);
	}

	@Override
	public boolean registerAsChart(String configurationPage) {
		if(configurationPage == null)
			charts.add(DEFAULT_PLOT_CONFIG_PAGE);
		else
			charts.add(configurationPage);
		return true;
	}

	@Override
	public List<String> getChartConfigPagesRegistered() {
		return new ArrayList<String>(charts);
	}

	@Override
	public List<DatapointGroup> getSubGroups() {
		return new ArrayList<DatapointGroup>(subGroups.values());
	}

	@Override
	public boolean addSubGroup(DatapointGroup dpGrp) {
		return subGroups.put(dpGrp.id(), dpGrp) == null;
	}

	@Override
	public boolean removeSubGroup(DatapointGroup dpGrp) {
		return subGroups.remove(dpGrp.id()) != null;
	}
	
	@Override
	public DatapointGroup getSubGroup(String id) {
		return subGroups.get(id);
	}
}
