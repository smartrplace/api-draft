package org.smartrplace.smarteff.admin.util;

import java.util.List;

import org.smartrplace.extenservice.resourcecreate.ExtensionCapabilityForCreate;
import org.smartrplace.extenservice.resourcecreate.ProviderPublicDataForCreate;
import org.smartrplace.extensionservice.ExtensionResourceType;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class ProviderPublicDataForCreateImpl implements ProviderPublicDataForCreate {
	private final ExtensionCapabilityForCreate inputProvider;
	
	public ProviderPublicDataForCreateImpl(ExtensionCapabilityForCreate inputProvider) {
		this.inputProvider = inputProvider;
	}

	@Override
	public List<EntryType> getEntryType() {
		return inputProvider.getEntryType();
	}
	
	@Override
	public List<Class<? extends ExtensionResourceType>> createTypes() {
		return inputProvider.createTypes();
	}

	@Override
	public String id() {
		return SmartrEffUtil.buildId(inputProvider);
	}

	@Override
	public String label(OgemaLocale locale) {
		// TODO Auto-generated method stub
		return null;
	}

}
