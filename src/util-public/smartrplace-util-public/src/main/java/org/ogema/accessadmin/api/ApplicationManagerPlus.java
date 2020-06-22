package org.ogema.accessadmin.api;

import org.ogema.accesscontrol.PermissionManager;
import org.ogema.core.application.ApplicationManager;
import org.ogema.devicefinder.api.DatapointService;

import de.iwes.widgets.api.OgemaGuiService;

/** Collection of typical extended framework services for comprehensive access within applications*/
public class ApplicationManagerPlus {
	private ApplicationManager appMan;
	private OgemaGuiService guiService;
	private DatapointService dpService;
	private UserPermissionService userPermService;
	private PermissionManager permMan;
	
	public ApplicationManagerPlus(ApplicationManager appMan) {
		this.appMan = appMan;		
	}
	public ApplicationManagerPlus(ApplicationManager appMan, OgemaGuiService guiService, DatapointService dpService,
			UserPermissionService userPermService) {
		this.appMan = appMan;
		this.guiService = guiService;
		this.dpService = dpService;
		this.userPermService = userPermService;
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
}
