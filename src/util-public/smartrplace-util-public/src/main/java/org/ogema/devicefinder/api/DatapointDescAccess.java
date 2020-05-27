package org.ogema.devicefinder.api;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Description of data point like {@link GaRoDataTypeI}, type of consumption representation etc.
 * Note that callling the setters may have no effect if the respective value is set by the
 * {@link DatapointInfoProvider} of the datapoint object.
 */
public interface DatapointDescAccess extends DatapointDesc {
	/** Set GaroDataType
	 * @return true if successful*/
	boolean setGaroDataType(GaRoDataType type);
	
	boolean setLabel(String label);
	
	boolean setRoom(DPRoom room);
	
	boolean setSubRoomLocation(OgemaLocale locale, Object context, String value);
	
	@Deprecated
	boolean setConsumptionInfo(DatapointInfo info);
}
