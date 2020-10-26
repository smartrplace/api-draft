package org.ogema.devicefinder.api;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.template.LabelledItem;

public class AlarmingGroupType implements LabelledItem {
	protected final String id;
	protected final String label;

	public AlarmingGroupType(String id, String label) {
		this.id = id;
		this.label = label;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String label(OgemaLocale locale) {
		return label;
	}
	
	public static final AlarmingGroupType LIMIT_ALARM = new AlarmingGroupType(
			"LIMIT_ALARM", "Resource limit violation");
	public static final AlarmingGroupType HomematicFailed = new AlarmingGroupType(
			"HomematicFailed", "Homematic Failed");
	public static final AlarmingGroupType SingleSensorFailed = new AlarmingGroupType(
			"SingleSensorFailed", "Single Sensor Failed");
}
