package org.smartrplace.util.frontend.servlet;

import org.ogema.core.channelmanager.measurements.StringValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.simple.StringResource;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

public class ServletStringResourceProvider implements ServletValueProvider {
	protected StringResource res;
	
	public ServletStringResourceProvider(StringResource res) {
		this.res = res;
	}
	
	@Override
	public Value getValue(String user, String key) {
		return new StringValue(res.getValue());
	}

	@Override
	public void setValue(String user, String key, String value) {
		res.setValue(value);
	}
}
