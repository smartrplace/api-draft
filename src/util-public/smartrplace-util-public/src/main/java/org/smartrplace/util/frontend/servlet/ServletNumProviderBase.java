package org.smartrplace.util.frontend.servlet;

import java.util.HashMap;

import org.ogema.core.channelmanager.measurements.ObjectValue;
import org.ogema.core.channelmanager.measurements.StringValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

public abstract class ServletNumProviderBase implements ServletValueProvider {
	protected abstract Value getValueInternal(String user, String key);
	protected abstract boolean isWritable();
	
	public UserServletParamData pdata = null;
	
	@Override
	public Value getValue(String user, String key) {
		if(pdata != null && pdata.provideExtended) {
			MultiValue mval = new MultiValue();
			mval.mainValue = getValueInternal(user, key);
			mval.permissions = new HashMap<>();
			mval.permissions.put("UserWritePermission", pdata.hasWritePermission);
			UserServletUtil.addTimeSeriesData(pdata, mval);

			//This provider does not offer POST
			mval.isWritable = isWritable();
			
			ObjectValue result = new ObjectValue(mval);
			return result;
		} else
			return getValueInternal(user, key);
	}
	
}
