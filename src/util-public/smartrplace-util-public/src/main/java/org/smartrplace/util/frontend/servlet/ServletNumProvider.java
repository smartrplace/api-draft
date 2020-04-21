package org.smartrplace.util.frontend.servlet;

import org.ogema.core.channelmanager.measurements.BooleanValue;
import org.ogema.core.channelmanager.measurements.FloatValue;
import org.ogema.core.channelmanager.measurements.IntegerValue;
import org.ogema.core.channelmanager.measurements.Value;

public class ServletNumProvider extends ServletNumProviderBase {
	protected final Float floatVal;
	protected final Integer intVal;
	protected final Boolean booleanVal;

	public ServletNumProvider(float val) {
		floatVal = val;
		intVal = null;
		booleanVal = null;
	}
	public ServletNumProvider(boolean val) {
		floatVal = null;
		intVal = null;
		booleanVal = val;
	}
	public ServletNumProvider(int val) {
		floatVal = null;
		intVal = val;
		booleanVal = null;
	}
		
	public Value getValueInternal(String user, String key) {
		if(floatVal != null)
			return new FloatValue(floatVal);
		else if(intVal != null)
			return new IntegerValue(intVal);
		else if(booleanVal != null)
			return new BooleanValue(booleanVal);
		throw new IllegalStateException("no value defined!");
	}
	@Override
	protected boolean isWritable() {
		return false;
	}
}
