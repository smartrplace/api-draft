package org.smartrplace.alarming.escalation.util;

import java.util.List;

import org.ogema.model.extended.alarming.AlarmGroupData;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.gateway.device.KnownIssueDataGw;

import de.iwes.widgets.template.LabelledItem;

public interface EscalationProvider extends LabelledItem {
	/** The provider can decide itself for which devices it can provide escalation services.
	 * In the future additional configurations may block certain providers from being asked
	 * @param iad
	 * @return
	 */
	boolean isDeviceRelevant(InstallAppDevice iad);
	
	public static class EscalationCheckResult {
		public boolean isDone = false;
		/** May be null. If messages to escalation levels are sent then the levels shall be
		 * listed in the result*/
		public List<AlarmingEscalationLevel> messagesSent = null;
	}
	/** Check if escalation services are performed by the provider
	 * TODO: Add parameter data so that the provider can send out messages to the {@link AlarmingEscalationLevel}s
	 * relevant for it.
	 * TODO: Maybe also additional input data is required, check when implementing
	 * 
	 * @param iad the device that shall be checked. The provider can assume that
	 * 		{@link InstallAppDevice#knownFault()} is active on the device and provides most of the
	 * 		necessary information.
	 * @return if true the escalation is done and does not need to be called for this EscalationLevel
	 * 		and this provider anymore
	 * 		TODO: Can/Need we handle this with persistence? => Yes, shall be stored in initDone field
	 *      in {@link AlarmGroupData}(known issue), so will be deleted when knownIssue is deleted.
	 */
	EscalationCheckResult checkEscalation(InstallAppDevice iad);
	
	public static class EscalationProviderRegistrationRequestData {
		/** Escalation levels for which the timing configurations shall be used. For aligned
		 * times all open knownIssues are checked, for non-aligned each knownIssue depending
		 * on its creation time.
		 */
		public List<AlarmingEscalationLevel> timerLevels;
	}
	/** Called once on startup. Chooses the timers to use.
	 * 
	 * @param settings
	 * @return
	 */
	EscalationProviderRegistrationRequestData registerProvider(AlarmingEscalationSettings settings);
}
