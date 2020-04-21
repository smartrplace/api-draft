package org.smartrplace.util.frontend.servlet;

import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.Value;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
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
	protected final boolean suppressNan;
			
	public ServletResourceDataProvider(Resource res, boolean suppressNan) {
		this.res = res;
		this.suppressNan = suppressNan;
	}
	
	@Override
	public ValueMode getValueMode() {
		return ValueMode.JSON;
	}
	
	@Override
	public JSONObject getJSON(String user, String key) {
		JSONObject result = new JSONObject();
		result.put("type", res.getResourceType().getName());
		if(res instanceof ValueResource)
			UserServletUtil.addValueEntry((ValueResource) res, suppressNan, result);
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
