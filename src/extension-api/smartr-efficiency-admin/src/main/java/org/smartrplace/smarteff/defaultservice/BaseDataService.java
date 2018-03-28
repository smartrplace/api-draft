package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application.AppStopReason;
import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.defaultservice.BuildingTablePage.Provider;
import org.smartrplace.util.directobjectgui.ApplicationManagerMinimal;

import extensionmodel.smarteff.api.base.BuildingData;
import extensionmodel.smarteff.api.base.SmartEffPriceData;

@Service(SmartEffExtensionService.class)
@Component
public class BaseDataService implements SmartEffExtensionService {
	//private ApplicationManagerMinimal appManMin;
	
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
	public final static Provider BUILDING_NAVI_PROVIDER = new BuildingTablePage().provider;
	/*@Deprecated
	public final static NavigationGUIProvider BUILDING_NAVI_PROVIDER2 = new NavigationGUIProvider() {
		//private ExtensionResourceType generalData;
		
		@Override
		public String label(OgemaLocale locale) {
			return "Standard Building Overview Table";
		}

		@Override
		public void initPage(ExtensionNavigationPage<?, ?> pageIn, ExtensionResourceType generalData) {
			// TODO Auto-generated method stub
			//this.generalData = generalData;
			@SuppressWarnings("unchecked")
			ExtensionNavigationPage<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData> page =
				(ExtensionNavigationPage<SmartEffUserDataNonEdit, ExtensionResourceAccessInitData>) pageIn;
			Label test = new Label(page.page, "test", "Hello World!");
			page.page.append(test);
		}

		@Override
		public void setUserData(int entryTypeIdx, List<ExtensionResourceType> entryResources,
				ExtensionResourceType userData, ExtensionUserDataNonEdit userDataNonEdit,
				ExtensionPageSystemAccessForCreate listener, OgemaHttpRequest req) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public List<EntryType> getEntryType() {
			return null;
		}
		
	};*/
	
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
		return Arrays.asList(new ExtensionCapability[] {BUILDING_NAVI_PROVIDER});
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
