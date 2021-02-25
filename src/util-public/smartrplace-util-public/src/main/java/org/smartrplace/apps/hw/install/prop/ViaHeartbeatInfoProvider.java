package org.smartrplace.apps.hw.install.prop;

import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DatapointInfoProviderImpl;
import org.ogema.tools.resource.util.ValueResourceUtils;

public class ViaHeartbeatInfoProvider extends DatapointInfoProviderImpl {

	/** Supported data types:<br>
	 * - memory values of type Float (lastValue)<br>
	 * - {@link SingleValueResource}s <br>
	 * - {@link Schedule}s <br>
	 * 
	 */
	private Float lastValue = null;
	private long lastValueWritten = -1;
	private SingleValueResource sres;
	public static interface StringProvider {
		void received(String strValue, long now);
		String getStringToSend(long now);
		boolean hasNewValue(long now);
		String getAlias();
		
		//just for debugging
		long getLastValueWritten();
		default long getLastRecvTime() {
			return -2;
		}
		long getLastUpdateSent();
		long getLastClean();
		void resendAllOnNextOccasion();
	}
	private StringProvider strProv;
	//private Schedule sched;
	
	private long lastValueUpdateSent = -Long.MIN_VALUE;
	private final DatapointService dpService;
	
	private long lastValueReceiveTime;
	
	//private SingleValueResource mirrorResource = null;
	private final Datapoint dp;
	
	public ViaHeartbeatInfoProvider(Datapoint dp, DatapointService dpService) {
		if(dp.getParameter(Datapoint.HEARTBEAT_STRING_PROVIDER_PARAM) != null) {
			this.setStrProv((StringProvider) dp.getParameter(Datapoint.HEARTBEAT_STRING_PROVIDER_PARAM));
			this.sres = null;
		} else if(dp.getResource() != null && (dp.getResource() instanceof SingleValueResource)) {
			this.sres = (SingleValueResource) dp.getResource();
			this.setStrProv(null);
		} else {
			this.sres = null;
			this.setStrProv(null);
		}
		this.dpService = dpService;
		this.dp = dp;
	}

	@Override
	public boolean setCurrentValue(Float value) {
		return setCurrentValue(value, dpService.getFrameworkTime());
	}
	public boolean setCurrentValue(Float value, long now) {
		if(sres != null) {
			if(value != null)
				ValueResourceUtils.setValue(sres, value);
		} else if(strProv != null)
			throw new IllegalStateException("setCurrentValue cannot be called if a StringProvider is defined!");
		else {
			lastValue = value;
			lastValueWritten =  now;
		}
		//lastValueWrittenForSend = value;
		return true;
	}
	
	@Override
	public Float getCurrentValue() {
		if(sres != null) {
			return ValueResourceUtils.getFloatValue(sres);
		} else if(strProv != null)
			throw new IllegalStateException("getCurrentValue cannot be called if a StringProvider is defined!");
		else
			return lastValue;
		//return lastValueReceived;
	}

	public boolean isValueNew(long now) {
		long lastWriteTime;
		if(sres != null) {
			lastWriteTime = sres.getLastUpdateTime();
		} else if(getStrProv() != null) {
			return getStrProv().hasNewValue(now);
		} else {
			lastWriteTime = lastValueWritten;
		}
		boolean result = (lastWriteTime > lastValueUpdateSent &&
				((lastWriteTime - lastValueReceiveTime) > 10));
		return result;
	}
	
	public Float getValueToSend(long now, boolean forceSendAllValues) {
		if((!forceSendAllValues) && (!isValueNew(now)))
			return null;
		lastValueUpdateSent = now;
		return getCurrentValue();
	}
	public String getValueToSendString(long now, boolean forceSendAllValues) {
		if((!forceSendAllValues) && (!isValueNew(now)))
			return null;
		lastValueUpdateSent = now;
		if(strProv == null)
			throw new IllegalStateException("getCurrentValueString can only be called if a StringProvider is defined!");
		return strProv.getStringToSend(now);
	}
	
	public boolean setValueReceived(Float value, long now) {
		lastValueReceiveTime = now;
		return setCurrentValue(value, now);
	}

	public boolean checkMirrorResorce() {
		if(sres != null)
			return false;
		SingleValueResource mirrorRes = (SingleValueResource) dp.getParameter(Datapoint.MIRROR_RESOURCE_PARAM);
		if(mirrorRes == null)
			return false;
		
		if(lastValue != null)
			ValueResourceUtils.setValue(sres, lastValue);
		sres = mirrorRes;
		return true;
	}

	public StringProvider getStrProv() {
		return strProv;
	}

	public void setStrProv(StringProvider strProv) {
		this.strProv = strProv;
	}

	//just for debugging
	public long getLastValueWritten() {
		if(sres != null)
			return sres.getLastUpdateTime();
		return lastValueWritten;
	}
	public long getLastRecvTime() {
		return lastValueReceiveTime;
	}
	public long getLastUpdateSent() {
		return lastValueUpdateSent;
	}
}
