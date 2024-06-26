package org.smartrplace.util.virtualdevice;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.smartrplace.util.frontend.servlet.UserServlet;
import org.smartrplace.util.virtualdevice.SetpointControlManager.RouterInstance;

/** Generic setpoint-feedback data*/
public class SensorDataFloat2Float extends SensorData {
	RouterInstance ccu = null;

	Resource parentAsSensor;
	final FloatResource setpoint;
	final FloatResource feedback;
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
		if(setpoint == null)
			UserServlet.logGeneralReport("Init SensorDataFloat2Float with setpoint null, parent:"+parentAsSensor.getLocation(), -29, ctrl.appMan);
		if(feedback == null)
			UserServlet.logGeneralReport("Init SensorDataFloat2Float with feedback null, parent:"+parentAsSensor.getLocation(), -31, ctrl.appMan);
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
					if(Boolean.getBoolean("org.smartrplace.util.virtualdevice.setpointControlManager.debug"))
						HmSetpCtrlManager.log().trace(" New setpoint FLOAT on "+resource.getLocation());
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
