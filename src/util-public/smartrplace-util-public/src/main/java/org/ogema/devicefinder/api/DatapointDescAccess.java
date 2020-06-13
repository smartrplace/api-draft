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
	
	/** If this name is set is will be used in priority to the type name
	 * based on GaRoDataType. Then this
	 * type name will be returned on {@link #getTypeName(OgemaLocale)}.
	 * @param typeName
	 * @param locale see {@link #setLabel(String, OgemaLocale)}
	 * @return true if successful
	 */
	boolean setDataTypeName(String typeName, OgemaLocale locale);
	
	/** The default label can be set explicitly or is determined by the locale labels. The default label shall be used
	 * if no fitting language label is available. If no other setting is made then the ENGLISH label shall be used
	 * as default.
	 * @param label
	 * @return
	 */
	boolean setLabelDefault(String label);
	
	/** See {@link #setDefaultLabel(String)}
	 * 
	 * @param label
	 * @param locale
	 * @return
	 */
	boolean setLabel(String label, OgemaLocale locale);
	
	boolean setRoom(DPRoom room);
	
	boolean setSubRoomLocation(OgemaLocale locale, Object context, String value);
	
	@Deprecated
	boolean setConsumptionInfo(DatapointInfo info);
}
