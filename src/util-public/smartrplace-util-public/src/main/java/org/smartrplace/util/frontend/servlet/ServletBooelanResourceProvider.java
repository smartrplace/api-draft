package org.smartrplace.util.frontend.servlet;

import org.ogema.core.model.simple.BooleanResource;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

public class ServletBooelanResourceProvider implements ServletValueProvider {
	protected BooleanResource res;
	
	public ServletBooelanResourceProvider(BooleanResource res) {
		this.res = res;
	}
	
	@Override
	public String getValue(String user, String key) {
		return String.valueOf(res.getValue());
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
