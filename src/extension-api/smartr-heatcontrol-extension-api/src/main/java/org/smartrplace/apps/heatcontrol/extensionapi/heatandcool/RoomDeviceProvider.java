package org.smartrplace.apps.heatcontrol.extensionapi.heatandcool;

import java.util.List;

import org.smartrplace.apps.heatcontrol.extensionapi.heatandcool.TemperatureControlBase.SmartPlugData;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public interface RoomDeviceProvider {
	default List<SmartPlugData> additionalDevicesForSwitching(OgemaLocale locale) { return null;}
}
