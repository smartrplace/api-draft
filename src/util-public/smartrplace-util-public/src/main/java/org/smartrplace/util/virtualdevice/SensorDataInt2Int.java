package org.smartrplace.util.virtualdevice;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.resourcemanager.ResourceValueListener;

/** Generic setpoint-feedback data e.g. used for thermostat controlMode*/
public abstract class SensorDataInt2Int extends SensorData {
	Resource parentAsSensor;
	IntegerResource setpoint;
	IntegerResource feedback;
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
						reportFbConfirmed();
					}
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

}
