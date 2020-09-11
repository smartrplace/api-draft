package org.ogema.devicefinder.api;

import java.util.List;
import java.util.Map;

import org.ogema.devicefinder.api.OGEMADriverPropertyService.AccessAvailability;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** TODO: Not used yet, to discuss whether we need this class
 * TODO: To discuss whether this class should have read/write operations like {@link PropertyService}. Otherwise
 * 		access would only be possible via {@link PropertyService}, which should support also reading resources
 * 		via {@link GaRoDataType}s packed into PropTypes.
 * @author dnestle
 *
 */
public class PropInstanceData extends PropType {
	/** If the property instance has a datapoint e.g. in the resource tree then
	 * this information shall be provided here. If null then the property usually is
	 * a real property only available via an {@link OGEMADriverPropertyService}.
	 */
	public final Datapoint dp;
	
	PropInstanceData(String id, List<PropUsage> relevantDevices, AccessAvailability access, Class<?> type,
			Map<OgemaLocale, String> description, PropAccessLevel accessLevel,
			Datapoint dp) {
		super(id, relevantDevices, access, type, description, accessLevel);
		this.dp = dp;
	}

}
