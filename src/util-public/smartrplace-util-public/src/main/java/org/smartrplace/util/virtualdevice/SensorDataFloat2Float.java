package org.smartrplace.util.virtualdevice;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.smartrplace.util.virtualdevice.SetpointControlManager.RouterInstance;

/** Generic setpoint-feedback data e.g. used for thermostat controlMode*/
public class SensorDataFloat2Float extends SensorData {
	RouterInstance ccu = null;

	Resource parentAsSensor;
	FloatResource setpoint;
	FloatResource feedback;
	//CCUInstance ccu;
	ResourceValueListener<FloatResource> setpointListener;
	ResourceValueListener<FloatResource> feedbackListener;
	
	public SensorDataFloat2Float(Resource parentAsSensor, FloatResource setpoint,
			SetpointControlManager<FloatResource> ctrl) {
		this(parentAsSensor, setpoint, parentAsSensor.getSubResource("stateFeedback", FloatResource.class), ctrl);
	}
	public SensorDataFloat2Float(Resource parentAsSensor, FloatResource setpoint,
				FloatResource feedback, SetpointControlManager<FloatResource> ctrl) {
		super(ctrl);
		parentAsSensor = parentAsSensor.getLocationResource();
		this.parentAsSensor = parentAsSensor;
		this.setpoint = setpoint;
		this.feedback = feedback;
		this.setpointListener = new ResourceValueListener<FloatResource>() {
			@Override
			public void resourceChanged(FloatResource resource) {
				if(ccu() != null) {
					Float newVal = setpoint.getValue();
					if((valueFeedbackPending != null) && ((valueFeedbackPending) != newVal)) {
						//request from different source => we do not check for feedback
						valueFeedbackPending = null;
					}
					ctrl.reportSetpointRequest(ccu());
				}
			}
		};
		this.setpoint.addValueListener(setpointListener, true);

		this.feedbackListener = new ResourceValueListener<FloatResource>() {
			@Override
			public void resourceChanged(FloatResource resource) {
				if(valueFeedbackPending != null) {
					float valFb = feedback.getValue();
					if(valFb == valueFeedbackPending) {
						valueFeedbackPending = null;
						reportFbConfirmed(valFb);
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
	public FloatResource setpoint() {
		return setpoint;
	}

	@Override
	public FloatResource feedback() {
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
		setpoint.setValue(value);
	}

	@Override
	public RouterInstance ccu() {
		if(ccu == null) {
			ccu = ctrl.getCCU(parentAsSensor);
			//if(ccu == null) {
			//	System.out.println("WARNING: No Default Router found for setpoint sensor:"+parentAsSensor.getLocation());
			//}
		}
		return ccu;
	}

	/*public boolean isValueFullySet(int value) {
		if(setpoint.getValue() != value)
			return false;
		if(feedback.getValue() != value)
			return false;
		return true;
	}*/
}
