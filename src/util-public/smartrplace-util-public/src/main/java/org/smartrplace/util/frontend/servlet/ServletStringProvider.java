package org.smartrplace.util.frontend.servlet;

import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

public class ServletStringProvider implements ServletValueProvider {
	protected String text;
	
	public ServletStringProvider(String text) {
		this.text = text;
	}
	
	@Override
	public String getValue(String user, String key) {
		return text;
	}
}
