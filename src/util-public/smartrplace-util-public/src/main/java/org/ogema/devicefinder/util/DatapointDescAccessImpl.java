package org.ogema.devicefinder.util;

import org.ogema.devicefinder.api.DatapointInfo;
import org.ogema.model.prototypes.PhysicalElement;
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
	public boolean setDataTypeName(String typeName, OgemaLocale locale) {
		this.typeName.put((locale!=null)?locale:OgemaLocale.ENGLISH, typeName);
		return true;
	}
	
	@Override
	public boolean setLabelDefault(String label) {
		this.labelDefault = label;
		if(labels.get(OgemaLocale.ENGLISH) == null)
			labels.put(OgemaLocale.ENGLISH, label);
		return true;
	}

	@Override
	public boolean setRoom(DPRoom room) {
		if(isRoomFinal && dpRoom != null)
			return false;
		dpRoom = room;
		return true;
	}

	@Override
	public boolean setRoom(DPRoom room, boolean isFinal) {
		this.isRoomFinal = isFinal; 
		dpRoom = room;
		return true;		
	}

	@Override
	public boolean setDevice(PhysicalElement device) {
		dpDevice = device;
		return true;
	}
	
	@Override
	public boolean setSubRoomLocation(OgemaLocale locale, Object context, String value) {
		return addToSubRoomLocationAtomic(locale, context, value, true);
	}
	
	@Override
	public boolean addToSubRoomLocationAtomic(OgemaLocale locale, Object context, String value, boolean isFirstElement) {
		if(subRoomLocation == null) {
			subRoomLocation = value;
			return true;
		}

		synchronized(subRoomLocation) {
			//String existing = getSubRoomLocation(null, null);
			if(subRoomLocation != null && (!subRoomLocation.isEmpty())) {
				if(subRoomLocation.contains(value))
					return false;
				if(isFirstElement)
					value = value+"-"+subRoomLocation;
				else
					value = subRoomLocation+"-"+value;
			}
			subRoomLocation = value;
			return true;
		}
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
	
	@Override
	public boolean addAlias(String alias) {
		aliases.add(alias);
		return true;
	}

}
