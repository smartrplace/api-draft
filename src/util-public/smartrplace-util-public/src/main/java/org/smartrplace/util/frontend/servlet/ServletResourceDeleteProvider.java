package org.smartrplace.util.frontend.servlet;

import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.Resource;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

public class ServletResourceDeleteProvider implements ServletValueProvider {
	protected Resource res;
	
	public ServletResourceDeleteProvider(Resource res) {
		this.res = res;
	}
	
	@Override
	public Value getValue(String user, String key) {
		return null;
	}

	@Override
	public void setValue(String user, String key, String value) {
		try  {
			boolean val = Boolean.parseBoolean(value);
			if(val)
				res.delete();
		} catch(NumberFormatException e) {
			//do nothing
		}
	}
}
