package org.smartrplace.util.frontend.servlet;

import org.ogema.core.model.simple.FloatResource;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

public class ServletFloatResourceProvider implements ServletValueProvider {
	protected FloatResource res;
	
	public ServletFloatResourceProvider(FloatResource res) {
		this.res = res;
	}
	
	@Override
	public String getValue(String user, String key) {
		return String.valueOf(res.getValue());
	}

	@Override
	public void setValue(String user, String key, String value) {
		try  {
			float val = Float.parseFloat(value);
			res.setValue(val);
		} catch(NumberFormatException e) {
			//do nothing
		}
	}
}
