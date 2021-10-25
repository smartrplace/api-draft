package org.smartrplace.util.message;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.messaging.api.MessageDTO;
import org.ogema.messaging.api.MessageTransport;
import org.ogema.messaging.api.MessagingConstants;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;
import org.ogema.tools.app.useradmin.api.UserDataAccess;
import org.ogema.tools.app.useradmin.config.MessagingAddress;

import de.iwes.widgets.api.extended.util.UserLocaleUtil;

public class FirebaseUtil {
	public static void sendMessageToUsers(String titleEN, String messageEN, String titleDE, String messageDE,
			Map<String, Object> additionalProperties,
			Collection<String> userNames, String collapseKeyGroupId,
			ApplicationManagerPlus appMan,
			String infoString) {
		UserDataAccess uad = appMan.dpService().userAdminDataService();
		MessageTransport mts = appMan.dpService().messageTransportService("firebase_token");
		if(uad != null && mts != null) {
			appMan.getLogger().info(infoString+titleEN+", message:"+messageEN);
			long now = appMan.getFrameworkTime();
			long validUntil = now + 10*TimeProcUtil.MINUTE_MILLIS;

			MessageDTO msgEN = new MessageDTO();
			MessageDTO msgDE = new MessageDTO();
			msgEN.messageProperties = new HashMap<>();
			msgEN.messageProperties.put("title", titleEN);
			msgEN.messageProperties.put("message", messageEN);
			msgEN.messageProperties.put("validUtil", validUntil);
			if(additionalProperties != null)
				msgEN.messageProperties.putAll(additionalProperties);
			if(titleDE != null) {
				msgDE.messageProperties = new HashMap<>();
				msgDE.messageProperties.put("title", titleDE);
				msgDE.messageProperties.put("message", messageDE);
				msgDE.messageProperties.put("validUtil", validUntil);
				if(additionalProperties != null)
					msgDE.messageProperties.putAll(additionalProperties);
			}

			Set<String> allAddressesDE = new HashSet<>();
			Set<String> allAddressesEN = new HashSet<>();
			
			for(String user: userNames) {
				try {
					List<MessagingAddress> userAddresses = uad.getMessagingAddresses(user, MessagingConstants.ADDRESS_TYPE_FIREBASE_TOKEN);
					if(titleDE == null) {
						for(MessagingAddress addr: userAddresses) {
							allAddressesEN.add(addr.address().getValue());
						}
						continue;
					}
					
					boolean isGerman = UserLocaleUtil.getLocaleString(user, appMan.appMan()).equals("de");
					for(MessagingAddress addr: userAddresses) {
						if(isGerman)
							allAddressesDE.add(addr.address().getValue());
						else
							allAddressesEN.add(addr.address().getValue());
					}
				} catch(IllegalArgumentException e) {
					System.out.println("Trying to send to UNKNOWN user: "+user);
					continue;
				}
			}
			
			sendMessage(msgEN, allAddressesEN, collapseKeyGroupId, mts);
			if(titleDE != null)
				sendMessage(msgDE, allAddressesDE, collapseKeyGroupId, mts);
			
			appMan.getLogger().info("SENDING VIA FIREBASE:"+ titleEN+
					", message:"+messageEN+" to "+allAddressesEN.size()+" in English, "+
					allAddressesDE.size()+" in German.");
		} else
			appMan.getLogger().info("NOT POSSIBLE via Firebase:"+titleEN+", message:"+messageEN);
	}
	public static void sendMessage(MessageDTO msg, Collection<String> allAddresses, String collapseKeyGroupId,
			MessageTransport mts) {
		if(!allAddresses.isEmpty()) {
			msg.recipients = new ArrayList<>(allAddresses);
			msg.messageProperties.put(MessagingConstants.COLLAPSE_KEY, collapseKeyGroupId);
			msg.messageProperties.put(MessagingConstants.TTL, 2*TimeProcUtil.HOUR_MILLIS);
			mts.sendMessage(msg);
		}
		
	}
}
