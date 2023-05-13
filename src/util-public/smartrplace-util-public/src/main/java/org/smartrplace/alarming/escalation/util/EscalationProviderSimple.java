package org.smartrplace.alarming.escalation.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.AppID;
import org.ogema.devicefinder.api.TimedJobMemoryData;
import org.ogema.model.extended.alarming.AlarmGroupData;
import org.ogema.model.extended.alarming.EscalationData;
import org.ogema.model.gateway.LocalGatewayInformation;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationLevel;
import org.smartrplace.alarming.escalation.model.AlarmingEscalationSettings;
import org.smartrplace.alarming.escalation.model.AlarmingMessagingApp;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.tissue.util.resource.GatewayUtil;
import org.smartrplace.util.message.FirebaseUtil;
import org.smartrplace.util.message.MessageImpl;
import org.ogema.devicefinder.util.AlarmingConfigUtil;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.messaging.MessagePriority;

/** Template for the implementation of EscalationProviders
 * 
 * @param <T> configuration data per issue processed. Non persistent data per knownIssue for the application. Persistent data
 * 		can be added as decorators to the {@link AlarmingEscalationLevel} resource of the provider or to {@link EscalationData}
 * 		for per-device data.
 */
public abstract class EscalationProviderSimple<T extends EscalationKnownIssue> implements EscalationProvider {
	Map<String, T> ongoingIssues = new HashMap<>();
	Set<String> knownIssuesNotRelevant = new HashSet<>();
	
	protected AlarmingEscalationLevel persistData;
	protected AlarmingEscalationSettings settings;
	
	/**Override if necessary. initStart is called first, then devices are checked*/
	protected void initStart() {};
	
	/** Check if a certain known issue is relevant for the provider. This implies checking the device and the 
	 * 
	 * @param iad containing a knownIssue
	 * @return if null the knownIssue will never be shown to the provider again until it is released or the app is restarted
	 * 		(not stored persistently)
	 */
	protected abstract T isEscalationRelevant(InstallAppDevice iad);

	public static class EscalationCheckResult {
		/** If this is returned then no more calls are made until the time is reached. Also stored persistently*/
		public Long blockedUntil = null;
		/** May be null. If messages to escalation levels are sent then the levels shall be
		 * listed in the result*/
		//public List<AlarmingEscalationLevel> messagesSent = null;
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
	protected abstract EscalationCheckResult checkEscalation(Collection<T> issues, List<AppID> appIDs, long now);
	
	@Override
	public String id() {
		return this.getClass().getSimpleName();
	}

	@Override
	public Boolean initProvider(AlarmingEscalationLevel persistData, AlarmingEscalationSettings settings,
			List<InstallAppDevice> knownIssueDevices) {
		this.persistData = persistData;
		this.settings = settings;
		initStart();
		for(InstallAppDevice iad: knownIssueDevices) {
			knownIssueNotification(iad);
		}
		return null;
	}

	@Override
	public void knownIssueNotification(InstallAppDevice iad) {
		if(knownIssuesNotRelevant.contains(iad.getLocation()))
			return;
		T result = isEscalationRelevant(iad);
		if(result == null) {
			knownIssuesNotRelevant.add(iad.getLocation());
			return;
		}
		if(result.device == null)
			result.device = iad;
		if(result.knownIssue == null)
			result.knownIssue = iad.knownFault();
		synchronized (ongoingIssues) {
			ongoingIssues.put(iad.getLocation(), result);			
		}
	}
	
	@Override
	public void execute(long now, TimedJobMemoryData data, List<AppID> appIDs) {
		if(persistData.blockedUntil().getValue() > 0) {
			if((now <= persistData.blockedUntil().getValue())) {
				return;
			} else {
				persistData.blockedUntil().setValue(-1);
			}
		}
		synchronized (ongoingIssues) {
			List<String> toRemove = new ArrayList<>();
			for(Entry<String, T> issue: ongoingIssues.entrySet()) {
				if(!issue.getValue().knownIssue.isActive())
					toRemove.add(issue.getKey());
			}
			for(String key: toRemove)
				ongoingIssues.remove(key);
		}
		EscalationCheckResult eres = checkEscalation(ongoingIssues.values(), appIDs, now);
		if(eres.blockedUntil != null) {
			ValueResourceHelper.setCreate(persistData.blockedUntil(), eres.blockedUntil);
		}
	}
	
