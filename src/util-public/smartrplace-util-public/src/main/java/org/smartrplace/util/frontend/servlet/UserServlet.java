package org.smartrplace.util.frontend.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
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
import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.channelmanager.measurements.BooleanValue;
import org.ogema.core.channelmanager.measurements.DoubleValue;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.IntegerValue;
import org.ogema.core.channelmanager.measurements.LongValue;
import org.ogema.core.channelmanager.measurements.ObjectValue;
import org.ogema.core.channelmanager.measurements.StringValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.ResourceUtils;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.gateway.device.GatewayDevice;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider.ValueMode;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Core class to provide servlets based on widgets pages
 * TODO: Entire package to be moved to smartrplace-util-proposed or similar location for Utils*/
public class UserServlet extends HttpServlet {
	private static final long serialVersionUID = -462293886580458217L;
	public static final String TIMEPREFIX = "&time=";
	public final String servletSubUrl;
	
	/** This is the default property check that may be used also by other servlets if no special property is needed*/
	protected String getPropertyToCheck() {
		return "org.smartrplace.apps.heatcontrol.servlet.istestinstance";
	}
    public boolean isTestInstance(final HttpServletResponse resp) {
    	return (Boolean.getBoolean(getPropertyToCheck())||Boolean.getBoolean("org.smartrplace.util.frontend.servlet.istestinstance"));
    }

	/** May be null*/
	private final ApplicationManagerPlus appManPlus;

	/** Management of timeseriesIDs*/
	public static final Map<String, TimeSeriesDataImpl> knownTS = new HashMap<>();
	
	/** Management of known pages
	 * ServletClassName -> pageName -> Provider*/
	private static Map<String, Map<String, ServletPageProvider<?>>> knownPages = new HashMap<>();
	public static ServletPageProvider<?> getProvider(String servletClassName, String providerName) {
		if(servletClassName == null) {
			for(Map<String, ServletPageProvider<?>> myMap: knownPages.values()) {
				ServletPageProvider<?> result = getProvider(providerName, myMap);
				if(result != null)
					return result;
			}
			return null;
		} else
			return getProvider(providerName, knownPages.get(servletClassName));
	}
	protected static ServletPageProvider<?> getProvider(String providerName, Map<String, ServletPageProvider<?>> myMap) {
		if(myMap == null)
			return null;
		return myMap.get(providerName);
	}
	public static void addProvider(String servletClassName, String providerName, ServletPageProvider<?> provider) {
		Map<String, ServletPageProvider<?>> myMap = knownPages.get(servletClassName);
		if(myMap == null) {
			myMap = new HashMap<>();
			knownPages.put(servletClassName, myMap);
		}
		myMap.put(providerName, provider);		
	}
	
	/** Management of numerical IDs*/
	protected static final Map<Integer, String> num2stringObjects = new HashMap<>();
	protected static void num2StringPut(String value) {
		try {
			int num = Integer.parseInt(value);
			num2stringObjects.put(num, value);
		} catch(NumberFormatException e) {
			int num = value.hashCode();
			num2stringObjects.put(num, value);
		}
		
	}
 	
	public static final String TimeSeriesServletImplClassName = "org.smartrplace.app.monbase.servlet.TimeseriesBaseServlet";
	
	final Logger logger = LoggerFactory.getLogger(UserServlet.class);
	public enum ReturnStructure {
		/**In this case a list is returned with each element having an element named "key". Additional
		standard elements may be defined in the future when elements are generated from
		widgets*/
		LIST,
		DICTIONARY,
		TOPARRAY_DICTIONARY
	}
	public interface ServletPageProvider<T extends Object> {
		/** Create single data providers for an object
		 * 
		 * @param object
		 * @param user
		 * @param parameters
		 * @return null if object shall not be added to the result
		 */
		Map<String, ServletValueProvider> getProviders(T object, String user, Map<String, String[]> parameters);
		
