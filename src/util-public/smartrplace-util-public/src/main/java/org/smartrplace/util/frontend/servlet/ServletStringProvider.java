package org.smartrplace.util.frontend.servlet;

import org.ogema.core.channelmanager.measurements.StringValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

public class ServletStringProvider implements ServletValueProvider {
	protected String text;
	
	public ServletStringProvider(String text) {
		if(text == null)
			this.text = "(null)";
		else
			this.text = text;
	}
	
	public ServletStringProvider(Object[] array) {
		if(array == null)
			this.text = "(null)";
		else {
			text = null;
			for(Object el: array) {
				if(text == null)
					text = el.toString();
				else
					text += (",")+el.toString();
			}
		}
	}
	
	@Override
	public Value getValue(String user, String key) {
		return new StringValue(text);
	}
}
