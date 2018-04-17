package org.smartrplace.smarteff.defaultservice;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.ogema.core.application.Application.AppStopReason;
import org.ogema.core.model.Resource;
import org.smartrplace.efficiency.api.base.SmartEffExtensionService;
import org.smartrplace.efficiency.api.base.SmartEffResource;
import org.smartrplace.extensionservice.ApplicationManagerSPExt;
import org.smartrplace.extensionservice.ExtensionCapability;
import org.smartrplace.extensionservice.ExtensionResourceTypeDeclaration;
import org.smartrplace.smarteff.defaultproposal.BuildingExampleAnalysis;
import org.smartrplace.smarteff.util.NaviPageBase;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.base.BuildingData;
import extensionmodel.smarteff.api.base.SmartEffGeneralData;
import extensionmodel.smarteff.api.base.SmartEffPriceData;
import extensionmodel.smarteff.example.DefaultProviderParamsPage;

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
		public String label(OgemaLocale locale) {
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
	public final static ExtensionResourceTypeDeclaration<SmartEffResource> PRICE_DATA = new ExtensionResourceTypeDeclaration<SmartEffResource>() {

		@Override
		public Class<? extends SmartEffResource> dataType() {
			return SmartEffPriceData.class;
		}

		@Override
		public String label(OgemaLocale locale) {
			return "Basic Price Data";
		}

		@Override
		public Class<? extends SmartEffResource> parentType() {
			return SmartEffGeneralData.class;
		}

		@Override
		public Cardinality cardinality() {
			return Cardinality.SINGLE_VALUE_REQUIRED;
		}
		
	};
	/*public final static org.smartrplace.smarteff.defaultservice.BuildingTablePage.Provider BUILDING_NAVI_PROVIDER = new BuildingTablePage().provider;
	public final static org.smartrplace.smarteff.defaultservice.BuildingEditPage.Provider BUILDING_EDIT_PROVIDER = new BuildingEditPage().provider;
	*/
	public final static NaviPageBase<Resource>.Provider RESOURCE_NAVI_PROVIDER = new ResourceTablePage().provider;
	public final static NaviPageBase<Resource>.Provider RESOURCEALL_NAVI_PROVIDER = new ResourceAllTablePage().provider;
	public final static NaviPageBase<Resource>.Provider PROPOSALTABLE_PROVIDER = new ProposalProvTablePage().provider;
	public final static NaviPageBase<Resource>.Provider RESULTTABLE_PROVIDER = new ResultTablePage().provider;
	//public final static NaviPageBase<DefaultProviderParams>.Provider BA_PARAMSEDIT_PROVIDER = new DefaultProviderParamsPage().provider;
	//public final static NaviPageBase<Resource>.Provider TOPCONFIG_NAVI_PROVIDER = new TopConfigTablePage().provider;
	public BuildingExampleAnalysis BUILDINGANALYSIS_PROVIDER;
	@Override
	public void start(ApplicationManagerSPExt appManExt) {
		//this.appManExt = appManExt;
		BUILDINGANALYSIS_PROVIDER = new BuildingExampleAnalysis(appManExt);
	}

	@Override
	public void stop(AppStopReason reason) {
	}

	@Override
	public Collection<ExtensionCapability> getCapabilities() {
		return Arrays.asList(new ExtensionCapability[] {new BuildingTablePage().provider, new BuildingEditPage().provider, RESOURCE_NAVI_PROVIDER, RESOURCEALL_NAVI_PROVIDER,
				PROPOSALTABLE_PROVIDER, RESULTTABLE_PROVIDER, new TopConfigTablePage().provider,
				BUILDINGANALYSIS_PROVIDER, new DefaultProviderParamsPage().provider});
	}

	@Override
	public Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> resourcesDefined() {
		Collection<ExtensionResourceTypeDeclaration<? extends SmartEffResource>> result = 
				new ArrayList<>();
		result.add(BUILDING_DATA);
		result.add(PRICE_DATA);
		result.add(BUILDINGANALYSIS_PROVIDER.getTypeDeclaration());
		if(BUILDINGANALYSIS_PROVIDER.getParamTypeDeclaration() != null) result.add(BUILDINGANALYSIS_PROVIDER.getParamTypeDeclaration());
		return result ;
	}
}
