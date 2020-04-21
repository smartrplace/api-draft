package org.smartrplace.util.frontend.servlet;

import java.util.Map;

import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.Value;

/** Intended to be packed into ObjectValue*/
public class MultiValue {
	public Value mainValue;
	/** May be null*/
	public Map<String, Value> additionalValues;
	/** May be null*/
	public Map<String, JSONObject> additionalJSON;
	/** This is a general information if the value supports POST requests. This is usually not
	 * implemented yet, but shall be available in the future. If the value is null then no information
	 * shall be added. TODO: Discuss whether this should be a list of supported methods like
	 * POST, DELETE, PUT*/
	public Boolean isWritable;
	/** key: UserReadPermission, UserWritePermission, in the future may be definde additional permissions,
	 * may be null*/
	public Map<String, Boolean> permissions;
}
