package org.smartrplace.util.frontend.servlet;

import org.ogema.core.channelmanager.measurements.BooleanValue;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.IntegerValue;
import org.ogema.core.channelmanager.measurements.Value;
import org.smartrplace.util.frontend.servlet.UserServlet.ServletValueProvider;

public class ServletNumProvider implements ServletValueProvider {
	protected final Float floatVal;
	protected final Integer intVal;
	protected final Boolean booleanVal;
	
	public ServletNumProvider(float val) {
		floatVal = val;
		intVal = null;
		booleanVal = null;
	}
		
	@Override
	public Value getValue(String user, String key) {
		if(floatVal != null)
			return new FloatValue(floatVal);
		else if(intVal != null)
			return new IntegerValue(intVal);
		else if(booleanVal != null)
			return new BooleanValue(booleanVal);
		throw new IllegalStateException("no value defined!");
	}
}
