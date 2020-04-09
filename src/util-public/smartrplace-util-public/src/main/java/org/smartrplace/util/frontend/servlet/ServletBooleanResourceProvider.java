package org.smartrplace.util.frontend.servlet;

import org.ogema.core.channelmanager.measurements.BooleanValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.simple.BooleanResource;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

public class ServletBooleanResourceProvider implements ServletValueProvider {
	protected BooleanResource res;
	
	public ServletBooleanResourceProvider(BooleanResource res) {
		this.res = res;
	}
	
	@Override
	public Value getValue(String user, String key) {
		return new BooleanValue(res.getValue());
	}

	@Override
	public void setValue(String user, String key, String value) {
		try  {
			boolean val = Boolean.parseBoolean(value);
			res.setValue(val);
		} catch(NumberFormatException e) {
			//do nothing
		}
	}
}
