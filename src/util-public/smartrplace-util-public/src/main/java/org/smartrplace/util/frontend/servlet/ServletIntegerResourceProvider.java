package org.smartrplace.util.frontend.servlet;

import org.ogema.core.model.simple.IntegerResource;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

public class ServletIntegerResourceProvider implements ServletValueProvider {
	protected IntegerResource res;
	
	public ServletIntegerResourceProvider(IntegerResource res) {
		this.res = res;
	}
	
	@Override
	public String getValue(String user, String key) {
		return String.valueOf(res.getValue());
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
