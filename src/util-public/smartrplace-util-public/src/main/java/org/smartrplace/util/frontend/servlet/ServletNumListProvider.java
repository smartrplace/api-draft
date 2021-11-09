package org.smartrplace.util.frontend.servlet;

import java.util.Collection;

import org.json.JSONArray;
import org.ogema.core.channelmanager.measurements.Value;
import org.smartrplace.util.frontend.servlet.UserServlet.JSONVarrRes;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

public class ServletNumListProvider<T> implements ServletValueProvider {
	protected final Collection<T> valListFinal;
	protected final String nullValue;

	public ServletNumListProvider(Collection<T> valList) {
		this(valList, null);
	}
	public ServletNumListProvider(Collection<T> valList, String nullValue) {
		this.valListFinal = valList;
		this.nullValue = nullValue;
	}
	
	@Override
	public ValueMode getValueMode() {
		return ValueMode.JSON;
	}
	
	@Override
	public JSONVarrRes getJSON(String user, String key) {
		//JSONObject result = new JSONObject();
		JSONArray arr = new JSONArray();
		for(T obj: valListFinal) {
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
		//result.put("value", arr);
		
		//TODO: Also support to return array
		JSONVarrRes realResult = new JSONVarrRes();
		//realResult.result = result;
		realResult.resultArr = arr;
		return realResult;
	}
	
	@Override
	public Value getValue(String user, String key) {
		throw new UnsupportedOperationException("Use getJSON!");
	}
}
