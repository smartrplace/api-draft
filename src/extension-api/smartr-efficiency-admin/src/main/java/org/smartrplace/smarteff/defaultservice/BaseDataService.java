package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application.AppStopReason;
import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;

import extensionmodel.smarteff.api.base.BuildingData;
import extensionmodel.smarteff.api.base.SmartEffPriceData;

@Service(SmartEffExtensionService.class)
@Component
public class BaseDataService implements SmartEffExtensionService {
	//private ApplicationManagerSPExt appManExt;
	
	public final static ExtensionResourceTypeDeclaration<SmartEffExtensionResourceType> BUILDING_DATA = new ExtensionResourceTypeDeclaration<SmartEffExtensionResourceType>() {

		@Override
		public Class<? extends SmartEffExtensionResourceType> dataType() {
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
	public final static org.smartrplace.smarteff.defaultservice.BuildingTablePage.Provider BUILDING_NAVI_PROVIDER = new BuildingTablePage().provider;
	public final static org.smartrplace.smarteff.defaultservice.BuildingEditPage.Provider BUILDING_EDIT_PROVIDER = new BuildingEditPage().provider;
	
	@Override
	public void start(ApplicationManagerSPExt appManExt) {
		//this.appManExt = appManExt;
		//Do nothing more here !
	}

	@Override
	public void stop(AppStopReason reason) {
	}

	@Override
	public Collection<ExtensionCapability> getCapabilities() {
		return Arrays.asList(new ExtensionCapability[] {BUILDING_NAVI_PROVIDER, BUILDING_EDIT_PROVIDER});
	}

	@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffExtensionResourceType>> resourcesDefined() {
		Collection<ExtensionResourceTypeDeclaration<? extends SmartEffExtensionResourceType>> result = 
				new ArrayList<>();
		result.add(BUILDING_DATA);
		result.add(new ExtensionResourceTypeDeclaration<SmartEffExtensionResourceType>() {

			@Override
			public Class<? extends SmartEffExtensionResourceType> dataType() {
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
