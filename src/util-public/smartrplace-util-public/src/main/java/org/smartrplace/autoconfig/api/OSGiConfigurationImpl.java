package org.smartrplace.autoconfig.api;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.service.cm.Configuration;

public class OSGiConfigurationImpl implements OSGiConfiguration {
	protected final Dictionary<String, Object> dict;
	protected final Configuration config;
	
	public OSGiConfigurationImpl(Configuration config) {
		this.config = config;
		Dictionary<String, Object> dictLoc = config.getProperties();
    	if(dictLoc == null) {
    		dictLoc = new Hashtable<String, Object>();
    	}
    	this.dict = dictLoc;
	}

	@Override
	public String getPid() {
		return config.getPid();
	}

	@Override
	public String name() {
		return config.getBundleLocation()+"::"+config.toString();
	}

	@Override
	public Float getFloat(String key) {
		Object obj = dict.get(key);
		if(obj instanceof Float)
			return (Float) obj;
		try {
			if(obj instanceof Double)
				return (Float)(float)(double)obj;
			if(obj instanceof Integer)
				return (Float)(float)(int)obj;
			if(obj instanceof Long)
				return (Float)(float)(long)obj;
			if(obj instanceof Boolean)
				return (Float)((boolean)obj?1.0f:0.0f);
			if(obj instanceof String)
				return Float.parseFloat((String)obj);
		} catch(Exception e) {
			return null;
		}
		return null;
	}

	@Override
	public boolean setFloat(String key, float value, boolean commitImmediately) {
		dict.put(key, value);
		if(commitImmediately)
			return commitChanges();
		return true;
	}

	@Override
	public Long getLong(String key) {
		Object obj = dict.get(key);
		if(obj instanceof Long)
			return (Long) obj;
		try {
			if(obj instanceof Double)
				return (Long)(long)(double)obj;
			if(obj instanceof Integer)
				return (Long)(long)(int)obj;
			if(obj instanceof Float)
				return (Long)(long)(float)obj;
			if(obj instanceof Boolean)
				return (Long)(long)((boolean)obj?1l:0l);
			if(obj instanceof String)
				return Long.parseLong((String)obj);
		} catch(Exception e) {
			return null;
		}
		return null;
	}

	@Override
	public boolean setLong(String key, long value, boolean commitImmediately) {
		dict.put(key, value);
		if(commitImmediately)
			return commitChanges();
		return true;
	}

	@Override
	public Boolean getBoolean(String key) {
		Object obj = dict.get(key);
		if(obj instanceof Boolean)
			return (Boolean) obj;
		try {
			if(obj instanceof Double)
				return(long)(obj) >= 0.5;
			if(obj instanceof Integer)
				return(int)obj >= 1;
			if(obj instanceof Float)
				return (float)obj > 0.5f;
			if(obj instanceof Long)
				return (long)obj >= 1;
			if(obj instanceof String)
				try {
					return Float.parseFloat((String)obj) > 0.5f;
				} catch(NumberFormatException e) {
					return Boolean.parseBoolean((String)obj);					
				}
		} catch(Exception e) {
			return null;
		}
		return null;
	}

	@Override
	public boolean setBoolean(String key, boolean value, boolean commitImmediately) {
		dict.put(key, value);
		if(commitImmediately)
			return commitChanges();
		return true;
	}

	@Override
	public String getString(String key) {
		Object obj = dict.get(key);
		if(obj instanceof String)
			return (String) obj;
		return obj.toString();
	}

	@Override
	public boolean setString(String key, String value, boolean commitImmediately) {
		dict.put(key, value);
		if(commitImmediately)
			return commitChanges();
		return true;
	}

	@Override
	public Float[] getFloatArray(String key) {
		Object obj = dict.get(key);
		if(obj instanceof Float[])
			return (Float[]) obj;
		return null;
	}

	@Override
	public boolean setFloatArray(String key, Float[] value, boolean commitImmediately) {
		dict.put(key, value);
		if(commitImmediately)
			return commitChanges();
		return true;
	}

	@Override
	public Long[] getLongArray(String key) {
		Object obj = dict.get(key);
		if(obj instanceof Long[])
			return (Long[]) obj;
		return null;
	}

	@Override
	public boolean setLongArray(String key, Long[] value, boolean commitImmediately) {
		dict.put(key, value);
		if(commitImmediately)
			return commitChanges();
		return true;
	}

	@Override
	public Boolean[] getBooleanArray(String key) {
		Object obj = dict.get(key);
		if(obj instanceof Boolean[])
			return (Boolean[]) obj;
		return null;
	}

	@Override
	public boolean setBooleanArray(String key, Boolean[] value, boolean commitImmediately) {
		dict.put(key, value);
		if(commitImmediately)
			return commitChanges();
		return true;
	}

	@Override
	public String[] getStringArray(String key) {
		Object obj = dict.get(key);
		if(obj instanceof String[])
			return (String[]) obj;
		return null;
	}

	@Override
	public boolean setStringArray(String key, String[] value, boolean commitImmediately) {
		dict.put(key, value);
		if(commitImmediately)
			return commitChanges();
		return true;
	}

	@Override
	public boolean commitChanges() {
		try {
			config.update(dict);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean deleteValue(String key, boolean commitImmediately) {
		boolean result = dict.remove(key) != null;
		if(!result)
			return false;
		if(commitImmediately)
			return commitChanges();
		return true;
	}

}
