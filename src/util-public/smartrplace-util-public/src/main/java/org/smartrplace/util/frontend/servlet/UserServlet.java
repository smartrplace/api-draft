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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.BooleanValue;
import org.ogema.core.channelmanager.measurements.DoubleValue;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.IntegerValue;
import org.ogema.core.channelmanager.measurements.LongValue;
import org.ogema.core.channelmanager.measurements.StringValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider.ValueMode;

/** Core class to provide servlets based on widgets pages
 * TODO: Entire package to be moved to smartrplace-util-proposed or similar location for Utils*/
public class UserServlet extends HttpServlet {
	private static final long serialVersionUID = -462293886580458217L;
	public static final String TIMEPREFIX = "&time=";

	final Logger logger = LoggerFactory.getLogger(UserServlet.class);
	public enum ReturnStructure {
		/**In this case a list is returned with each element having an element named "key". Additional
		standard elements may be defined in the future when elements are generated from
		widgets*/
		LIST,
		DICTIONARY
	}
	public interface ServletPageProvider<T extends Object> {
		Map<String, ServletValueProvider> getProviders(T object, String user, Map<String, String[]> parameters);
		
		/** Usually this should be implemented.
		 *  
		 * @param user
		 * @return if null then {@link #getObject(String)} must be implemented. In this case an
		 * 		object has to be specified with each request. This would be suitable for accessing larget databases.
		 */
		Collection<T> getAllObjects(String user);
		
		/** Page may provide object based on ID. In this case data for a single object can be read via the servlet*/
		default T getObject(String objectId) {return null;}
		
		default ReturnStructure getGETStructure() {return ReturnStructure.DICTIONARY;}
		
		default String getObjectId(T obj) {
			if(obj instanceof Resource)
				return ResourceUtils.getHumanReadableShortName((Resource)obj);
			else
				return obj.toString();			
		}
	}
	public interface ServletValueProvider {
		/** Called on GET*/
		Value getValue(String user, String key);
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
	/*private static UserServlet instance = null;
	public static UserServlet getInstance() {
		if(instance == null) instance = new UserServlet();
		return instance;
	}*/
	
	public UserServlet() {
	}

	public void addPage(String pageId, ServletPageProvider<?> prov) {
		if(stdPageId == null)
			stdPageId = pageId;
		pages.put(pageId, prov);
	}
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		String user = req.getParameter("user");
		String pageId = req.getParameter("page");
		String object = req.getParameter("object");
		String timeStr = req.getParameter("time");
		String returnStruct = req.getParameter("structure");
		//if(user == null) return;
		if(pageId == null) pageId = stdPageId ;
		ServletPageProvider<?> pageMap = pages.get(pageId);
		//if(pageMap == null) return;
		String pollStr = req.getParameter("poll");
		Map<String, String[]> paramMap = getParamMap(req);
		
		JSONObject result = getJSON(object, user, pollStr, timeStr, pageMap, returnStruct, paramMap);
		
		resp.getWriter().write(result.toString());
		resp.setStatus(200);
	}
	
	protected Map<String, String[]> getParamMap(HttpServletRequest req) {
		@SuppressWarnings("unchecked")
		Map<String, String[]> paramMap = new HashMap<>(req.getParameterMap());
		//paramMap.remove("user");
		//paramMap.remove("page");
		paramMap.remove("object");
		//paramMap.remove("time");
		paramMap.remove("structure");
		paramMap.remove("poll");
		return paramMap;
	}
	
