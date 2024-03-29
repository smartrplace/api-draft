package org.smartrplace.util.virtualdevice;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.smartrplace.util.frontend.servlet.UserServlet;

/** Generic setpoint-feedback data e.g. used for thermostat controlMode*/
public abstract class SensorDataInt2Int extends SensorData {
	Resource parentAsSensor;
	final IntegerResource setpoint;
	final IntegerResource feedback;
	//CCUInstance ccu;
	ResourceValueListener<IntegerResource> setpointListener;
	ResourceValueListener<IntegerResource> feedbackListener;
	
	public SensorDataInt2Int(Resource parentAsSensor, IntegerResource setpoint,
				IntegerResource feedback, SetpointControlManager<IntegerResource> ctrl) {
		super(ctrl);
		parentAsSensor = parentAsSensor.getLocationResource();
		this.parentAsSensor = parentAsSensor;
		this.setpoint = setpoint;
		this.feedback = feedback;
		if(setpoint == null)
			UserServlet.logGeneralReport("Init SensorDataInt2Int with setpoint null, parent:"+parentAsSensor.getLocation(), -32, ctrl.appMan);
		if(feedback == null)
			UserServlet.logGeneralReport("Init SensorDataInt2Int with feedback null, parent:"+parentAsSensor.getLocation(), -33, ctrl.appMan);
		this.setpointListener = new ResourceValueListener<IntegerResource>() {
			@Override
			public void resourceChanged(IntegerResource resource) {
				if(ccu() != null) {
					int newVal = setpoint.getValue();
					if((valueFeedbackPending != null) && (valueFeedbackPending != newVal)) {
						//request from different source => we do not check for feedback
						valueFeedbackPending = null;
					}
					ctrl.reportSetpointRequest(ccu());
					if(Boolean.getBoolean("org.smartrplace.util.virtualdevice.setpointControlManager.debug"))
						HmSetpCtrlManager.log().trace(" New setpoint INT on "+resource.getLocation());
				}
			}
		};
		this.setpoint.addValueListener(setpointListener, true);

		this.feedbackListener = new ResourceValueListener<IntegerResource>() {
			@Override
			public void resourceChanged(IntegerResource resource) {
				if(valueFeedbackPending != null) {
					float valFb = feedback.getValue();
					if(valFb == valueFeedbackPending) {
						valueFeedbackPending = null;
						reportFbConfirmed(valFb);
					}
				} else {
					float valFb = feedback.getValue();
					reportFbConfirmed(valFb, false);
				}
			}
		};
		this.feedback.addValueListener(feedbackListener, true);
		ccu();
	}

	@Override
	public Resource sensor() {
		return parentAsSensor;
	}

	@Override
	public IntegerResource setpoint() {
		return setpoint;
	}

	@Override
	public IntegerResource feedback() {
		return feedback;
	}

	@Override
	public ResourceValueListener<?> setpointListener() {
		return setpointListener;
	}

	@Override
	public void stop() {
		setpointListener = null;
	}

	@Override
	public void writeSetpoint(float value) {
		setpoint.setValue((int)value);
	}

	public boolean isValueFullySet(int value) {
		if(setpoint.getValue() != value)
			return false;
		if(feedback.getValue() != value)
			return false;
		return true;
	}
}
