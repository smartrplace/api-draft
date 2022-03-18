package org.smartrplace.alarming.escalation.util;

import java.util.List;

import org.ogema.core.application.AppID;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationLevel;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationSettings;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.widgets.template.LabelledItem;

public interface EscalationProvider extends LabelledItem {

	void knownIssueNotification(InstallAppDevice iad);
	
	public void execute(long now, TimedJobMemoryData data, List<AppID> appIDs);

	/** Called once on startup. Chooses the timers to use.
	 * 
	 * @param settings
	 * @return if true the service shall be running from start, otherwise it is stopped from start
	 */
	boolean initProvider(AlarmingEscalationLevel persistData, AlarmingEscalationSettings settings,
			List<InstallAppDevice> knownIssueDevices);

	/** Pre-configure especially {@link AlarmingEscalationLevel#timedJobData()}*/
	void initConfig(AlarmingEscalationLevel levelRes);
}
