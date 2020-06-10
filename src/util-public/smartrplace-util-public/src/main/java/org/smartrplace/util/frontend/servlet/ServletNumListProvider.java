package org.smartrplace.util.frontend.servlet;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.Value;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

public class ServletNumListProvider<T> implements ServletValueProvider {
	protected final List<T> floatVal;
	protected final String nullValue;

	public ServletNumListProvider(List<T> valList) {
		this(valList, null);
	}
	public ServletNumListProvider(List<T> valList, String nullValue) {
		floatVal = valList;
		this.nullValue = nullValue;
	}
	
	@Override
	public ValueMode getValueMode() {
		return ValueMode.JSON;
	}
	
	@Override
	public JSONObject getJSON(String user, String key) {
		JSONObject result = new JSONObject();
		JSONArray arr = new JSONArray();
		for(T obj: floatVal) {
			if(obj == null) {
				if(nullValue != null)
					arr.put(nullValue);
			} else if(obj instanceof Float) {
				float inval = (Float)obj;
				Float val = UserServletUtil.getJSONValue(inval);
				if(val == null) {
					if(nullValue != null)
						arr.put(nullValue);
				} else
					arr.put(val);
			} else if(obj instanceof Integer)
				arr.put((Integer)obj);
			else if(obj instanceof Boolean)
				arr.put((Boolean)obj);
			else
				arr.put(obj.toString());
		}
		result.put("value", arr);
		return result;
	}
	
	@Override
	public Value getValue(String user, String key) {
		throw new UnsupportedOperationException("Use getJSON!");
	}
}
