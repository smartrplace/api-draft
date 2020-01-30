package org.smartrplace.util.frontend.servlet;

import org.ogema.core.channelmanager.measurements.IntegerValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

public class ServletIntegerResourceProvider implements ServletValueProvider {
	protected IntegerResource res;
	
	public ServletIntegerResourceProvider(IntegerResource res) {
		this.res = res;
	}
	
	@Override
	public Value getValue(String user, String key) {
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
}
