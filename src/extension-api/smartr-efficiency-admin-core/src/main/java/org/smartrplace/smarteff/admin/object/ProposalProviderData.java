package org.smartrplace.smarteff.admin.object;

import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extenservice.proposal.ProposalProvider;

public class ProposalProviderData {
	public final ProposalProvider provider;
	public final SmartEffExtensionService parent;
	
	public ProposalProviderData(ProposalProvider provider, SmartEffExtensionService parent) {
		this.provider = provider;
		this.parent = parent;
	}
	
}
