package org.sp.example.smartrheating;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application.AppStopReason;
import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.ApplicationManagerMinimal;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;

import extensionmodel.smarteff.api.base.BuildingData;

@Service(SmartEffExtensionService.class)
@Component
public class SmartrHeatingExtension implements SmartEffExtensionService {
	private ApplicationManagerMinimal appManMin;
	
	@Override
	public void start(ApplicationManagerMinimal appManMin) {
		this.appManMin = appManMin;
		//Do nothing more here !
	}

	@Override
	public void stop(AppStopReason reason) {
	}

	@Override
	public Collection<ExtensionCapability> getCapabilities() {
		return Arrays.asList(new ExtensionCapability[] {new SmartrHeatingEditPage(appManMin),
				new SmartrHeatingRecommendationProvider()});
	}

	@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffExtensionResourceType>> resourcesDefined() {
		Collection<ExtensionResourceTypeDeclaration<? extends SmartEffExtensionResourceType>> result = 
				new ArrayList<>();
		result.add(new ExtensionResourceTypeDeclaration<SmartEffExtensionResourceType>() {

			@Override
			public Class<? extends SmartEffExtensionResourceType> resourceType() {
				return SmartrHeatingData.class;
			}

			@Override
			public String resourceName() {
				return "smartrHeatingData";
			}

			@Override
			public Class<? extends SmartEffExtensionResourceType> parentType() {
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
