package org.ogema.devicefinder.util;

import org.ogema.devicefinder.api.DatapointInfo;
import org.smartrplace.util.format.WidgetHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.DatapointDesc;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** To generate datapoint description objects this implementation can be inherited or
 * own implementations can be made
 */
public class DatapointDescImpl implements DatapointDesc {
	protected GaRoDataType garoDataType = null;
	
	/** Type name is only relevant if garoDataType == null. Then a special type name may
	 * be given*/
	protected Map<OgemaLocale, String> typeName = new HashMap<>();
	protected String labelDefault;
	protected Map<OgemaLocale, String> labels = new HashMap<>();
	protected DPRoom dpRoom = null;
	protected DatapointInfo consumptionInfo = null;
	protected String subRoomLocation = null;
	protected Boolean isLocal = null;

	public DatapointDescImpl(GaRoDataType garoDataType, DPRoom dpRoom, DatapointInfo consumptionInfo,
			String subRoomLocation, Boolean isLocal) {
		this.garoDataType = garoDataType;
		this.dpRoom = dpRoom;
		this.consumptionInfo = consumptionInfo;
		this.subRoomLocation = subRoomLocation;
		this.isLocal = isLocal;
	}
	
	@Override
	public GaRoDataType getGaroDataType() {
		return garoDataType;
	}

	@Override
	public String getTypeName(OgemaLocale locale) {
		if(garoDataType == null || garoDataType == GaRoDataType.Unknown) {
			String result = typeName.get(locale);
			if(result != null)
				return result;
			result = typeName.get(OgemaLocale.ENGLISH);
			if(result != null)
				return result;
		}
		return DatapointDesc.super.getTypeName(locale);
	}
	
	@Override
	public DPRoom getRoom() {
		return dpRoom;
	}

	@Override
	/** In the standard description implementation locale and context are not used*/
	public String getSubRoomLocation(OgemaLocale locale, Object context) {
		return subRoomLocation;
	}

	@Override
	public DatapointInfo info() {
		return consumptionInfo;
	}

	@Override
	public Boolean isLocal() {
		return isLocal;
	}

	@Override
	public String id() {
		return WidgetHelper.getValidWidgetId(label(null));
	}

	@Override
	public String label(OgemaLocale locale) {
		String result;
		if(locale == null)
			result = labelDefault;
		else {
			result = labels.get(locale);
			if(result == null)
				result = labelDefault;
		}
		if(result == null) {
			return this.toString();
		}
		return result;
	}

	@Override
	public String labelDefault() {
		return labelDefault;
	}

	@Override
	public Map<OgemaLocale, String> getAllLabels() {
		return Collections.unmodifiableMap(labels);
	}

}
