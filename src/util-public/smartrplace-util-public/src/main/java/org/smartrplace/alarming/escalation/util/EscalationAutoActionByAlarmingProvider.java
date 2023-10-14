package org.smartrplace.alarming.escalation.util;

import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.core.model.Resource;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationLevel;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationSettings;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public abstract class EscalationAutoActionByAlarmingProvider<R extends Resource> extends EscalationProviderSimple<EscalationKnownIssue> {
	protected final Class<R> deviceType;
	protected final Integer alignedTiming;
	protected final long offsetOrInterval;
	protected final long minimumTimeFromErrorToAction;
	protected final String name;
	
	protected final ApplicationManagerPlus appManPlus;
	protected final String baseUrl;
	protected final LocalGatewayInformation gwRes;
	
	/** Implement to perform action
	 * 
	 * @param iad
	 * @param device
	 * @param issue
	 * @return String to be appended to Email message. If null nothing is appended.
	 */
	protected abstract String performAction(InstallAppDevice iad, R device, EscalationKnownIssue issue);
	
	public EscalationAutoActionByAlarmingProvider(Class<R> deviceType, Integer alignedTiming,
			long offsetOrInterval, long minimumTimeFromErrorToAction, String name, ApplicationManagerPlus appManPlus) {
		this.appManPlus = appManPlus;
		this.gwRes = ResourceHelper.getLocalGwInfo(appManPlus.appMan());
		this.baseUrl = gwRes.gatewayBaseUrl().getValue();

		this.deviceType = deviceType;
		this.alignedTiming = alignedTiming;
		this.offsetOrInterval = offsetOrInterval;
		this.minimumTimeFromErrorToAction = minimumTimeFromErrorToAction;
		this.name = name;
	}
	
	@Override
	public String label(OgemaLocale locale) {
		return name;
	}

	@Override
	public void initConfig(AlarmingEscalationLevel levelRes) {
		if(alignedTiming != null)
			ValueResourceHelper.setCreate(levelRes.timedJobData().alignedInterval(), alignedTiming);
		ValueResourceHelper.setCreate(levelRes.timedJobData().interval(), offsetOrInterval / TimeProcUtil.MINUTE_MILLIS);
		ValueResourceHelper.setCreate(levelRes.standardDelay(), minimumTimeFromErrorToAction);
	}

	@Override
	protected EscalationKnownIssue isEscalationRelevant(InstallAppDevice iad) {
		if(deviceType.isAssignableFrom(iad.device().getResourceType()))
			return new EscalationKnownIssue();
		return null;
	}

	@SuppressWarnings("unchecked")
	@Override
	/** The standard implementation checks if an alarm is beyond minimum time and there are still active alarms on the device. If
	 * so the action is performed and an email is sent
	 */
	protected EscalationCheckResult checkEscalation(Collection<EscalationKnownIssue> issues, List<AppID> appIDs, long now) {
		int maxFault = 0;
		String emailMessage = EscalationProviderSimple.getMessageHeaderLinks(baseUrl, gwRes, appManPlus.appMan());
		int countDevice = 0;
		for(EscalationKnownIssue issue: issues) {
			if(issue.knownIssue.assigned().getValue() > 0)
				continue;
			long duration = now - issue.knownIssue.ongoingAlarmStartTime().getValue();
			if(duration < persistData.standardDelay().getValue())
				continue;
			int[] alarms = AlarmingConfigUtil.getActiveAlarms(issue.device);
			//For now we do not release, but we shall check what's going on
			if(alarms[0] == 0) {
				continue;
			}
			if(alarms[0] > maxFault) {
				maxFault = alarms[0];
			}
			countDevice++;
			String result = performAction(issue.device, (R)issue.device.device(), issue);

			if(emailMessage == null) {
				//We put a return upfront as initial line will be filled with "Notification :" by EmailService, which disturbs when reading through the messages
				emailMessage = "\r\n"+issue.knownIssue.lastMessage().getValue();
			} else
				emailMessage += "\r\n\r\n"+issue.knownIssue.lastMessage().getValue();
			
			if(result != null)
				emailMessage += "\r\n "+result;

			if(Boolean.getBoolean("org.smartrplace.apps.alarmingconfig.escalationservices.debugsource")) {
				emailMessage += "\r\n SOURCE DEBUG: "+issue.knownIssue.getLocation();
			}
		}
		if(maxFault > 0) {
			EscalationProviderSimple.sendDeviceSpecificMessage(emailMessage, countDevice, maxFault, "Performed CCU Reboot",
					appIDs, persistData, appManPlus);
		}
		return new EscalationCheckResult();
	}
	
	@Override
	public Boolean initProvider(AlarmingEscalationLevel persistData, AlarmingEscalationSettings settings,
			List<InstallAppDevice> knownIssueDevices) {
		super.initProvider(persistData, settings, knownIssueDevices);
		return true;
	}
}
