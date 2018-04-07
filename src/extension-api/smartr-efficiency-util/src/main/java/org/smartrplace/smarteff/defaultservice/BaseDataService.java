package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application.AppStopReason;
import org.ogema.core.model.Resource;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.util.NaviPageBase;

import extensionmodel.smarteff.api.base.BuildingData;
import extensionmodel.smarteff.api.base.SmartEffPriceData;

@Service(SmartEffExtensionService.class)
@Component
public class BaseDataService implements SmartEffExtensionService {
	//private ApplicationManagerSPExt appManExt;
	
	public final static ExtensionResourceTypeDeclaration<SmartEffResource> BUILDING_DATA = new ExtensionResourceTypeDeclaration<SmartEffResource>() {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return BuildingData.class;
		}

		@Override
		public String resourceName() {
			return "Building Data";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return null;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.MULTIPLE_OPTIONAL;
		}
		
	};
	public final static org.smartrplace.smarteff.defaultservice.BuildingTablePage.Provider BUILDING_NAVI_PROVIDER = new BuildingTablePage().provider;
	public final static org.smartrplace.smarteff.defaultservice.BuildingEditPage.Provider BUILDING_EDIT_PROVIDER = new BuildingEditPage().provider;
	public final static NaviPageBase<Resource>.Provider RESOURCE_NAVI_PROVIDER = new ResourceTablePage().provider;
	public final static NaviPageBase<Resource>.Provider RESOURCEALL_NAVI_PROVIDER = new ResourceAllTablePage().provider;
	
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
		return Arrays.asList(new ExtensionCapability[] {BUILDING_NAVI_PROVIDER, BUILDING_EDIT_PROVIDER, RESOURCE_NAVI_PROVIDER, RESOURCEALL_NAVI_PROVIDER});
	}

	@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> resourcesDefined() {
		Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> result = 
				new ArrayList<>();
		result.add(BUILDING_DATA);
		result.add(new ExtensionResourceTypeDeclaration<SmartEffResource>() {

			@Override
			public Class<? extends SmartEffResource> dataType() {
				return SmartEffPriceData.class;
			}

			@Override
			public String resourceName() {
				return "Basic Price Data";
			}

			@Override
			public Class<? extends SmartEffResource> parentType() {
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
