package org.sp.example.smartrheating;

import java.util.ArrayList;
import java.util.List;

import org.smartrplace.efficiency.api.base.SmartEffExtensionResourceType;
import org.smartrplace.efficiency.api.capabilities.RecommendationProvider;
import org.smartrplace.extensionservice.gui.DataEntryProvider;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import extensionmodel.smarteff.api.base.BuildingData;
import extensionmodel.smarteff.api.base.SmartEffGeneralData;
import extensionmodel.smarteff.api.base.SmartEffUserData;

public class SmartrHeatingRecommendationProvider implements RecommendationProvider {

	@Override
	public String id() {
		return SmartrHeatingRecommendationProvider.class.getSimpleName();
	}

	@Override
	public String label(OgemaLocale locale) {
		return "SmartrHeating Recommendations";
	}

	@Override
	public String description(OgemaLocale locale) {
		return label(locale);
	}

	@Override
	public List<Class<? extends SmartEffExtensionResourceType>> inputResourceTypes() {
		List<Class<? extends SmartEffExtensionResourceType>> result = new ArrayList<>();
		result .add(BuildingData.class);
		result.add(SmartrHeatingData.class);
		return result;
	}

	@Override
	public void updateRecommendations(SmartEffUserData userData, SmartEffGeneralData generalData,
			List<SmartEffExtensionResourceType> resourcesChanged, List<Recommendation> recommendations) {
		final List<BuildingData> buildings;
		if(resourcesChanged == null) {
			buildings = userData.buildings().getAllElements();
		} else
			buildings = getBuildingsChanged(resourcesChanged);
		for(BuildingData b: buildings) {
			SrtrHeatingRecommendation rec = checkBuilding(b);
			for(Recommendation exist: recommendations) {
				if(((SrtrHeatingRecommendation)exist).building.equalsLocation(b)) {
					recommendations.remove(exist);
					if(rec != null) recommendations.add(rec);
					break;
				}
			}
		}
	}

	private class SrtrHeatingRecommendation implements Recommendation {
		public BuildingData building;
		
		@Override
		public String id() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String label(OgemaLocale locale) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String description(OgemaLocale locale) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public float getNetInvestment() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public float getAnnualNetSavings() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public float getAnnualCO2Savings() {
			// TODO Auto-generated method stub
			return 0;
		}
		
	}
	
	private List<BuildingData> getBuildingsChanged(List<SmartEffExtensionResourceType> resourcesChanged) {
		//TODO
		return null;
	}
	private SrtrHeatingRecommendation checkBuilding(BuildingData building) {
		//TODO
		return null;
	}

	@Override
	public <S extends SmartEffExtensionResourceType> DataEntryProvider<S> resultPageDefinition() {
		// TODO: A result page is foreseen in the mockup
		return null;
	}
}
