package org.smartrplace.util.frontend.servlet;

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
	public String getValue(String user, String key) {
		return text;
	}
}
