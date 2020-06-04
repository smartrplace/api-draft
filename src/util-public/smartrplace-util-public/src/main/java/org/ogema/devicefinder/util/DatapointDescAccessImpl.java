package org.ogema.devicefinder.util;

import org.ogema.devicefinder.api.DatapointInfo;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.DatapointDescAccess;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** To generate datapoint description objects this implementation can be inherited or
 * own implementations can be made
 */
public class DatapointDescAccessImpl extends DatapointDescImpl implements DatapointDescAccess {
	
	public DatapointDescAccessImpl(GaRoDataType garoDataType, DPRoom dpRoom, DatapointInfo consumptionInfo,
			String subRoomLocation) {
		super(garoDataType, dpRoom, consumptionInfo, subRoomLocation, null);
	}
	public DatapointDescAccessImpl() {
		this(null, null, null, null);
	}

	@Override
	public boolean setGaroDataType(GaRoDataType type) {
		garoDataType = type;
		return true;
	}
	
	@Override
	public boolean setLabelDefault(String label) {
		this.labelDefault = label;	
		return true;
	}

	@Override
	public boolean setRoom(DPRoom room) {
		dpRoom = room;
		return true;
	}

	@Override
	public boolean setSubRoomLocation(OgemaLocale locale, Object context, String value) {
		subRoomLocation = value;
		return true;
	}
	
	@Override
	public boolean setConsumptionInfo(DatapointInfo info) {
		this.consumptionInfo = info;
		return true;
	}
	@Override
	public String labelDefault() {
		return labelDefault;
	}
	@Override
	public boolean setLabel(String label, OgemaLocale locale) {
		if(locale == null)
			locale = OgemaLocale.ENGLISH;
		labels.put(locale,  label);
		if(labelDefault == null)
			labelDefault = label;
		else if(locale == OgemaLocale.ENGLISH)
			labelDefault = label;
		return true;
	}

}