	protected <T> JSONObject getJSON(String objectId, String user, String pollStr, String timeString,
			ServletPageProvider<T> pageprov, String returnStruct, Map<String, String[]> paramMap) {
		JSONObject result = new JSONObject();
		if(pageprov == null) {
			String message = "In Userservlet page not found: "+getParameter("page", paramMap);
			if(Boolean.getBoolean("org.smartrplace.util.frontend.servlet.servererrorstoconsole"))
				System.out.println(message);
			else
				logger.info("Servlet provider exception: {}", message);
			result.put("exception", message);
			return result;
		}

		final ReturnStructure retStruct;
		if(returnStruct == null)
			retStruct = pageprov.getGETStructure();
		else {
			if(returnStruct.toLowerCase().equals("list"))
				retStruct = ReturnStructure.LIST;
			else
				retStruct = ReturnStructure.DICTIONARY;
		}
		Collection<T> objects = getObjects(objectId, user, pageprov);
		if(objects == null || objects.contains(null)) {
			String message = "In Userservlet object not found:"+objectId+" Pageprov:"+pageprov.getClass().getName();
			if(Boolean.getBoolean("org.smartrplace.util.frontend.servlet.servererrorstoconsole"))
				System.out.println(message);
			else
				logger.info("Servlet provider exception: {}", message);
			result.put("exception", message);
			return result;
		}
		
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
			//if(obj == null) continue;
			Map<String, ServletValueProvider> data = null;
			String objStr = null;
			try {
				data = pageprov.getProviders(obj, user, paramMap);
				objStr = pageprov.getObjectId(obj);
			} catch(Exception e) {
				if(Boolean.getBoolean("org.smartrplace.util.frontend.servlet.servererrorstoconsole"))
					e.printStackTrace();
				else
					logger.info("Servlet provider exception: ", e);
				result.put("exception", e.toString());
				return result;
			}
			JSONObject subJson;
			final JSONArray subJsonArr;
			if(retStruct == ReturnStructure.LIST) {
				subJson = null;
				subJsonArr = new JSONArray();
			} else {
				subJson = new JSONObject();
				subJsonArr = null;
			}
			//if(obj instanceof Resource)
			//	objStr = ResourceUtils.getHumanReadableShortName((Resource)obj);
			//else
			//	objStr = obj.toString();
			for(Entry<String, ServletValueProvider> prov: data.entrySet()) {
				ServletValueProvider valprov = prov.getValue();
				final String jsonkey;
				if(retStruct == ReturnStructure.LIST) {
					subJson = new JSONObject();
					subJson.put("key", prov.getKey());
					jsonkey = "value";
				} else
					jsonkey = prov.getKey();
				if(doPoll) {
					if(valprov == null)
						continue;
					if(valprov.getPollInterval() > pollInterval)
						continue;
				}
				try {
				if(valprov == null) {
					subJson.put(jsonkey, "n/a");
					continue;
				}
				String key;
				if(timeString != null)
					key = prov.getKey()+TIMEPREFIX+timeString;
				else
					key = prov.getKey();
				if(valprov.getValueMode() == ValueMode.STRING) {
					Value value = valprov.getValue(user, key);
					if(value == null)
						subJson.put(jsonkey, "");
					if(value instanceof FloatValue) {
						float val = value.getFloatValue();
						if(Float.isNaN(val))
							subJson.put(jsonkey, "NaN");
						else
							subJson.put(jsonkey, val);
					} else if(value instanceof IntegerValue)
						subJson.put(jsonkey, value.getIntegerValue());
					else if(value instanceof DoubleValue)
						subJson.put(jsonkey, value.getDoubleValue());
					else if(value instanceof BooleanValue)
						subJson.put(jsonkey, value.getBooleanValue());
					else if(value instanceof LongValue)
						subJson.put(jsonkey, value.getLongValue());
					else if(value instanceof StringValue)
						subJson.put(jsonkey, value.getStringValue());
					else
						subJson.put(jsonkey, value);
				} else {
					JSONObject value = valprov.getJSON(user, key);
					subJson.put(jsonkey, value);
				}
				} catch(Exception e) {
					subJson.put(jsonkey, e.toString());
					if(Boolean.getBoolean("org.smartrplace.util.frontend.servlet.servererrorstoconsole"))
						e.printStackTrace();
					else
						logger.info("Servlet exception: ", e);
				}
				if(retStruct == ReturnStructure.LIST)
					subJsonArr.put(subJson);
			}
			if(retStruct == ReturnStructure.LIST) {
				result.put(objStr, subJsonArr);
			} else
				result.put(objStr, subJson);
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
		String timeStr = req.getParameter("time");
		Map<String, String[]> paramMap = getParamMap(req);
		
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
			postJSON(object, user, result, pageMap, response, timeStr, paramMap);
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
	
	protected <T> String postJSON(String objectId, String user, JSONObject result,
			ServletPageProvider<T> pageprov, String response, String timeString,
			Map<String, String[]> paramMap) {
		Collection<T> objects = getObjects(objectId, user, pageprov);
		if(objects == null) return response;
		
		for(T obj: objects) {
			Map<String, ServletValueProvider> userMap = pageprov.getProviders(obj, user, paramMap);
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
					String keyForSetValue;
					if(timeString != null)
						keyForSetValue = key+TIMEPREFIX+timeString;
					else
						keyForSetValue = key;
					prov.setValue(user, keyForSetValue, value);
				} catch(Exception e) {
					if(objectId != null)
						throw new IllegalStateException(key+" cannot be processed for "+pageprov.toString()+", object; "+e.getMessage()+objectId, e);
					else
						throw new IllegalStateException(key+" cannot be processed for "+pageprov.toString()+", object not provided; "+e.getMessage(), e);
				}
			}
		}
		
		return response;
	}

	public static String getParameter(String name, Map<String, String[]> paramMap) {
		String[] arr = paramMap.get(name);
		if(arr==null)
			return null;
		if(arr.length == 0)
			return null;
		return arr[0];
	}
	
	public static void addValueEntry(ValueResource res, JSONObject result) {
		addValueEntry(res, "value", result);
	}
	public static void addValueEntry(ValueResource res, String valueKey, JSONObject result) {
		if(res instanceof FloatResource) {
			float val = ((FloatResource)res).getValue();
			addValueEntry(val, valueKey, result);
		} else if(res instanceof StringResource)
			result.put(valueKey, ((StringResource)res).getValue());
		else if(res instanceof IntegerResource)
			result.put(valueKey, ((IntegerResource)res).getValue());
		else if(res instanceof BooleanResource)
			result.put(valueKey, ((BooleanResource)res).getValue());
		else if(res instanceof TimeResource)
			result.put(valueKey, ((TimeResource)res).getValue());	
	}
	public static void addValueEntry(float value, String valueKey, JSONObject result) {
		if(Float.isNaN(value))
			result.put(valueKey, "NaN");
		else if(value == Float.POSITIVE_INFINITY)
			result.put(valueKey, Float.MAX_VALUE);				
		else if(value == Float.NEGATIVE_INFINITY)
			result.put(valueKey, -Float.MAX_VALUE);				
		else
			result.put(valueKey, value);		
	}
}
