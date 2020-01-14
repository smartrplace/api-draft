package org.smartrplace.util.frontend.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider.ValueMode;

/** Core class to provide servlets based on widgets pages
 * TODO: Entire package to be moved to smartrplace-util-proposed or similar location for Utils*/
public class UserServlet extends HttpServlet {
	private static final long serialVersionUID = -462293886580458217L;

	public interface ServletPageProvider<T extends Object> {
		Map<String, ServletValueProvider> getProviders(T object, String user);
		
		/** Usually this should be implemented.
		 *  
		 * @param user
		 * @return if null then {@link #getObject(String)} must be implemented. In this case an
		 * 		object has to be specified with each request. This would be suitable for accessing larget databases.
		 */
		Collection<T> getAllObjects(String user);
		
		/** Page may provide object based on ID. In this case data for a single object can be read via the servlet*/
		default T getObject(String objectId) {return null;}
	}
	public interface ServletValueProvider {
		/** Called on GET*/
		String getValue(String user, String key);
		default JSONObject getJSON(String user, String key) {throw new UnsupportedOperationException("Not implemented for VALUEMODE STRING");}
		/** Called on POST*/
		default void setValue(String user, String key, String value) {};
		default long getPollInterval() {return 30000l;}
		public enum ValueMode {
			STRING,
			JSON
		}
		/** Define how the values shall be obtained
		 * 
		 * @return if JSON is returned then {@link #getJSON(String, String)} is used instead of {@link #getValue(String, String)}
		 */
		default ValueMode getValueMode() {return ValueMode.STRING;} 
	}
	/*public enum UseMode {
		GET,
		POST,
		BOTH
	}*/
	
	//final protected Map<String, Map<String, ServletValueProvider>> data = new HashMap<>();
	//final protected Map<String, ServletValueProvider> dataAllUsers = new HashMap<>();
	////final protected Map<String, Map<String, ServletValueProvider>> postData = new HashMap<>();
	////final protected Map<String, ServletValueProvider> postDataAllUsers = new HashMap<>();
	final protected Map<String, ServletPageProvider<?>> pages = new HashMap<>();
	protected String stdPageId = null;
	
	//private final SmartrplaceHeatcontrolController controller;
	private static UserServlet instance = null;
	public static UserServlet getInstance() {
		if(instance == null) instance = new UserServlet();
		return instance;
	}
	
	private UserServlet() {
	}

	public void addPage(String pageId, ServletPageProvider<?> prov) {
		if(stdPageId == null)
			stdPageId = pageId;
		pages.put(pageId, prov);
	}
	
	/*public void addData(String user, String key, ServletValueProvider value) {
		Map<String, ServletValueProvider> subMap = data.get(user);
		if(subMap == null) {
			subMap = new HashMap<>(dataAllUsers);
			data.put(user, subMap);
		}
		subMap.put(key, value);
	}
	
	public void addDataAllUsers(String key, ServletValueProvider value) {
		dataAllUsers.put(key, value);
		for(Map<String, ServletValueProvider> userData: data.values()) {
			userData.put(key, value);
		}
	}
	
	public void addData(String user,  Map<String, ServletValueProvider> newData) {
		for(Entry<String, ServletValueProvider> e: newData.entrySet()) {
			addData(user, e.getKey(), e.getValue());
		}
	}
	
	public void addDataAllUsers(Map<String, ServletValueProvider> newData) {
		for(Entry<String, ServletValueProvider> e: newData.entrySet()) {
			addDataAllUsers(e.getKey(), e.getValue());
		}
	}
	
	public void addData(String user, String key, ServletValueProvider value, UseMode mode) {
		if(mode == UseMode.GET || mode == UseMode.BOTH)
			addData(user, key, value);
		if(mode == UseMode.POST || mode == UseMode.BOTH) {
			Map<String, ServletValueProvider> subMap = postData.get(user);
			if(subMap == null) {
				subMap = new HashMap<>(dataAllUsers);
				postData.put(user, subMap);
			}
			subMap.put(key, value);
		}
	}
	
	public void addDataAllUsers(String key, ServletValueProvider value, UseMode mode) {
		if(mode == UseMode.GET || mode == UseMode.BOTH)
			addDataAllUsers(key, value);
		if(mode == UseMode.POST || mode == UseMode.BOTH) {
			postDataAllUsers.put(key, value);
			for(Map<String, ServletValueProvider> userData: postData.values()) {
				userData.put(key, value);
			}
		}
	}
	
	public void addData(String user,  Map<String, ServletValueProvider> newData, UseMode mode) {
		for(Entry<String, ServletValueProvider> e: newData.entrySet()) {
			addData(user, e.getKey(), e.getValue(), mode);
		}
	}
	
	public void addDataAllUsers(Map<String, ServletValueProvider> newData, UseMode mode) {
		for(Entry<String, ServletValueProvider> e: newData.entrySet()) {
			addDataAllUsers(e.getKey(), e.getValue(), mode);
		}
	}*/
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String user = req.getParameter("user");
		String pageId = req.getParameter("page");
		String object = req.getParameter("object");
		if(user == null) return;
		if(pageId == null) pageId = stdPageId ;
		ServletPageProvider<?> pageMap = pages.get(pageId);
		if(pageMap == null) return;
		String pollStr = req.getParameter("poll");
		
