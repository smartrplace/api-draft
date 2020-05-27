package org.ogema.devicefinder.api;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper.RecIdVal;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Description of data point like {@link GaRoDataTypeI}, type of consumption representation etc.
 */
public interface DatapointDesc {
	GaRoDataType getGaroDataType();
	
	String label();

	/** The method getTypeName shall always return a non-null String even if no type 
	 * information is available;
	 */
	default String getTypeName(OgemaLocale locale) {
		GaRoDataType type = getGaroDataType();
		if(type != null)
			return getTypeLabel(type.label(null), locale);
		return "noTypeInfo";
	};

	DPRoom getRoom();
	
	/** Get a short label indicating the position within the room
	 * 
	 * @param locale
	 * @param context to be defined in the future, insert null for now
	 * @return null if no such label is available
	 */
	String getSubRoomLocation(OgemaLocale locale, Object context);
	
	/** See {@link DatapointInfo} for more information. This method shall NOT return null,
	 * but shall always return an object that provides information based on all datapoint information available.
	 * @return
	 */
	DatapointInfo info();
	
	/** If true the timeseries belongs to the local gateway*/
	Boolean isLocal();
	
	/** Get label for a GaRoDataType.
	 * 
	 * @param id usally GaRoDataTypeI.label(null)
	 * @param locale
	 * @return
	 */
	public static String getTypeLabel(String id, OgemaLocale locale) {
		RecIdVal entry = GaRoEvalHelper.recIdSnippets.get(id);
		if(entry == null) return id;
		String ger = entry.label.get(locale);
		if(ger != null)
			return ger;
		return entry.label.get(OgemaLocale.ENGLISH);
	}
}
