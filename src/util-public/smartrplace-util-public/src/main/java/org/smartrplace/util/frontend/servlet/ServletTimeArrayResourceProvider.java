package org.smartrplace.util.frontend.servlet;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.array.TimeArrayResource;
import org.smartrplace.util.frontend.servlet.UserServlet.JSONVarrRes;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ValueResourceHelper;

public class ServletTimeArrayResourceProvider implements ServletValueProvider {
	protected TimeArrayResource res;
	
	public ServletTimeArrayResourceProvider(TimeArrayResource res) {
		this.res = res;
	}
	public ServletTimeArrayResourceProvider(TimeArrayResource res, Map<String, String[]> paramMap) {
		this(res, new UserServletParamData(paramMap, null));
	}
	public ServletTimeArrayResourceProvider(TimeArrayResource res, Map<String, String[]> paramMap,
			Boolean hasWritePermission) {
		this(res, new UserServletParamData(paramMap, hasWritePermission));
	}
	public ServletTimeArrayResourceProvider(TimeArrayResource res, UserServletParamData pdata) {
		this(res);
	}

	@Override
	public ValueMode getValueMode() {
		return ValueMode.JSON;
	}

	@Override
	public JSONVarrRes getJSON(String user, String key) {
		JSONVarrRes result = new JSONVarrRes();
		result.result = new JSONObject();
		List<Long> values = new ArrayList<>();
		for(long val: res.getValues()) {
			values.add(val);
		}
		result.result.put("values", values);
		return result;
	}
	
	@Override
	public void setValue(String user, String key, String value) {
		long[] values = null;
		try  {
			JSONObject json = new JSONObject(value);
			if(json.has("values")) {
				JSONArray valobj = json.getJSONArray("values");
				values = new long[valobj.length()];
				for(int idx=0; idx<valobj.length(); idx++) {
					Long val = valobj.getLong(idx);
					values[idx] = val;
				}
			}
		} catch(JSONException | NumberFormatException e) {
			return;
		}
		if(values == null) try {
			List<String> strs = StringFormatHelper.getListFromString(value);
			values = new long[strs.size()];
			int idx = 0;
			for(String str: strs) {
				long val = Long.parseLong(str);
				values[idx] = val;
				idx++;
			}
		} catch(NumberFormatException e) {
			return;
		}
		ValueResourceHelper.setCreate(res, values);
		res.setValues(values);
	}

	@Override
	public Value getValue(String user, String key) {
		throw new IllegalStateException("Use JSON!");
	}
}
