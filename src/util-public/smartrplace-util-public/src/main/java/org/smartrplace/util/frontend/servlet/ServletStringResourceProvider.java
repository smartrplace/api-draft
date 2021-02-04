package org.smartrplace.util.frontend.servlet;

import java.util.Map;

import org.ogema.core.channelmanager.measurements.StringValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.simple.StringResource;

import de.iwes.util.resource.ValueResourceHelper;

public class ServletStringResourceProvider extends ServletNumProviderBase {
	protected StringResource res;
	
	public ServletStringResourceProvider(StringResource res) {
		this.res = res;
	}
	public ServletStringResourceProvider(StringResource res, Map<String, String[]> paramMap,
			Boolean hasWritePermission) {
		this(res, new UserServletParamData(paramMap, hasWritePermission));
	}
	public ServletStringResourceProvider(StringResource res, UserServletParamData pdata) {
		this(res);
		this.pdata = pdata;
	}
	
	@Override
	public Value getValueInternal(String user, String key) {
		return new StringValue(res.getValue());
	}

	@Override
	public void setValue(String user, String key, String value) {
		ValueResourceHelper.setCreate(res, value);
	}
	@Override
	protected boolean isWritable() {
		return true;
	}
}
