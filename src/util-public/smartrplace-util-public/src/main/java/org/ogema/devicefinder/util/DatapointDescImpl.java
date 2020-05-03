package org.ogema.devicefinder.util;

import org.ogema.devicefinder.api.ConsumptionInfo;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.DatapointDesc;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataTypeI;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** To generate datapoint description objects this implementation can be inherited or
 * own implementations can be made
 */
public class DatapointDescImpl implements DatapointDesc {
	protected GaRoDataTypeI garoDataType = null;
	protected DPRoom dpRoom = null;
	protected ConsumptionInfo consumptionInfo = null;
	protected String subRoomLocation = null;
	protected Boolean isLocal = null;

	public DatapointDescImpl(GaRoDataTypeI garoDataType, DPRoom dpRoom, ConsumptionInfo consumptionInfo,
			String subRoomLocation, Boolean isLocal) {
		this.garoDataType = garoDataType;
		this.dpRoom = dpRoom;
		this.consumptionInfo = consumptionInfo;
		this.subRoomLocation = subRoomLocation;
		this.isLocal = isLocal;
	}
	
	@Override
	public GaRoDataTypeI getGaroDataType() {
		return garoDataType;
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
	public ConsumptionInfo getConsumptionInfo() {
		return consumptionInfo;
	}

	@Override
	public Boolean isLocal() {
		return isLocal;
	}

}
