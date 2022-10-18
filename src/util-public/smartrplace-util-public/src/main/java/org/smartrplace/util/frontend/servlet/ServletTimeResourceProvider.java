package org.smartrplace.util.frontend.servlet;

import java.util.Map;

import org.ogema.core.channelmanager.measurements.LongValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.tools.resource.util.LoggingUtils;

import de.iwes.util.resource.ValueResourceHelper;

public class ServletTimeResourceProvider extends ServletNumProviderBase {
	protected TimeResource res;
	
	public ServletTimeResourceProvider(TimeResource res) {
		this.res = res;
	}
	public ServletTimeResourceProvider(TimeResource res, Map<String, String[]> paramMap) {
		this(res, new UserServletParamData(paramMap, null));
	}
	public ServletTimeResourceProvider(TimeResource res, Map<String, String[]> paramMap,
			Boolean hasWritePermission) {
		this(res, new UserServletParamData(paramMap, hasWritePermission));
	}
	public ServletTimeResourceProvider(TimeResource res, UserServletParamData pdata) {
		this(res);
		this.pdata = pdata;
		if(pdata != null && pdata.provideExtended && LoggingUtils.isLoggingEnabled(res)) {
			pdata.tsDataRaw = res.getHistoricalData();
			pdata.tsLocationOrBaseId = res.getLocation();
		}		
	}

	
	@Override
	public Value getValueInternal(String user, String key) {
		return new LongValue(res.getValue());
	}

	@Override
	public void setValue(String user, String key, String value) {
		try  {
			long val = Long.parseLong(value);
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