		/** Usually this should be implemented.
		 *  
		 * @param user
		 * @return if null then {@link #getObject(String)} must be implemented. In this case an
		 * 		object has to be specified with each request. This would be suitable for accessing larget databases.
		 */
		Collection<T> getAllObjects(String user);
		
		/** Page may provide object based on ID. In this case data for a single object can be read via the servlet
		 * @param user */
		default T getObject(String objectId, String user) {return null;}
		
		default T createObject(String objectId) {return null;}
		
		default ReturnStructure getGETStructure() {return ReturnStructure.DICTIONARY;}
		
		default String getObjectId(T obj) {
			if(obj instanceof Resource)
				return ResourceUtils.getHumanReadableShortName((Resource)obj);
			else
				return obj.toString();			
		}
		
		/** By default the object can be obtained via a REST path element or parameter named object.
		 * If this is set also a more expressive String can be used like "building", "room" or similar.
		 * @return
		 */
		default String getObjectName() { return null;}
		
		default int getNumericalId(String stringId) {
			return stringId.hashCode();
		}
	}
	public interface ServletValueProvider {
		/** Called on GET*/
		Value getValue(String user, String key);
		default JSONVarrRes getJSON(String user, String key) {throw new UnsupportedOperationException("Not implemented for VALUEMODE STRING");}
		/** Called on POST*/
		default void setValue(String user, String key, String value) {};
		default long getPollInterval() {return 30000l;}
		public enum ValueMode {
			VALUE,
			JSON
		}
		/** Define how the values shall be obtained
		 * 
		 * @return if JSON is returned then {@link #getJSON(String, String)} is used instead of {@link #getValue(String, String)}
		 */
		default ValueMode getValueMode() {return ValueMode.VALUE;} 
	}

	final protected Map<String, ServletPageProvider<?>> pages = new HashMap<>();
	protected String stdPageId = null;
	
	public UserServlet(String servletPath, ApplicationManagerPlus appManPlus) {
		this.appManPlus = appManPlus;
		if(servletPath == null)
			this.servletSubUrl = null;
		else {
			int endidx = servletPath.endsWith("*")?endidx=servletPath.length()-1:servletPath.length();
			if(servletPath.startsWith("/apiweb"))
				this.servletSubUrl = servletPath.substring("/apiweb".length(), endidx);
			else if(servletPath.startsWith("/apimobile"))
				this.servletSubUrl = servletPath.substring("/apimobile".length(), endidx);
			else
				this.servletSubUrl = null;
		}
	}
	public UserServlet(ApplicationManagerPlus appManPlus) {
		this(null, appManPlus);
	}
	public UserServlet() {
		this(null, null);
	}

	public void addPage(String pageId, ServletPageProvider<?> prov) {
		if(stdPageId == null)
			stdPageId = pageId;
		pages.put(pageId, prov);
		addProvider(prov.getClass().getName(), pageId, prov);
	}
	
    @Override
    protected void doOptions(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
		@SuppressWarnings("unused")
		String user = GUIUtilHelper.getUserLoggedInBase(req.getSession());
		if(isTestInstance(resp)) {
			//NOTE: This is not really relevant here as /apiweb/ is accessed via sessin and thus
			//cannot be accessed cross-site
			resp.setCharacterEncoding("UTF-8");
	    	resp.setContentType("application/json");
	    	resp.addHeader("Access-Control-Allow-Origin", "*"); //CORS header
	        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
	        resp.addHeader("Access-Control-Allow-Headers", "*");
	        //resp.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept");
	        resp.addHeader("Access-Control-Allow-Credentials", "true");
		}
    }

