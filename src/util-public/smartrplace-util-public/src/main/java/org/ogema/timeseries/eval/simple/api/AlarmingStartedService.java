package org.ogema.timeseries.eval.simple.api;

import org.ogema.core.model.ValueResource;
import org.ogema.devicefinder.util.AlarmingExtensionBase.ValueListenerDataBase;

/** Service indicating that alarming has fully started after system restart*/
public interface AlarmingStartedService {
	/** As soon as the service is available this should always be true*/
	boolean isAlarmingStarted();
	
	/** Get alarming internal parameter data */
	ValueListenerDataBase getValueListenerData(ValueResource res);
}
