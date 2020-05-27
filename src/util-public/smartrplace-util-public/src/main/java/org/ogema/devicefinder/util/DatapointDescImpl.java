package org.ogema.devicefinder.util;

import org.ogema.devicefinder.api.DatapointInfo;
import org.ogema.devicefinder.api.DPRoom;
import org.ogema.devicefinder.api.DatapointDesc;

import de.iwes.timeseries.eval.garo.api.base.GaRoDataType;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** To generate datapoint description objects this implementation can be inherited or
 * own implementations can be made
 */
public class DatapointDescImpl implements DatapointDesc {
	protected GaRoDataType garoDataType = null;
	protected String label;
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
	public String label() {
		return label;
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

}
