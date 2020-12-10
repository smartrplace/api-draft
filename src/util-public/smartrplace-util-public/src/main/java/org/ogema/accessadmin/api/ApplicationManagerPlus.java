package org.ogema.accessadmin.api;

import org.ogema.accesscontrol.PermissionManager;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.core.resourcemanager.ResourceManagement;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.core.security.WebAccessManager;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.recordeddata.DataRecorder;

import de.iwes.widgets.api.OgemaGuiService;
import de.iwes.widgets.api.services.MessagingService;

/** Collection of typical extended framework services for comprehensive access within applications*/
public class ApplicationManagerPlus {
	private ApplicationManager appMan;
	private OgemaGuiService guiService;
	private DatapointService dpService;
	private UserPermissionService userPermService;
	private PermissionManager permMan;
	private MessagingService messagingService;
	private DataRecorder dataRecorder;
	
	public ApplicationManagerPlus(ApplicationManager appMan) {
		this.appMan = appMan;		
	}
	public ApplicationManagerPlus(ApplicationManager appMan, OgemaGuiService guiService, DatapointService dpService,
			UserPermissionService userPermService) {
		this.appMan = appMan;
		this.guiService = guiService;
		this.dpService = dpService;
		this.userPermService = userPermService;
		if(guiService != null)
			setMessagingService(guiService.getMessagingService());
	}

	public ApplicationManager appMan() {
		return appMan;
	}

	public void setAppMan(ApplicationManager appMan) {
		this.appMan = appMan;
	}

	public OgemaGuiService guiService() {
		return guiService;
	}

	public void setGuiService(OgemaGuiService guiService) {
		this.guiService = guiService;
		this.messagingService = guiService.getMessagingService();
	}

	public DatapointService dpService() {
		return dpService;
	}

	public void setDpService(DatapointService dpService) {
		this.dpService = dpService;
	}

	public UserPermissionService userPermService() {
		return userPermService;
	}

	public void setUserPermService(UserPermissionService userPermService) {
		this.userPermService = userPermService;
	}

	public PermissionManager permMan() {
		return permMan;
	}

	public void setPermMan(PermissionManager permMan) {
		this.permMan = permMan;
	}
	public MessagingService getMessagingService() {
		return messagingService;
	}
	public void setMessagingService(MessagingService messagingService) {
		this.messagingService = messagingService;
	}
	
	public long getFrameworkTime() {
		return appMan.getFrameworkTime();
	}
	public OgemaLogger getLogger() {
		return appMan.getLogger();
	}
	public ResourceAccess getResourceAccess() {
		return appMan.getResourceAccess();
	}
	public ResourcePatternAccess getResourcePatternAccess() {
		return appMan.getResourcePatternAccess();
	}
	public ResourceManagement getResourceManagement() {
		return appMan.getResourceManagement();
	}
	public WebAccessManager getWebAccessManager() {
		return appMan.getWebAccessManager();
	}
	public DataRecorder dataRecorder() {
		return dataRecorder;
	}
	public void setDataRecorder(DataRecorder dataRecorder) {
		this.dataRecorder = dataRecorder;
	}
}
