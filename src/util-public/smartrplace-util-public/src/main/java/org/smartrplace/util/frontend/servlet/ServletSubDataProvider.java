package org.smartrplace.util.frontend.servlet;

import java.util.Map;

import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.smartrplace.util.frontend.servlet.UserServlet.JSONVarrRes;
import org.smartrplace.util.frontend.servlet.UserServlet.ReturnStructure;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

/** Provides basic information for any resource. Currently implementation is only relevant for
 * SingleValueResources, but shall be extended in the future. Also POST support shall be implemented
 * in the future.
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
	
	public ServletSubDataProvider(ServletPageProvider<T> provider, T object,
			boolean useNumericalId,
			Map<String, String[]> parameters) {
		this(provider, object, useNumericalId, ReturnStructure.DICTIONARY, parameters);
	}
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
				parameters, logger);
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
}