	public static String getMessageHeaderLinks(String baseUrl, LocalGatewayInformation gwRes) {
		String emailMessage;
		if(baseUrl == null)
			emailMessage = null;
		else
			emailMessage = "Known issues: "+baseUrl+"/org/smartrplace/alarmingexpert/deviceknownfaults.html" +
					"\r\nDevices: "+baseUrl+"/org/smartrplace/hardwareinstall/expert/index.html";
		if(gwRes != null) {
			if(gwRes.gatewayOperationDatabaseUrl().isActive() && gwRes.gatewayOperationDatabaseUrl().getValue().length() > 5) {
				if(emailMessage == null)
					emailMessage = "Operation data collection: "+gwRes.gatewayOperationDatabaseUrl().getValue();
				else {
					emailMessage += "\r\nOperation data collection: "+gwRes.gatewayOperationDatabaseUrl().getValue();
				}
			}
			if(gwRes.gatewayLinkOverviewUrl().isActive() && gwRes.gatewayLinkOverviewUrl().getValue().length() > 5) {
				if(emailMessage == null)
					emailMessage = "Gateway Documentation Links/Wiki: "+gwRes.gatewayLinkOverviewUrl().getValue();
				else
					emailMessage += "\r\nGateway Documentation Links/Wiki: "+gwRes.gatewayLinkOverviewUrl().getValue();
			}
		}
		return emailMessage;
	}
	
	/** Send messages to all configured. This is to report unassigned messages for certain device
	 * types
	 * 
	 * @param title
	 * @param emailMessage
	 * @param firebaseMessage
	 * @param countDevice
	 * @param maxFault
	 * @param prio
	 * @param appIDs
	 * @param persistData
	 * @param appManPlus
	 * @return firebase short message
	 */
	public static String sendDeviceSpecificMessage(String emailMessage,
			int countDevice, int maxFault, String deviceTypeTypePluralString,
			List<AppID> appIDs, 
			AlarmingEscalationLevel persistData,
			ApplicationManagerPlus appManPlus) {
		String gwId = GatewayUtil.getGatewayId(appManPlus.getResourceAccess());
		String title = gwId+"::"+countDevice+" "+deviceTypeTypePluralString+" still with open issues("+maxFault+")!";
		String message = countDevice + " unassigned "+deviceTypeTypePluralString+", max active alarms: "+maxFault;
		String firebaseDebugInfoMessage = "Sending Unassigned "+deviceTypeTypePluralString+" warning message:";
		sendEscalationMessage(title, emailMessage, message,
				firebaseDebugInfoMessage,
				appIDs, persistData, appManPlus);
		return message;
	}
	
	/** Send messages to all configured. This is mainly relevant if the email message is NOT
	 * sent in the WeeklyEmail format
	 * 
	 * @param title message email and firebase message title
	 * @param emailMessage email message
	 * @param firebaseMessage short message sent via firebase if relevant. If null no
	 * 		firebase message is sent.
	 * @param countDevice number of devices unassigned (only relevant for firebase)
	 * @param maxFault
	 * @param prio
	 * @param appIDs
	 * @param persistData
	 * @param appManPlus
	 * @return
	 */
	public static void sendEscalationMessage(String title, String emailMessage,
			String firebaseMessage,
			String firebaseDebugInfoMessage,
			List<AppID> appIDs, 
			AlarmingEscalationLevel persistData,
			ApplicationManagerPlus appManPlus) {
		MessagePriority prio = AlarmingConfigUtil.getMessagePrio(persistData.alarmLevel().getValue());
		for(AppID appId: appIDs) {
			appManPlus.guiService().getMessagingService().sendMessage(appId,
					new MessageImpl(title, emailMessage, prio));		
			//AlarmingManager.reallySendMessage(title, emailMessage , prio, appId);
		}
		Map<String, Object> additionalProperties = new HashMap<>();
		if(firebaseMessage == null)
			return;
		List<Room> rooms = appManPlus.getResourceAccess().getResources(Room.class);
		String roomId;
		if(rooms.isEmpty())
			roomId = "System";
		else
			roomId = ResourceUtils.getValidResourceName(rooms.get(0).getLocation());
		for(AlarmingMessagingApp mapp: persistData.messagingApps().getAllElements()) {
			FirebaseUtil.sendMessageToUsers(title,
					firebaseMessage, title, firebaseMessage,
					additionalProperties, Arrays.asList(mapp.usersForPushMessage().getValues()),
					roomId, appManPlus,
					firebaseDebugInfoMessage);
		}
	}

}
