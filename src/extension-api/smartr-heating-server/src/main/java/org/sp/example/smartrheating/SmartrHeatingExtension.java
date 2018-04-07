package org.sp.example.smartrheating;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application.AppStopReason;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;

import extensionmodel.smarteff.api.base.BuildingData;

@Service(SmartEffExtensionService.class)
@Component
public class SmartrHeatingExtension implements SmartEffExtensionService {
	private ApplicationManagerSPExt appManExt;
	
	@Override
	public void start(ApplicationManagerSPExt appManExt) {
		this.appManExt = appManExt;
		//Do nothing more here !
	}

	@Override
	public void stop(AppStopReason reason) {
	}

	@Override
	public Collection<ExtensionCapability> getCapabilities() {
		return Arrays.asList(new ExtensionCapability[] {new SmartrHeatingEditPage(appManExt),
				new SmartrHeatingRecommendationProvider()});
	}

	@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> resourcesDefined() {
		Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> result = 
				new ArrayList<>();
		result.add(new ExtensionResourceTypeDeclaration<SmartEffResource>() {

			@Override
			public Class<? extends SmartEffResource> dataType() {
				return SmartrHeatingData.class;
			}

			@Override
			public String resourceName() {
				return "smartrHeatingData";
			}

			@Override
			public Class<? extends SmartEffResource> parentType() {
				return BuildingData.class;
			}

			@Override
			public Cardinality cardinality() {
				return Cardinality.SINGLE_VALUE_REQUIRED;
			}
			
		});
		return result ;
	}
}
