package org.smartrplace.util.frontend.servlet;

import java.util.Map;

import org.ogema.core.channelmanager.measurements.IntegerValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.tools.resource.util.LoggingUtils;

public class ServletIntegerResourceProvider extends ServletNumProviderBase {
	protected IntegerResource res;
	
	public ServletIntegerResourceProvider(IntegerResource res) {
		this.res = res;
	}
	public ServletIntegerResourceProvider(IntegerResource res, Map<String, String[]> paramMap) {
		this(res, new UserServletParamData(paramMap, null));
	}
	public ServletIntegerResourceProvider(IntegerResource res, Map<String, String[]> paramMap,
			Boolean hasWritePermission) {
		this(res, new UserServletParamData(paramMap, hasWritePermission));
	}
	public ServletIntegerResourceProvider(IntegerResource res, UserServletParamData pdata) {
		this(res);
		this.pdata = pdata;
		if(pdata != null && pdata.provideExtended && LoggingUtils.isLoggingEnabled(res)) {
			pdata.tsDataRaw = res.getHistoricalData();
			pdata.tsLocationOrBaseId = res.getLocation();
		}		
	}

	
	@Override
	public Value getValueInternal(String user, String key) {
		return new IntegerValue(res.getValue());
	}

	@Override
	public void setValue(String user, String key, String value) {
		try  {
			int val = Integer.parseInt(value);
			res.setValue(val);
		} catch(NumberFormatException e) {
			//do nothing
		}
	}

	@Override
	protected boolean isWritable() {
		return true;
	}
}
