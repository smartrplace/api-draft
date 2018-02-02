package org.smartrplace.efficiency.api;

import java.util.List;

import org.ogema.model.locations.Building;
import org.ogema.model.user.NaturalPerson;
import org.smartrplace.efficiency.model.SmartEffBaseData;
import org.smartrplace.efficiency.model.SmartEffUserData;

import de.iwes.timeseries.eval.api.LabelledItem;

/** To be provided as OSGi service
 * TODO: This is only a rough draft. Much more entries and probably a more complex data structure has to be defined.
 * */
public interface RecommendationProvider {
	/** TODO: Should we rather provide a Recommendation resource here? This makes the interface much more flexible,
	if an older version of a data model is used just some fields may be missing, would be replaced by default value
	*/
	public interface Recommendation extends LabelledItem {
		float getNetInvestment();
		float getAnnualNetSavings();
		float getAnnualCO2Savings();
		//...
	}
	
	/**
	 * 
	 * @param userData
	 * @param user
	 * @param building also BuildingPropertyUnit should be possible here, maybe parameters need to be more flexible anyways.
	 * 		we have an extended GaRo-like hierarchy here.
	 * @return
	 */
	public List<Recommendation> getRecommendations(SmartEffUserData userData, NaturalPerson user, Building building,
			SmartEffBaseData baseData);
}
