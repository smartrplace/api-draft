package org.smartrplace.apps.hw.install.prop;

import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.util.DatapointInfoProviderImpl;
import org.ogema.tools.resource.util.ValueResourceUtils;

import de.iwes.util.format.StringFormatHelper;

public class ViaHeartbeatInfoProvider extends DatapointInfoProviderImpl {

	private Float lastValue = null;
	private long lastValueWritten = -1;
	//private Float lastValueWrittenForSend = null;
	//private Float lastValueReceived = null;
	private SingleValueResource sres;
	private long lastValueUpdateSent = -Long.MIN_VALUE;
	private final DatapointService dpService;
	
	private long lastValueReceiveTime;
	
	//private SingleValueResource mirrorResource = null;
	private final Datapoint dp;
	
	public ViaHeartbeatInfoProvider(Datapoint dp, DatapointService dpService) {
		if(dp.getResource() != null && (dp.getResource() instanceof SingleValueResource))
			this.sres = (SingleValueResource) dp.getResource();
		else
			this.sres = null;
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
		}
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
		}
		else
			return lastValue;
		//return lastValueReceived;
	}

	public boolean isValueNew(long now) {
		long lastWriteTime;
		if(sres != null) {
			lastWriteTime = sres.getLastUpdateTime();
		}
		else {
			lastWriteTime = lastValueWritten;
		}
		boolean result = (lastWriteTime > lastValueUpdateSent &&
				((lastWriteTime - lastValueReceiveTime) > 10));
System.out.println("   isValNew: For "+dp.getLocation()+" result: "+result+" lastWrite:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastWriteTime));
System.out.println("        lastSent:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastValueUpdateSent)+
	" lastRecv:"+StringFormatHelper.getFullTimeDateInLocalTimeZone(lastValueReceiveTime)+ " sres:"+(sres!=null?ValueResourceUtils.getValue(sres):"null"));		
		return result;
	}
	
	public Float getValueToSend(long now) {
		if(!isValueNew(now))
			return null;
		lastValueUpdateSent = now;
		return getCurrentValue();
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
}
