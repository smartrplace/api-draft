package org.smartrplace.util.frontend.servlet;

import java.util.Map;

import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.tools.resource.util.LoggingUtils;

import de.iwes.util.resource.ValueResourceHelper;

public class ServletFloatResourceProvider extends ServletNumProviderBase {
	protected FloatResource res;
	
	public ServletFloatResourceProvider(FloatResource res) {
		this.res = res;
	}
	public ServletFloatResourceProvider(FloatResource res, Map<String, String[]> paramMap) {
		this(res, new UserServletParamData(paramMap, null));
	}
	public ServletFloatResourceProvider(FloatResource res, Map<String, String[]> paramMap,
			Boolean hasWritePermission) {
		this(res, new UserServletParamData(paramMap, hasWritePermission));
	}
	public ServletFloatResourceProvider(FloatResource res, UserServletParamData pdata) {
		this(res);
		this.pdata = pdata;
		if(pdata != null && pdata.provideExtended && LoggingUtils.isLoggingEnabled(res)) {
			pdata.tsDataRaw = res.getHistoricalData();
			pdata.tsLocationOrBaseId = res.getLocation();
		}		
	}
	
	@Override
	public Value getValueInternal(String user, String key) {
		return new FloatValue(res.getValue());
	}

	@Override
	public void setValue(String user, String key, String value) {
		try  {
			float val = Float.parseFloat(value);
			ValueResourceHelper.setCreate(res, val);
		} catch(NumberFormatException e) {
			//do nothing
		}
	}

	@Override
	protected boolean isWritable() {
		return true;
	}
}
