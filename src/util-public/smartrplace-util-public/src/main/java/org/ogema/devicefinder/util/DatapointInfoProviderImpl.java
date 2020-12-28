package org.ogema.devicefinder.util;

import java.util.Map;
import java.util.Set;

import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.DatapointInfo;
import org.ogema.devicefinder.api.DatapointInfoProvider;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Just overwrite any method for which you want to provide special information*/
public abstract class DatapointInfoProviderImpl implements DatapointInfoProvider {

	@Override
	public GaRoDataType getGaroDataType() {
		return null;
	}

	@Override
	public String labelDefault() {
		return null;
	}

	@Override
	public Map<OgemaLocale, String> getAllLabels() {
		return null;
	}

	@Override
	public DPRoom getRoom() {
		return null;
	}

	@Override
	public String getSubRoomLocation(OgemaLocale locale, Object context) {
		return null;
	}

	@Override
	public DatapointInfo info() {
		return null;
	}

	@Override
	public Boolean isLocal() {
		return null;
	}

	@Override
	public ScalingProvider getScale() {
		return null;
	}

	@Override
	final public String id() {
		throw new IllegalStateException("DatapointId may not be overridden by DatapointInfoProvider, always use implementation method!");
	}

	@Override
	public String label(OgemaLocale locale) {
		return null;
	}

	@Override
	public Float getCurrentValue() {
		return null;
	}

	@Override
	public boolean setCurrentValue(Float value) {
		return false;
	}

	@Override
	public Set<String> getAliases() {
		return null;
	}
}
