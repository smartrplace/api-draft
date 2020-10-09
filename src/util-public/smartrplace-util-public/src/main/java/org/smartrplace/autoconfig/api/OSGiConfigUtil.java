package org.smartrplace.autoconfig.api;

public class OSGiConfigUtil {
	public static OSGiConfigurationValue setIfNew(float value, String pid, String variableName, OSGiConfigAccessService service,
			boolean exceptionIfNotFound) {
		OSGiConfiguration config = service.getConfiguration(pid);
		if(config == null) {
			if(exceptionIfNotFound)
				throw new IllegalArgumentException("Pid is not available: "+pid);
			else
				return null;
		}
		OSGiConfigurationValue valRes = config.getValue(variableName);
		if(valRes == null) {
			if(exceptionIfNotFound)
				throw new IllegalArgumentException("Value is not available: Pid:"+pid+" Variable:"+variableName);
			else
				return null;
		}
		
		Float currentVal = valRes.getValue();
		Float defaultVal = valRes.getDefaultValue();
		if(currentVal == null || (!currentVal.equals(defaultVal))) {
			valRes.setValue(value);
		}
		return valRes;
	}

	public static OSGiConfigurationValue setIfNew(String value, String pid, String variableName,
			OSGiConfigAccessService service, boolean exceptionIfNotFound, OneTimeConfigStep otc1) {
		if(!otc1.performConfig(pid+"##"+variableName))
			return null;
		OSGiConfiguration config = service.getConfiguration(pid);
		if(config == null) {
			if(exceptionIfNotFound)
				throw new IllegalArgumentException("Pid is not available: "+pid);
			else
				return null;
		}
		OSGiConfigurationValue valRes = config.getValue(variableName);
		if(valRes == null) {
			if(exceptionIfNotFound)
				throw new IllegalArgumentException("Value is not available: Pid:"+pid+" Variable:"+variableName);
			else
				return null;
		}
		
		Float currentVal = valRes.getValue();
		Float defaultVal = valRes.getDefaultValue();
		if(currentVal == null || (!currentVal.equals(defaultVal))) {
			valRes.setValue(value);
		}
		return valRes;
		
	}
}