		JSONObject result = getJSON(object, user, pollStr, pageMap);
		
		resp.getWriter().write(result.toString());
		resp.setStatus(200);
	}
	
	protected <T> JSONObject getJSON(String objectId, String user, String pollStr, 
			ServletPageProvider<T> pageprov) {
		JSONObject result = new JSONObject();

		Collection<T> objects = getObjects(objectId, user, pageprov);
		if(objects == null) return result;
		
		boolean doPoll;
		long pollInterval = -1;
		if(pollStr == null) doPoll = false;
		else {
			try {
				pollInterval = Long.parseLong(pollStr)*1000;
				doPoll = true;
			} catch(NumberFormatException e) {
				doPoll = false;
			}
		}
		
		for(T obj: objects) {
			if(obj == null) continue;
			Map<String, ServletValueProvider> data = pageprov.getProviders(obj, user);
			final String objStr;
			final JSONObject subJson = new JSONObject();
			if(obj instanceof Resource)
				objStr = ResourceUtils.getHumanReadableShortName((Resource)obj);
			else
				objStr = obj.toString();
			for(Entry<String, ServletValueProvider> prov: data.entrySet()) {
				if(doPoll) {
					if(prov.getValue().getPollInterval() > pollInterval)
						continue;
				}
				if(prov.getValue().getValueMode() == ValueMode.STRING) {
					String value = prov.getValue().getValue(user, prov.getKey());
					if(!Boolean.getBoolean("org.smartrplace.util.frontend.servlet.initialstructure")) {
						subJson.put(prov.getKey(), value);
					} else
						result.put(objStr+"/"+prov.getKey(), value);
				} else {
					JSONObject value = prov.getValue().getJSON(user, prov.getKey());
					if(!Boolean.getBoolean("org.smartrplace.util.frontend.servlet.initialstructure")) {
						subJson.put(prov.getKey(), value);
					} else
						result.put(objStr+"/"+prov.getKey(), value);					
				}
			}
			if(!Boolean.getBoolean("org.smartrplace.util.frontend.servlet.initialstructure")) {
				result.put(objStr, subJson);
			}
		}
		
		return result;
	}
	
	protected <T> Collection<T> getObjects(String objectId, String user, ServletPageProvider<T> prov) {
		if(objectId != null) {
			T obj = prov.getObject(objectId);
			List<T> result = new ArrayList<T>();
			result.add(obj);
			return result;
		}
		return prov.getAllObjects(user);
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		String user = req.getParameter("user");
		if(user == null) return;
		String pageId = req.getParameter("page");
		if(pageId == null) pageId = stdPageId ;
		String object = req.getParameter("object");
		ServletPageProvider<?> pageMap = pages.get(pageId);
		if(pageMap == null) return;
		
		StringBuilder sb = new StringBuilder();
		BufferedReader reader = req.getReader();
		int status;
		String response = "";
		
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append('\n');
			}
		} finally {
			reader.close();
		}
		String request = sb.toString();

		try {
			JSONObject result = new JSONObject(request);
			postJSON(object, user, result, pageMap, response);
			//Map<String, ServletValueProvider> userMap = postData.get(user);
			//for(String key: result.keySet()) {
			//	ServletValueProvider prov = userMap.get(key);
			//	String value = result.getString(key);
			//	prov.setValue(user, key, value);
			//}
			response = response + " Success!";
			status = HttpServletResponse.SC_OK;
		} catch (Exception e) {
			response = response + "An error occurred: " + e.toString();
			if(Boolean.getBoolean("org.smartrplace.util.frontend.servlet.servererrorstoconsole"))
				e.printStackTrace();
			status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
		}
		
		//resp.getWriter().write("success");
		resp.getWriter().write(response);
		resp.setStatus(status);
	}
	
	protected <T> String postJSON(String objectId, String user, JSONObject result, ServletPageProvider<T> pageprov, String response) {
		Collection<T> objects = getObjects(objectId, user, pageprov);
		if(objects == null) return response;
		
		for(T obj: objects) {
			Map<String, ServletValueProvider> userMap = pageprov.getProviders(obj, user);
			for(String key: result.keySet()) {
				ServletValueProvider prov = userMap.get(key);
				if(prov == null)
					throw new IllegalStateException(key+" not available for "+pageprov.toString());
				String value;
				try {
					value = result.getString(key).toString();
				} catch(JSONException e) {
					value = result.getJSONObject(key).toString();
				}
				try {
					prov.setValue(user, key, value);
				} catch(Exception e) {
					if(objectId != null)
						throw new IllegalStateException(key+" cannot be processed for "+pageprov.toString()+", object:"+objectId, e);
					else
						throw new IllegalStateException(key+" cannot be processed for "+pageprov.toString()+", object not provided", e);
				}
			}
		}
		
		return response;
	}

}
