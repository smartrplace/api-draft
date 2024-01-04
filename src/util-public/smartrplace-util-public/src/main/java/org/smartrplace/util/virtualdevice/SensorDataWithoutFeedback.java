package org.smartrplace.util.virtualdevice;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.tools.resource.util.ValueResourceUtils;
import org.smartrplace.util.frontend.servlet.UserServlet;

/** SensorData that just supports check if writing needs to be blocked due to overload*/
public abstract class SensorDataWithoutFeedback extends SensorData {
	final ValueResource setpoint;
	
	public SensorDataWithoutFeedback(ValueResource setpoint, SetpointControlManager<IntegerResource> ctrl) {
		super(ctrl);
		this.setpoint = setpoint;
		if(setpoint == null)
			UserServlet.logGeneralReport("Init SensorDataWithoutFeedback with setpoint null", -38, ctrl.appMan);

		//No feedback check
		valueFeedbackPending = null;
		ccu();
	}

	@Override
	public Resource sensor() {
		return setpoint;
	}

	@Override
	public ValueResource setpoint() {
		return setpoint;
	}

	@Override
	public ValueResource feedback() {
		return setpoint;
		//throw new IllegalStateException("Feedback not supported!");
	}

	@Override
	public ResourceValueListener<?> setpointListener() {
		return null;
	}

	@Override
	public void stop() {
	}

	@Override
	public void writeSetpoint(float value) {
		if(setpoint instanceof SingleValueResource)
			ValueResourceUtils.setValue((SingleValueResource)setpoint, value);
		else
			throw new IllegalArgumentException("Do not write single float into ArrayResource or Schedule! ValueResoource:"+setpoint.getLocation());
	}
	@Override
	void writeSetpointData(Object setpointData) {
		ValueResourceUtils.setValue(setpoint, setpointData);
	}

}
