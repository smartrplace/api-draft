package org.smartrplace.util.frontend.servlet;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.ogema.core.channelmanager.measurements.StringValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.ResourceAccess;
import org.ogema.core.timeseries.InterpolationMode;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.timeseries.eval.simple.api.KPIResourceAccess;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletPageProvider;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.util.resource.ValueResourceHelper;

public class UserServletUtil {
	public static final Map<String, TimeSeriesDataImpl> knownTS = new HashMap<>();
	
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
		} else if(value == Float.POSITIVE_INFINITY || value == Float.MAX_VALUE) {
			if(!suppressNan)
				result.put(valueKey, Float.MAX_VALUE);				
		} else if(value == Float.NEGATIVE_INFINITY || value == -Float.MAX_VALUE) {
			if(!suppressNan)
				result.put(valueKey, -Float.MAX_VALUE);				
		} else {
			double dval = ValueResourceHelper.getFloatAsDouble(value);
			result.put(valueKey, dval);
		}
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
		String tsID = getOrAddTimeSeriesData(pdata);
		if(tsID != null) {
			if(mval.additionalValues == null)
				mval.additionalValues = new HashMap<>();
			mval.additionalValues.put("timeseries", new StringValue(tsID));
		}		
	}
	
	/** Make sure time series data is registered with TimeSeriesServletBase
	 * 
	 * @param pdata
	 * @return timeseriesID
	 */
	public static String getOrAddTimeSeriesData(UserServletParamData pdata) {
		TimeSeriesDataImpl tsd = null;
		if(pdata.tsData != null) {
			tsd = pdata.tsData;
		} else if(pdata.tsDataRaw != null && pdata.tsLocationOrBaseId != null) {
			String hash = ServletPageProvider.getNumericalIdString(pdata.tsLocationOrBaseId);
			tsd = new TimeSeriesDataImpl(pdata.tsDataRaw, hash, hash, InterpolationMode.NONE);
		}
		return getOrAddTimeSeriesData(tsd);
	}
	public static TimeSeriesDataImpl getOrAddTimeSeriesDataPlus(UserServletParamData pdata) {
		TimeSeriesDataImpl tsd = null;
		if(pdata.tsData != null) {
			tsd = pdata.tsData;
		} else if(pdata.tsDataRaw != null && pdata.tsLocationOrBaseId != null) {
			String hash = ServletPageProvider.getNumericalIdString(pdata.tsLocationOrBaseId); //pdata.tsLocationOrBaseId.hashCode();
			tsd = new TimeSeriesDataImpl(pdata.tsDataRaw, hash, hash, InterpolationMode.NONE);
		}
		getOrAddTimeSeriesData(tsd);
		return tsd;
	}
	
	public static String getOrAddTimeSeriesData(TimeSeriesDataImpl tsd) {
		if(tsd != null) {
			UserServlet.knownTS.put(tsd.label(null), tsd);
			return tsd.label(null);
		}
		return null;		
	}

	public static String getOrAddTimeSeriesData(ReadOnlyTimeSeries tsDataRaw, String tsLocationOrBaseId) {
		String hash = ServletPageProvider.getNumericalIdString(tsLocationOrBaseId);
		TimeSeriesDataImpl tsd = new TimeSeriesDataImpl(tsDataRaw, hash, hash, InterpolationMode.NONE);
		return getOrAddTimeSeriesData(tsd);
	}
	public static TimeSeriesDataImpl getOrAddTimeSeriesDataPlus(ReadOnlyTimeSeries tsDataRaw, String tsLocationOrBaseId) {
		String hash = ServletPageProvider.getNumericalIdString(tsLocationOrBaseId);
		TimeSeriesDataImpl tsd = new TimeSeriesDataImpl(tsDataRaw, hash, hash, InterpolationMode.NONE);
		getOrAddTimeSeriesData(tsd);
		return tsd;
	}
	
	/** Get location/name/other String objectId based on numerical or String object identifiert
	 * 
	 * @param objectId
	 * @param objectIdPostiveOnly value of {@link ServletPageProvider#objectIdPostiveOnly()} of relevant provider
	 * @return
	 */
	public static String getStringObjectId(String objectId, boolean objectIdPostiveOnly) {
		int numId = 0;
		final String result;
		if(objectId != null) {
			try {
				numId = Integer.parseInt(objectId);
				result = UserServlet.num2stringObjects.get(numId);
				return result;
			} catch(NumberFormatException e) {
				UserServlet.num2StringPut(objectId, objectIdPostiveOnly);
				return objectId;
			}
		} else
			return null;
	}
	
	public static boolean initDeviceIdsDone = false;
	public static String getStringObjectId(String objectId, boolean objectIdPostiveOnly, DatapointService dpService) {
		String result = getStringObjectId(objectId, objectIdPostiveOnly);
		if(result != null || initDeviceIdsDone)
			return result;
		initDeviceIdsDone = true;
		for(InstallAppDevice iad: dpService.managedDeviceResoures(null)) {
			UserServlet.num2StringPut(iad.device().getLocation(), true);
			UserServlet.num2StringPut(iad.device().getLocation(), false);
		}
		return getStringObjectId(objectId, objectIdPostiveOnly);
	}

	
	public static <T extends Resource> T getObject(String objectId, Collection<T> allObjects) {
		for(T resource: allObjects) {
			if(resource.getName().equals(objectId)) return resource;
			if(resource.getSubResource("name", StringResource.class).getValue().equals(objectId)) return resource;
			if(resource.getLocation().equals(objectId)) return resource;
		}
		return null;
	}

	public static Room getRoomById(String objectId, ResourceAccess resAcc) {
		List<Room> rooms = KPIResourceAccess.getRealRooms(resAcc);
		String objectIdLoc = getStringObjectId(objectId, true);
		return getObject(objectIdLoc, rooms);
	}
	public static Room getRoomById(String objectId, ResourceAccess resAcc, DatapointService dpService) {
		List<Room> rooms = KPIResourceAccess.getRealRooms(resAcc);
		String objectIdLoc = getStringObjectId(objectId, true, dpService);
		return getObject(objectIdLoc, rooms);
	}
	
	public static PhysicalElement getDeviceById(String objectId, ResourceAccess resAcc, DatapointService dpService) {
		String devLoc = UserServletUtil.getStringObjectId(objectId, false, dpService);
		return resAcc.getResource(devLoc);
	}

	public static boolean isDepthTimeSeries(Map<String, String[]> paramMap) {
		String depth = UserServlet.getParameter("depth", paramMap);
		if(depth == null)
			return false;
		return depth.contains("timeseries");
	}
	public static boolean isValueOnly(Map<String, String[]> paramMap) {
		String depth = UserServlet.getParameter("depth", paramMap);
		if(depth == null)
			return false;
		return depth.equalsIgnoreCase("valueOnly");
	}
	public static boolean isPOST(Map<String, String[]> paramMap) {
		String method = UserServlet.getParameter("METHOD", paramMap);
		if(method == null)
			return false;
		return method.equals("POST");
	}
	
	public static String getHashWithPrefix(String preFix, String toHash) {
		int hash = toHash.hashCode();
		if(hash >= 0)
			return preFix+"P"+hash;
		else
			return preFix+"N"+Math.abs(hash);
	}

	public static JSONObject getJSONObjectOfPublic(Object obj) {
		JSONObject result = new JSONObject();
		Method[] methods = obj.getClass().getMethods();
		for(Method method: methods) {
			if(method.getParameters().length != 0)
				continue;
			String name = method.getName();
			if(name.equals("getClass") || name.equals("hashCode") || name.equals("toString"))
				continue;
			if(name.startsWith("get"))
				name = name.substring(3, 4).toLowerCase()+name.substring(4);
			try {
				Object value = method.invoke(obj);
				if(value instanceof Float)
					result.put(name, (Float)value);
				else if(value instanceof Double)
					result.put(name, (Double)value);
				else if(value instanceof Integer)
					result.put(name, (Integer)value);
				else if(value instanceof Boolean)
					result.put(name, (Boolean)value);
				else if(value instanceof Long)
					result.put(name, (Long)value);
				else
					result.put(name, value);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | JSONException e) {
				//e.printStackTrace();
			} catch(Exception e) {
				result.put(name, e.getMessage());
				e.printStackTrace();
			}
		}
		return result ;
	}
	
	/** Access supervision*/
	public static class LastAccessData {
		/** URL and servlet path*/
		public String lastServletPath;
		
		public long lastAccess;
		public long lastStartTimeRequested;
		public long lastEndTimeRequested;
		public String user;
	}
	/** URL or datapoint location (for datapoint requests) -> LastAccessData*/
	public static Map<String, LastAccessData> lastEndpointAccess = new HashMap<>();
	public static LastAccessData getLastAccessData(String location) {
		LastAccessData result = lastEndpointAccess.get(location);
		if(result == null) {
			result = new LastAccessData();
			result.lastServletPath = location;
			lastEndpointAccess.put(location, result);
		}
		return result;
	}

	/** Call this when the URL is accessed*/
	public static LastAccessData getLastAccessDataForEvent(String servletPath,
			long now) {
		LastAccessData result;
		result = getLastAccessData(servletPath);			
		result.lastAccess = now;
		return result;
	}
}
