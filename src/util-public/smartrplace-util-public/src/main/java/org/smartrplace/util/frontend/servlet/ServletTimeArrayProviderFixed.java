package org.smartrplace.util.frontend.servlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONObject;
import org.smartrplace.util.frontend.servlet.UserServlet.JSONVarrRes;

public class ServletTimeArrayProviderFixed extends ServletTimeArrayResourceProvider {
	private final Collection<Long> valList;
	
	public ServletTimeArrayProviderFixed(Collection<Long> valList) {
		super(null);
		this.valList = valList;
	}

	@Override
	public JSONVarrRes getJSON(String user, String key) {
		JSONVarrRes result = new JSONVarrRes();
		result.result = new JSONObject();
		List<Long> values = new ArrayList<>();
		for(Long val: valList) {
			values.add(val);
		}
		result.result.put("values", values);
		return result;
	}	
	
	@Override
	public void setValue(String user, String key, String value) {
		throw new IllegalStateException("Write is not supported!");
	}
}
