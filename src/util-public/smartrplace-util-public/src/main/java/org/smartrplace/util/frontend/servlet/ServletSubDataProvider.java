package org.smartrplace.util.frontend.servlet;

import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.util.frontend.servlet.UserServlet.GetObjectResult;
import org.smartrplace.util.frontend.servlet.UserServlet.JSONVarrRes;
import org.smartrplace.util.frontend.servlet.UserServlet.ReturnStructure;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

/** Add JSON provided by ServletValueProvider into another ServletValueProvider
 */
public class ServletSubDataProvider<T> implements ServletValueProvider {
	final static Logger logger = LoggerFactory.getLogger(UserServlet.class);
	
	protected final ServletPageProvider<T> provider;
	protected final T object;
	protected final boolean useNumericalId;
	/** The implementation can add addtional elements to the result overwriting this method*/
	protected void addAdditionalInformation(JSONObject result) {};
	
	protected final UserServletParamData paramD;
	protected final Map<String, String[]> parameters;

	protected final ReturnStructure returnStruct;
	
	/** 
	 * @param provider
	 * @param object may be null. In this case all elements provided by getAllObjects of provider are used
	 * @param useNumericalId
	 * @param parameters
	 */
	public ServletSubDataProvider(ServletPageProvider<T> provider, T object,
			boolean useNumericalId,
			Map<String, String[]> parameters) {
		this(provider, object, useNumericalId, ReturnStructure.DICTIONARY, parameters);
	}
	/** 
	 * 
	 * @param provider
	 * @param object may be null. In this case all elements provided by getAllObjects of provider are used
	 * @param useNumericalId
	 * @param returnStruct
	 * @param parameters
	 */
	public ServletSubDataProvider(ServletPageProvider<T> provider, T object,
			boolean useNumericalId, ReturnStructure returnStruct,
			Map<String, String[]> parameters) {
		this.provider = provider;
		this.object = object;
		this.useNumericalId = useNumericalId;
		this.paramD = new UserServletParamData(parameters);
		this.returnStruct = returnStruct;
		this.parameters = parameters;
	}
	
	@Override
	public ValueMode getValueMode() {
		return ValueMode.JSON;
	}
	
	@Override
	public JSONVarrRes getJSON(String user, String key) {
		//JSONObject result = new JSONObject();
		String[] exist;
		if(object == null)
			exist = parameters.remove("object");
		else {
			String objId = provider.getObjectId(object);
			exist = parameters.put("object", new String[] {objId});
		}
		JSONVarrRes json = UserServlet.getJSON(user, null, paramD.timeString, provider,
				returnStruct,
				parameters, logger, "ServletSubDataProvider", null);
		if(object != null && returnStruct == ReturnStructure.TOPARRAY_DICTIONARY) {
			json.result = json.resultArr.getJSONObject(0);
			json.resultArr = null;
		}
		//String objectId = provider.getObjectId(object);
		//if(useNumericalId) {
		//	long id = provider.getNumericalId(objectId);
		//	result.put("id", id);
		//} else
		//	result.put("id", objectId);
		if(json.result != null)
			addAdditionalInformation(json.result);
		if(exist != null)
			parameters.put("object", exist);
		return json;
	}
	
	@Override
	public Value getValue(String user, String key) {
		throw new IllegalStateException("Returns JSON");
	}
	
	@Override
	public void setValue(String user, String keyMain, String value) {
		JSONObject in = new JSONObject(value);
		Iterator<String> keys = in.keys();
		while(keys.hasNext()) {
		    String key = keys.next();
		    if (in.get(key) instanceof JSONObject) {
				String timeString = UserServlet.getParameter("time", parameters);
				GetObjectResult<T> odata = UserServlet.getObjects(user, provider, key, true);
				UserServlet.postJSON(user, (JSONObject)in.get(key), provider, timeString, parameters, odata);
		    }
		}
		
		//String timeString = UserServlet.getParameter("time", parameters);
		//UserServlet.postJSON(user, in, provider, timeString, parameters);
	}
}
