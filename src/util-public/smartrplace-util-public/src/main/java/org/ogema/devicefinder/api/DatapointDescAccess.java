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
	
	/** Via an alias a datapoint can be accessed like via the location. Aliases are usually
	 * Strings that represent a certain functionality like "mainMeterTotalConsumptionDaily". See the constants
	 * defined in class {@link Datapoint} for standard aliases.
	 * @param alias
	 * @return
	 */
	boolean addAlias(String alias);
	
	/** See {@link #setDefaultLabel(String)}
	 * 
	 * @param label
	 * @param locale
	 * @return
	 */
	boolean setLabel(String label, OgemaLocale locale);
	
	boolean setRoom(DPRoom room);
	
	boolean setSubRoomLocation(OgemaLocale locale, Object context, String value);
	/** If the sub room location already exists and it does not contain the new String yet then
	 * the new String is added with a hyphen
	 * @param locale
	 * @param context
	 * @param value
	 * @param if true then the element will be added to the beginning of the String followed by a hyphen, otherwise appended
	 * @return
	 */
	boolean addToSubRoomLocationAtomic(OgemaLocale locale, Object context, String value, boolean isFirstElement);
	
	@Deprecated
	boolean setConsumptionInfo(DatapointInfo info);
}