    @Override
    protected void doHead(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		//NOTE: This is not really relevant here as /apiweb/ is accessed via sessin and thus
		//cannot be accessed cross-site
		resp.setCharacterEncoding("UTF-8");
		resp.setContentType("application/json");
		resp.addHeader("Access-Control-Allow-Origin", "*"); //CORS header
		resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
		resp.addHeader("Access-Control-Allow-Headers", "*");
		resp.addHeader("Access-Control-Allow-Credentials", "true");
    	super.doHead(req, resp);
    }

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		//String user = req.getParameter("user");
		//if(user == null || user.startsWith("["))
		String user = GUIUtilHelper.getUserLoggedInBase(req.getSession());
		if(isTestInstance(resp)) {
			//NOTE: This is not really relevant here as /apiweb/ is accessed via sessin and thus
			//cannot be accessed cross-site
			resp.setCharacterEncoding("UTF-8");
	    	resp.setContentType("application/json");
	    	resp.addHeader("Access-Control-Allow-Origin", "*"); //CORS header
	        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
	        resp.addHeader("Access-Control-Allow-Headers", "*");
	        //resp.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept");
	        resp.addHeader("Access-Control-Allow-Credentials", "true");
		}
		doGet(req, resp, user, false);
	}
	void doGet(HttpServletRequest req, HttpServletResponse resp, String user, boolean isMobile)
			throws ServletException, IOException {
		Map<String, String[]> paramMap = getParamMap(req);
		addParametersFromUrl(req, paramMap);
		doGet(req, resp, user, paramMap, isMobile);
	}
	void doGet(HttpServletRequest req, HttpServletResponse resp, String user, Map<String, String[]> paramMap, boolean isMobile)
			throws ServletException, IOException {
		//String object = req.getParameter("object");
		
		String timeStr = UserServlet.getParameter("time", paramMap);
		String returnStruct = UserServlet.getParameter("structure", paramMap);
		//if(user == null) return;
		String pageId1 = UserServlet.getParameter("page", paramMap);
		List<String> pageList = null;
		if(pageId1 == null) {
			String pageIds = UserServlet.getParameter("pages", paramMap);
			if(pageIds != null)
				pageList = StringFormatHelper.getListFromString(pageIds);
			else
				pageId1 = stdPageId;
		}
		if(pageList == null) {
			pageList = new ArrayList<>();
			pageList.add(pageId1);
		}
		String pollStr = UserServlet.getParameter("poll", paramMap);
		
		JSONVarrRes result;
		if(pageList.isEmpty())
			throw new IllegalStateException("No page found!!");
		else if(pageList.size() == 1) {
			ServletPageProvider<?> pageMap = pages.get(pageList.get(0));
			result = getJSON(user, pollStr, timeStr, pageMap, returnStruct, paramMap);			

			incrementAccessCounter(pageList.get(0), isMobile);			
		} else {
			result = new JSONVarrRes();
			result.result = new JSONObject();
			for(String pageId: pageList) {
				ServletPageProvider<?> pageMap = pages.get(pageId);
				JSONVarrRes resultSub = getJSON(user, pollStr, timeStr, pageMap, returnStruct, paramMap);
				if(resultSub.result != null)
					result.result.put(pageId, resultSub.result);
				else
					result.result.put(pageId, resultSub.resultArr);

				incrementAccessCounter(pageId, isMobile);				
			}
		}
		resp.addHeader("content-type", "application/json;charset=utf-8");
		String out;
		if(result.result != null) {
			out = result.result.toString();
		} else {
			out = result.resultArr.toString();
		}
		resp.getWriter().write(out);
		if(logger.isDebugEnabled()) {
			String fullURL = req.getRequestURL().toString();
			String paramStr = req.getQueryString();
			if(paramStr != null)
				logger.debug("Finished GET for:"+fullURL+"?"+paramStr);
			else
				logger.debug("Finished GET for:"+fullURL);
			logger.trace("GET Response:"+out);
		}
		resp.setStatus(200);
	}
	
	protected Map<String, String[]> getParamMap(HttpServletRequest req) {
		@SuppressWarnings("unchecked")
		Map<String, String[]> paramMap = new HashMap<>(req.getParameterMap());
		//paramMap.remove("user");
		//paramMap.remove("page");
		//paramMap.remove("object");
		//paramMap.remove("time");
		//paramMap.remove("structure");
		//paramMap.remove("poll");
		return paramMap;
	}
	
	public static class JSONVarrRes {
		public JSONObject result = null;
		public JSONArray resultArr = null;
		public String message = null;
	}
	protected <T> JSONVarrRes getJSON(String user, String pollStr, String timeString,
			ServletPageProvider<T> pageprov, String returnStruct, Map<String, String[]> paramMap) {
		final ReturnStructure retStruct;
		if(returnStruct == null)
			retStruct = pageprov.getGETStructure();
		else {
			if(returnStruct.toLowerCase().equals("toparray"))
				retStruct = ReturnStructure.TOPARRAY_DICTIONARY;
			else if(returnStruct.toLowerCase().equals("list"))
				retStruct = ReturnStructure.LIST;
			else
				retStruct = ReturnStructure.DICTIONARY;
		}

		JSONVarrRes result = getJSON(user, pollStr, timeString, pageprov, retStruct, paramMap, logger);
		if(result.message != null) {
			writeMessage(result, "exception", result.message);
		}
		return result;
	}
	protected static <T> JSONVarrRes getJSON(String user, String pollStr, String timeString,
			ServletPageProvider<T> pageprov, ReturnStructure retStruct, Map<String, String[]> paramMap,
			Logger logger) {
		final boolean topArray = UserServlet.getBoolean("topArray", paramMap) || retStruct==ReturnStructure.TOPARRAY_DICTIONARY;
		if(retStruct == ReturnStructure.TOPARRAY_DICTIONARY)
			retStruct = ReturnStructure.DICTIONARY;
		JSONVarrRes res = new JSONVarrRes();
		if(topArray)
			res.resultArr = new JSONArray();
		else
			res.result = new JSONObject();
		boolean suppressNan = UserServletUtil.suppressNan(paramMap);
		
		if(pageprov == null) {
			res.message = "In Userservlet page not found: "+getParameter("page", paramMap);
			if(Boolean.getBoolean("org.smartrplace.util.frontend.servlet.servererrorstoconsole"))
				System.out.println(res.message);
			else
				logger.info("Servlet provider exception: {}", res.message);
			//writeMessage(res, "exception", message);
			//result.put("exception", message);
			return res;
		}

		GetObjectResult<T> odata = getObjects(user, pageprov, paramMap, false);
		if(odata.objects == null || odata.objects.contains(null)) {
			res.message = "In Userservlet object not found:"+odata.objectId+" Pageprov:"+pageprov.getClass().getName();
			if(Boolean.getBoolean("org.smartrplace.util.frontend.servlet.servererrorstoconsole"))
				System.out.println(res.message);
			else
				logger.info("Servlet provider exception: {}", res.message);
			//writeMessage(res, "exception", message);
			//result.put("exception", message);
			return res;
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
		
		int orgSize = odata.objects.size();
		int count = 0;
		try { for(T obj: odata.objects) {
			count++;
			//if(obj == null) continue;
			Map<String, ServletValueProvider> data = null;
			String objStr = null;
			try {
				data = pageprov.getProviders(obj, user, paramMap);
				if(data == null)
					continue;
				objStr = pageprov.getObjectId(obj);
			} catch(Exception e) {
				if(Boolean.getBoolean("org.smartrplace.util.frontend.servlet.servererrorstoconsole"))
					e.printStackTrace();
				else
					logger.info("Servlet provider exception: ", e);
				res.message = e.toString();
				//writeMessage(res, "exception", e.toString());
				//result.put("exception", e.toString());
				return res;
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
				if(valprov.getValueMode() == ValueMode.VALUE) {
					Value value = valprov.getValue(user, key);
					if(value == null)
						continue;
					if(value instanceof ObjectValue) {
						Object multiValObj = value.getObjectValue();
						if(!(multiValObj instanceof MultiValue))
							throw new IllegalArgumentException("Found ObjectValue with object of type:"+
									multiValObj.getClass().getName());
						 MultiValue multiVal = (MultiValue) multiValObj;
						 if(retStruct == ReturnStructure.LIST) {
							 if(multiVal.additionalValues != null) for(Entry<String, Value> add: multiVal.additionalValues.entrySet()) {
								 addValue(add.getValue(), add.getKey(), subJson, suppressNan);
							 }
							 if(multiVal.additionalJSON != null) for(Entry<String, JSONObject> add: multiVal.additionalJSON.entrySet()) {
								 subJson.put(add.getKey(), add.getValue());
							 }
							 if(multiVal.permissions != null) for(Entry<String, Boolean> add: multiVal.permissions.entrySet()) {
								 subJson.put(add.getKey(), add.getValue());
							 }
							 if(multiVal.isWritable != null)
								 subJson.put("POSTsupported", multiVal.isWritable);
						 }
						 value = multiVal.mainValue;
					}
					addValue(value, jsonkey, subJson, suppressNan);

				} else {
					JSONVarrRes valueF = valprov.getJSON(user, key);
					if(valueF == null)
						continue;
					if(valueF.result != null) {
						JSONObject value = valueF.result;
						if(value.toString() == null) {
							logger.info("JSON toString null for "+valprov.getClass().getName());
							continue;
						}
						subJson.put(jsonkey, value);
					} else {
						JSONArray value = valueF.resultArr;
						if(value.toString() == null) {
							logger.info("JSON toString null for "+valprov.getClass().getName());
							continue;
						}
						subJson.put(jsonkey, value);						
					}
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
				if(res.result != null)
					res.result.put(objStr, subJsonArr);
				else
					res.resultArr.put(subJsonArr);
			} else
				if(res.result != null)
					res.result.put(objStr, subJson);
				else
					res.resultArr.put(subJson);
		}
		} catch(ConcurrentModificationException e) {
			System.out.println("Count:"+count+"  Org size:"+orgSize+" now:"+odata.objects.size());
			e.printStackTrace();
		}
		return res;
	}
	
	
	protected void writeMessage(JSONVarrRes res, String key, String message) {
		if(res.result != null)
			res.result.put(key, message);
		else
			res.resultArr.put(key+" : "+message);
	}
	
	protected static class GetObjectResult<T> {
		public Collection<T> objects;
		public String objectId = null;
	}
	/** 
	 * 
	 * @param <T>
	 * @param user
	 * @param pageprov
	 * @param paramMap
	 * @param allowToCreate usually true for POST. In this case the ServletPageProvider may create an object to write to
	 * @return
	 */
	protected static <T> GetObjectResult<T> getObjects(String user, ServletPageProvider<T> pageprov, Map<String, String[]> paramMap,
			boolean allowToCreate) {
		String objectName = pageprov.getObjectName();
		String objectId = null;
		if(objectName != null)
			objectId = UserServlet.getParameter(objectName, paramMap);
		if(objectId == null)
			objectId = UserServlet.getParameter("object", paramMap);
		return getObjects(user, pageprov, objectId, allowToCreate);
	}
	protected static <T> GetObjectResult<T> getObjects(String user, ServletPageProvider<T> pageprov, String objectId,
				boolean allowToCreate) {
		GetObjectResult<T> result = new GetObjectResult<T>();
		int numId = 0;
		result.objectId = objectId;
		if(result.objectId != null) {
			try {
				numId = Integer.parseInt(result.objectId);
				result.objectId = num2stringObjects.get(numId);
			} catch(NumberFormatException e) {
				//int numIdNew = result.objectId.hashCode();
				num2StringPut(result.objectId);
			}
		}
		if(result.objectId != null) {
			T obj = pageprov.getObject(result.objectId, user);
			if(obj == null && allowToCreate)
				obj = pageprov.createObject(result.objectId);
			if(obj != null) {
				result.objects = new ArrayList<T>();
				result.objects.add(obj);
				return result;
			}
		}
		Collection<T> allObj = pageprov.getAllObjects(user);
		for(T obj: allObj) {
			String id = pageprov.getObjectId(obj);
			//int numIdNew = id.hashCode();
			num2StringPut(id);			
		}
		if(numId != 0) {
			//we try to find the object once more with the new information
			result.objectId = num2stringObjects.get(numId);
			if(result.objectId != null) {
				T obj = pageprov.getObject(result.objectId, user);
				result.objects = new ArrayList<T>();
				result.objects.add(obj);
				return result;				
			}
		}
		result.objects = Collections.unmodifiableList(new ArrayList<T>(allObj));
		return result;
	}
	
	/** SubUrl must end on '/' */
	protected void addParametersFromUrl(HttpServletRequest req, Map<String, String[]> paramMap, String servletSubUrl) {
		String fullURL = req.getRequestURL().toString();
		if(logger.isDebugEnabled())  {
			String paramStr = req.getQueryString();
			if(paramStr != null)
				logger.debug("Starting GET for(A):"+fullURL+"?"+paramStr);
			else
				logger.debug("Starting GET for(A):"+fullURL);
		}
		int idx = fullURL.indexOf(servletSubUrl);
		String[] subURL;
		if(idx >= 0)
			subURL = fullURL.substring(idx+servletSubUrl.length()).split("/");
		else {
			return;
		}
		if(subURL != null && subURL.length > 0) {
			int startIdx;
			String page = subURL[0];
			if(pages.containsKey(page)) {
				addParameter("page", page, paramMap);
				startIdx = 1;
			} else
				startIdx = 0;
			for(idx=startIdx; idx<subURL.length-1; idx+=2) {
				String paramName = subURL[idx];
				String param = subURL[idx+1];
				addParameter(paramName, param, paramMap);
			}
		}		
	}

	protected void addParametersFromUrl(HttpServletRequest req, Map<String, String[]> paramMap) {
		if(this.servletSubUrl != null) {
			addParametersFromUrl(req, paramMap, this.servletSubUrl);
			return;
		}
		String fullURL = req.getRequestURL().toString();
		if(logger.isDebugEnabled())  {
			String paramStr = req.getQueryString();
			if(paramStr != null)
				logger.debug("Starting GET for:"+fullURL+"?"+paramStr);
			else
				logger.debug("Starting GET for:"+fullURL);
		}
		int idx = fullURL.indexOf("/userdata/");
		String[] subURL;
		if(idx >= 0)
			subURL = fullURL.substring(idx+"/userdata/".length()).split("/");
		else {
			idx =  fullURL.indexOf("/userdatatest/");
			if(idx >= 0)
				subURL = fullURL.substring(idx+"/userdatatest/".length()).split("/");
			else
				subURL = null;
		}
		if(subURL != null && subURL.length > 1) {
			for(idx=0; idx<subURL.length-1; idx+=2) {
				String paramName = subURL[idx];
				String param = subURL[idx+1];
				addParameter(paramName, param, paramMap);
			}
		}		
	}
	
	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		//String user = req.getParameter("user");
		//if(user == null) return;
		String user = GUIUtilHelper.getUserLoggedInBase(req.getSession());
		doPost(req, resp, user, false);
		if(isTestInstance(resp)) {
			//NOTE: This is not really relevant here as /apiweb/ is accessed via sessin and thus
			//cannot be accessed cross-site
			resp.setCharacterEncoding("UTF-8");
	    	resp.setContentType("application/json");
	    	resp.addHeader("Access-Control-Allow-Origin", "*"); //CORS header
	        resp.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
	        resp.addHeader("Access-Control-Allow-Headers", "*");
	        //resp.addHeader("Access-Control-Allow-Headers", "origin, content-type, accept");
	        resp.addHeader("Access-Control-Allow-Credentials", "true");
		}
	}		
	protected void doPost(HttpServletRequest req, HttpServletResponse resp, String user, boolean isMobile)
			throws ServletException, IOException {

		Map<String, String[]> paramMap = getParamMap(req);
		addParametersFromUrl(req, paramMap);

		String pageId = req.getParameter("page");
		if(pageId == null) pageId = stdPageId ;
		//String object = req.getParameter("object");
		final ServletPageProvider<?> pageMap = pages.get(pageId);
		if(pageMap == null) return;
		String timeStr = req.getParameter("time");
		
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

		if(Boolean.getBoolean("org.smartrplace.util.frontend.servlet.logdetails")) {
			String fullURL = req.getRequestURL().toString();
			logger.info("POST message to "+fullURL);
			logger.info("POST body: "+request);
		}
		
		try {
			JSONObject result = new JSONObject(request);
			JSONObject params = result.optJSONObject("params");
			if(params != null) {
				boolean isGET = true;
				Map<String, Object> it = params.toMap();
				for(Entry<String, Object> el: it.entrySet()) {
					if(el.getValue() instanceof Collection) {
						@SuppressWarnings("unchecked")
						Iterator<Object> subit = ((Collection<Object>)el.getValue()).iterator();
						//List<String> paramvals = new ArrayList<>();
						while(subit.hasNext()) {
							Object subel = subit.next();
							addParameter(el.getKey(), subel.toString(), paramMap);
							//paramvals.add(subel.toString());
						}
					} else
						addParameter(el.getKey(), el.getValue().toString(), paramMap);
					if(el.getKey().equals("getData") && (el.getValue()!=null && "false".equals(el.getValue().toString())))
						isGET = false;
				}
				if(isGET) {
					if(getParameter("structure", paramMap) == null)
						addParameter("structure", "toparray", paramMap);
					doGet(req, resp, user, paramMap, isMobile);
					return;
				}
			}
			postJSON(user, result, pageMap, timeStr, paramMap);
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
	
	protected static void addValue(Value value, String jsonkey, JSONObject subJson,
			boolean suppressNan) {
		if(value == null)
			subJson.put(jsonkey, "");
		if(value instanceof FloatValue) {
			float val = value.getFloatValue();
			UserServletUtil.addValueEntry(val, jsonkey, suppressNan, subJson);
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
	}
	
	protected static <T> void postJSON(String user, JSONObject postData,
			ServletPageProvider<T> pageprov,
			//String response,
			String timeString,
			Map<String, String[]> paramMap) {
		GetObjectResult<T> odata = getObjects(user, pageprov, paramMap, true);
		postJSON(user, postData, pageprov, timeString, paramMap, odata);
	}
	protected static <T> void postJSON(String user, JSONObject postData,
			ServletPageProvider<T> pageprov,
			//String response,
			String timeString,
			Map<String, String[]> paramMap,
			GetObjectResult<T> odata) {
		if(odata.objects == null) return;
		if(odata.objects.size() > 1) {
			if(odata.objectId != null)
				throw new IllegalStateException("POST can be applied only to a single object, we found:"+odata.objects.size()+"for "+pageprov.toString()+", object:"+odata.objectId);
			else
				throw new IllegalStateException("POST can be applied only to a single object, we found:"+odata.objects.size()+"for "+pageprov.toString());
		}
		
		paramMap.put("METHOD", new String[] {"POST"});
		//for(T obj: odata.objects) {
		T obj = odata.objects.iterator().next();
		Map<String, ServletValueProvider> userMap = pageprov.getProviders(obj, user, paramMap);
		if(userMap == null) {
			if(odata.objectId != null)
				throw new IllegalStateException("Object not part of result in POST for "+pageprov.toString()+", object:"+odata.objectId);
			else
				throw new IllegalStateException("Object not part of result in POST for "+pageprov.toString());			
		}
		for(String key: postData.keySet()) {
			ServletValueProvider prov = userMap.get(key);
			if(prov == null)
				throw new IllegalStateException(key+" not available for "+pageprov.toString());
			String value;
			try {
				value = postData.get(key).toString();
			} catch(JSONException e) {
				value = postData.getJSONObject(key).toString();
			}
			try {
				String keyForSetValue;
				if(timeString != null)
					keyForSetValue = key+TIMEPREFIX+timeString;
				else
					keyForSetValue = key;
				prov.setValue(user, keyForSetValue, value);
			} catch(Exception e) {
				if(odata.objectId != null)
					throw new IllegalStateException(key+" cannot be processed for "+pageprov.toString()+", object:"+odata.objectId+", "+e.getMessage(), e);
				else
					throw new IllegalStateException(key+" cannot be processed for "+pageprov.toString()+", object not provided; "+e.getMessage(), e);
			}
		}
		//}
		
		//return response;
	}

	public static String getParameter(String name, Map<String, String[]> paramMap) {
		return getParameter(name, paramMap, null);
	}
	public static String getParameter(String name, Map<String, String[]> paramMap,
			String defaultValue) {
		String[] arr = paramMap.get(name);
		if(arr==null)
			return defaultValue;
		if(arr.length == 0)
			return defaultValue;
		return arr[0];
	}
	public static Integer getInteger(String name, Map<String, String[]> paramMap) {
		return getInteger(name, paramMap, null);
	}
	public static Integer getInteger(String name, Map<String, String[]> paramMap,
			Integer defaultValue) {
		String val = getParameter(name, paramMap);
		if(val == null)
			return defaultValue;
		try  {
			return Integer.parseInt(val);
		} catch(NumberFormatException e) {
			return defaultValue;
		}
	}
	public static Float getFloat(String name, Map<String, String[]> paramMap) {
		return getFloat(name, paramMap, null);
	}
	public static Float getFloat(String name, Map<String, String[]> paramMap,
			Float defaultValue) {
		String val = getParameter(name, paramMap);
		if(val == null)
			return defaultValue;
		try  {
			return Float.parseFloat(val);
		} catch(NumberFormatException e) {
			return defaultValue;
		}
	}
	public static boolean getBoolean(String name, Map<String, String[]> paramMap) {
		String val = getParameter(name, paramMap);
		return Boolean.parseBoolean(val);
	}

	public static OgemaLocale getLocale(Map<String, String[]> paramMap) {
		return getLocale(paramMap, null);
	}
	public static OgemaLocale getLocale(Map<String, String[]> paramMap, OgemaLocale defaultLocale) {
		String loc = getParameter("locale", paramMap);
		if(loc == null)
			return defaultLocale;
		OgemaLocale result = OgemaLocale.getLocale(loc);
		if(result == null)
			return defaultLocale;
		return result;
	}
	
	public static void addParameter(String paramName, String param, Map<String, String[]> paramMap) {
		String[] subMap = paramMap.get(paramName);
		if(subMap == null) {
			paramMap.put(paramName, new String[] {param});
		} else {
			List<String> newList = new ArrayList<String>(Arrays.asList(subMap));
			newList.add(param);
			paramMap.put(paramName, newList.toArray(new String[0]));
		}		
	}
	
	static GatewayDevice gatewayData = null;
	Map<String, Long> lastAccess = new HashMap<>();
	public static long API_ACCESS_AGGREGATION_TIME = 30000;
	protected void incrementAccessCounter(String methodIn, boolean isMobile) {
		if(gatewayData == null) {
			if(appManPlus == null)
				return;
			gatewayData = ResourceHelper.getLocalDevice(appManPlus.appMan());
			if(gatewayData == null)
				return;
		}
		long now;
		if(appManPlus != null)
			now = appManPlus.getFrameworkTime();
		else
			now = System.currentTimeMillis();
		String method;
		if(servletSubUrl != null)
			method = servletSubUrl+"-"+methodIn;
		else
			method = methodIn;
		Long last = lastAccess.get(method);
		if(last != null && (now - last < API_ACCESS_AGGREGATION_TIME))
			return;
		int idx = ValueResourceUtils.appendValueIfUniqueIndex(gatewayData.apiMethods(), method, true);
		ValueResourceHelper.setCreate(gatewayData.apiMethodAccess(), isMobile?(idx+0.5f):idx);
		lastAccess.put(method, now);
	}
}
