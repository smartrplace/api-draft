package org.smartrplace.util.frontend.servlet;

import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

/** Provides basic information for any resource. Currently implementation is only relevant for
 * SingleValueResources, but shall be extended in the future. Also POST support shall be implemented
 * in the future.
 */
public class ServletResourceDataProvider implements ServletValueProvider {
	protected final Resource res;
	/** The implementation can add addtional elements to the result overwriting this method*/
	protected void addAdditionalInformation(JSONObject result) {};
	
	protected boolean suppressLocation = false;
			
	public ServletResourceDataProvider(Resource res) {
		this.res = res;
	}
	
	@Override
	public ValueMode getValueMode() {
		return ValueMode.JSON;
	}
	
	@Override
	public JSONObject getJSON(String user, String key) {
		JSONObject result = new JSONObject();
		result.put("type", res.getResourceType().getName());
		if(res instanceof FloatResource)
			result.put("value", ((FloatResource)res).getValue());
		else if(res instanceof StringResource)
			result.put("value", ((StringResource)res).getValue());
		else if(res instanceof IntegerResource)
			result.put("value", ((IntegerResource)res).getValue());
		else if(res instanceof BooleanResource)
			result.put("value", ((BooleanResource)res).getValue());
		else if(res instanceof TimeResource)
			result.put("value", ((TimeResource)res).getValue());
		if(!suppressLocation)
			result.put("location", res.getLocation());
		addAdditionalInformation(result);
		return result;
	}
	
	@Override
	public Value getValue(String user, String key) {
		throw new IllegalStateException("Returns JSON");
	}

}
