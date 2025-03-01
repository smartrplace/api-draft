package org.smartrplace.util.frontend.servlet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.json.JSONObject;
import org.smartrplace.util.frontend.servlet.UserServlet.JSONVarrRes;

public class ServletStringArrayProviderFixed extends ServletTimeArrayResourceProvider {
	private final Collection<String> valList;
	
	public ServletStringArrayProviderFixed(Collection<String> valList) {
		super(null);
		this.valList = valList;
	}

	@Override
	public JSONVarrRes getJSON(String user, String key) {
		JSONVarrRes result = new JSONVarrRes();
		result.result = new JSONObject();
		List<String> values = new ArrayList<>();
		for(String val: valList) {
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
