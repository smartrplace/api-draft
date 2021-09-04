package org.ogema.timeseries.eval.simple.api;

/** Service indicating that alarming has fully started after system restart*/
public interface AlarmingStartedService {
	/** As soon as the service is available this should always be true*/
	boolean isAlarmingStarted();
}
