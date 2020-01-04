package org.smartrplace.util.frontend.servlet;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

public abstract class ServletResourceListProvider<T extends Resource> implements ServletValueProvider {
	protected final String id;
	protected ResourceList<T> res;
	protected abstract JSONObject getJSON(T element);
	protected abstract void setElementData(T element, JSONObject json);	
	public ServletResourceListProvider(ResourceList<T> res, String id) {
		this.res = res;
		this.id = id;
	}
	
	@Override
	public ValueMode getValueMode() {
		return ValueMode.JSON;
	}
	
	@Override
	public JSONObject getJSON(String user, String key) {
		JSONArray result = new JSONArray();
		for(T el: res.getAllElements()) {
			JSONObject eljson = getJSON(el);
			if(eljson != null)
				result.put(eljson);
		}
		JSONObject res = new JSONObject();
		res.put(id, result);
		return res;
	}
	
	@Override
	public String getValue(String user, String key) {
		throw new IllegalStateException("Returns JSON");
	}

	@Override
	public void setValue(String user, String key, String value) {
		try  {
			JSONObject jsonobj = new JSONObject(value);
			JSONArray json = jsonobj.getJSONArray(id);
			int len = json.length();
			if(len < res.size()) {
				List<T> allRes = res.getAllElements();
				for(int i=len; i<allRes.size(); i++) {
					allRes.get(i).delete();
				}
			} else if(len > res.size()) {
				for(int i=res.size(); i<len; i++) {
					res.add();
				}
			}
			for(int i=0; i<len; i++) {
				List<T> allRes = res.getAllElements();
				setElementData(allRes.get(i), (JSONObject) json.get(i));
			}
		} catch(NumberFormatException e) {
			//do nothing
		}
	}
}
