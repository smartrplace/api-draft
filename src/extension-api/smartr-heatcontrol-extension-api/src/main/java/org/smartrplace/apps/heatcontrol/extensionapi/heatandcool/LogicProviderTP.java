package org.smartrplace.apps.heatcontrol.extensionapi.heatandcool;

import org.ogema.model.prototypes.Configuration;

public interface LogicProviderTP {
	void setRemoteManualSetting();
	HeatControlLogicI logic();
	boolean isFinal();
	Configuration roomTemperatureControlSettings();
}
