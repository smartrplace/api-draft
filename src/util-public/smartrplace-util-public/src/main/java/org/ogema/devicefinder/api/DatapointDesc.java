package org.ogema.devicefinder.api;

import java.util.Map;
import java.util.Set;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper;
import de.iwes.timeseries.eval.garo.api.helper.base.GaRoEvalHelper.RecIdVal;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.template.LabelledItem;

/** Description of data point like {@link GaRoDataTypeI}, type of consumption representation etc.
 */
public interface DatapointDesc extends LabelledItem {
	GaRoDataType getGaroDataType();
	
	/** See {@link DatapointDescAccess#setLabelDefault(String))}. If no label is specified at all then this method may
	 * return null whereas the method {@link #label(OgemaLocale)} shall always return a readable String. So
	 * getDefaultLabel() and label(null) will return the same result exactly when a label is defined.
	 * 
	 * @return
	 */
	String labelDefault();
	
	/** See {@link DatapointDescAccess#addAlias(String)}*/
	Set<String> getAliases();
	
	/** If labels if all languages shall be used e.g. for the generation of labels of a dependent
	 * Datapoint then all labels are needed
	 * @return
	 */
	Map<OgemaLocale, String> getAllLabels();
	
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
	
	public static interface ScalingProvider {
		/** Provide scaled value
		 * 
		 * @param rawValue
		 * @param timeStamp the scale may depend on the time. If null is given a default
		 * 		scale shall be applied
		 * @return
		 */
		float getStdVal(float rawValue, Long timeStamp);
	}
	/** Provides a scaling for the values in the datapoint. The scaling must be applied
	 * by any application using the values.
	 * @return null if no scaling is defined. In this case the values shall be used
	 * 		unscaled.
	 */
	ScalingProvider getScale();
}
