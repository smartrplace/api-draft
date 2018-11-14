package org.smartrplace.tissue.util.resource;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.recordeddata.RecordedData;

import de.iwes.util.resource.ValueResourceHelper;

/** Intended to move into {@link ValueResourceHelper} in the future.
 * 
 */
public class ValueResourceHelperSP {
	/** Get Recorded data from SingleValueResource
	 * 
	 * @param valueResource
	 * @return null if type of resource does not support getHistoricalData
	 */
	public static RecordedData getRecordedData(SingleValueResource valueResource) {
		if(valueResource instanceof FloatResource)
			return ((FloatResource)valueResource).getHistoricalData();
		if(valueResource instanceof IntegerResource)
			return ((IntegerResource)valueResource).getHistoricalData();
		if(valueResource instanceof TimeResource)
			return ((TimeResource)valueResource).getHistoricalData();
		if(valueResource instanceof BooleanResource)
			return ((BooleanResource)valueResource).getHistoricalData();
		return null;
	}
}
