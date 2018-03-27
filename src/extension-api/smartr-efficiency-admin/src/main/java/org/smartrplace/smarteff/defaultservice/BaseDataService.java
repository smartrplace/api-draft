package org.smartrplace.smarteff.defaultservice;

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
import extensionmodel.smarteff.api.base.SmartEffPriceData;

@Service(SmartEffExtensionService.class)
@Component
public class BaseDataService implements SmartEffExtensionService {
	public final static ExtensionResourceTypeDeclaration<SmartEffExtensionResourceType> BUILDING_DATA = new ExtensionResourceTypeDeclaration<SmartEffExtensionResourceType>() {

		@Override
		public Class<? extends SmartEffExtensionResourceType> resourceType() {
			return BuildingData.class;
		}

		@Override
		public String resourceName() {
			return "Building Data";
		}

		@Override
		public Class<? extends SmartEffExtensionResourceType> parentType() {
			return null;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.MULTIPLE_OPTIONAL;
		}
		
	};
	static {
		
	}
	
	//private ApplicationManagerMinimal appManMin;
	
	@Override
	public void start(ApplicationManagerMinimal appManMin) {
		//this.appManMin = appManMin;
		//Do nothing more here !
	}

	@Override
	public void stop(AppStopReason reason) {
	}

	@Override
	public Collection<ExtensionCapability> getCapabilities() {
		return Arrays.asList(new ExtensionCapability[] {});
	}

	@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffExtensionResourceType>> resourcesDefined() {
		Collection<ExtensionResourceTypeDeclaration<? extends SmartEffExtensionResourceType>> result = 
				new ArrayList<>();
		result.add(BUILDING_DATA);
		result.add(new ExtensionResourceTypeDeclaration<SmartEffExtensionResourceType>() {

			@Override
			public Class<? extends SmartEffExtensionResourceType> resourceType() {
				return SmartEffPriceData.class;
			}

			@Override
			public String resourceName() {
				return "Basic Price Data";
			}

			@Override
			public Class<? extends SmartEffExtensionResourceType> parentType() {
				return null;
			}

			@Override
			public Cardinality cardinality() {
				return Cardinality.MULTIPLE_OPTIONAL;
			}
			
		});
		return result ;
	}
}
