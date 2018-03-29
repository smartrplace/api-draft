package org.smartrplace.smarteff.admin.object;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.capabilities.SmartEffRecommendationProvider;
import org.smartrplace.extenservice.proposal.ProposalProvider;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceType;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.extensionservice.gui.NavigationGUIProvider;
import org.smartrplace.smarteff.admin.SpEffAdminController;
import org.smartrplace.smarteff.admin.util.SmartrEffUtil;

public class SmartrEffExtResourceTypeData {
	public final Class<? extends ExtensionResourceType> resType;
	public final ExtensionResourceTypeDeclaration<? extends ExtensionResourceType> typeDeclaration;
	public final List<SmartEffExtensionService> requiredBy = new ArrayList<>();
	public int numberTotal;
	public int numberPublic;
	public int numberNonEdit;
	
	public SmartrEffExtResourceTypeData(ExtensionResourceTypeDeclaration<? extends SmartEffExtensionResourceType> typeDeclaration,
			SmartEffExtensionService parent, SpEffAdminController app) {
		this.resType = typeDeclaration.dataType();
		this.typeDeclaration = typeDeclaration;
		addParent(parent);
		if(app != null) resetResourceStatistics(app);
	}
	
	public void resetResourceStatistics(SpEffAdminController app) {
		numberTotal = app.appMan.getResourceAccess().getResources(resType).size();
		numberPublic = app.appConfigData.generalData().getSubResources(resType, true).size();
		numberNonEdit = app.appConfigData.userDataNonEdit().getSubResources(resType, true).size();		
	}

	public void addParent(SmartEffExtensionService parent) {
		this.requiredBy.add(parent);
	}
	
	/** 
	 * 
	 * @param parent
	 * @return true if the type is not used anymore
	 */
	public boolean removeParent(SmartEffExtensionService parent) {
		this.requiredBy.remove(parent);
		if(requiredBy.isEmpty()) return true;
		else return false;
	}
	
	public static class ServiceCapabilities {
		public final Set<ProposalProvider> proposalProviders = new HashSet<>();
		public final Set<NavigationGUIProvider> naviProviders = new HashSet<>();
		public final Set<SmartEffRecommendationProvider> recommendationProviders = new HashSet<>();
		public final Set<ExtensionCapability> otherProviders = new HashSet<>();
	}
	public static ServiceCapabilities getServiceCaps(SmartEffExtensionService service) {
		ServiceCapabilities result = new ServiceCapabilities();
    	for(ExtensionCapability c: service.getCapabilities()) {
    		if(c instanceof ProposalProvider) result.proposalProviders.add((ProposalProvider) c);
    		else if(c instanceof SmartEffRecommendationProvider) result.recommendationProviders.add((SmartEffRecommendationProvider) c);
    		else if(c instanceof NavigationGUIProvider) result.naviProviders.add((NavigationGUIProvider) c);
    		else result.otherProviders.add(c);
    	}
		return result;
	}

	@SuppressWarnings("incomplete-switch")
	public void registerElement(ExtensionResourceType res) {
		numberTotal++;
		switch(SmartrEffUtil.getAccessType(res)) {
		case PUBLIC:
			numberPublic++;
			break;
		case READONLY:
			numberNonEdit++;
			break;
		}		
	}
	@SuppressWarnings("incomplete-switch")
	public void unregisterElement(SmartEffExtensionResourceType res) {
		numberTotal--;
		switch(SmartrEffUtil.getAccessType(res)) {
		case PUBLIC:
			numberPublic--;
			break;
		case READONLY:
			numberNonEdit--;
			break;
		}		
	}
	
	@Override
	public String toString() {
		return ResourceUtils.getValidResourceName(resType.getName());
	}
}
