package org.smartrplace.util.frontend.servlet;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.StringValue;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.timeseries.InterpolationMode;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;

public class UserServletUtil {
	public static void addValueEntry(ValueResource res, boolean suppressNan,
			JSONObject result) {
		addValueEntry(res, "value", suppressNan, result);
	}
	public static void addValueEntry(ValueResource res, String valueKey, boolean suppressNan,
			JSONObject result) {
		if(res instanceof FloatResource) {
			float val = ((FloatResource)res).getValue();
			addValueEntry(val, valueKey, suppressNan, result);
		} else if(res instanceof StringResource)
			result.put(valueKey, ((StringResource)res).getValue());
		else if(res instanceof IntegerResource)
			result.put(valueKey, ((IntegerResource)res).getValue());
		else if(res instanceof BooleanResource)
			result.put(valueKey, ((BooleanResource)res).getValue());
		else if(res instanceof TimeResource)
			result.put(valueKey, ((TimeResource)res).getValue());	
	}
	public static void addValueEntry(float value, String valueKey, boolean suppressNan,
			JSONObject result) {
		if(Float.isNaN(value)) {
			if(!suppressNan)
				result.put(valueKey, "NaN");
		} else if(value == Float.POSITIVE_INFINITY)
			result.put(valueKey, Float.MAX_VALUE);				
		else if(value == Float.NEGATIVE_INFINITY)
			result.put(valueKey, -Float.MAX_VALUE);				
		else
			result.put(valueKey, value);		
	}
	
	/** In this method Nan values are suppressed in general*/
	public static Float getJSONValue(float value) {
		if(Float.isNaN(value))
			return null;
		else if(value == Float.POSITIVE_INFINITY)
			return Float.MAX_VALUE;				
		else if(value == Float.NEGATIVE_INFINITY)
			return Float.MAX_VALUE;				
		else
			return value;				
	}

	public static boolean suppressNan(Map<String, String[]> parameters) {
		return UserServlet.getBoolean("suppressNaN", parameters);
	}
	
	public static void addTimeSeriesData(UserServletParamData pdata, MultiValue mval) {
		TimeSeriesDataImpl tsd = null;
		if(pdata.tsData != null) {
			tsd = pdata.tsData;
		} else if(pdata.tsDataRaw != null && pdata.tsLocationOrBaseId != null) {
			String hash = ""+pdata.tsLocationOrBaseId.hashCode();
			tsd = new TimeSeriesDataImpl(pdata.tsDataRaw, hash, hash, InterpolationMode.NONE);
		}
		if(tsd != null) {
			if(mval.additionalValues == null)
				mval.additionalValues = new HashMap<>();
			mval.additionalValues.put("timeseries", new StringValue(tsd.label(null)));
			if(pdata.tsMap != null)
				pdata.tsMap.put(tsd.label(null), tsd);
		}
	}
}
