package org.smartrplace.util.virtualdevice;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.smartrplace.util.frontend.servlet.UserServlet;
import org.smartrplace.util.virtualdevice.SetpointControlManager.RouterInstance;

/** Generic setpoint-feedback data e.g. used for switch on/off setpoint*/
public class SensorDataBool2Bool extends SensorData {
	RouterInstance ccu = null;

	Resource parentAsSensor;
	final BooleanResource setpoint;
	final BooleanResource feedback;
	//CCUInstance ccu;
	ResourceValueListener<BooleanResource> setpointListener;
	ResourceValueListener<BooleanResource> feedbackListener;
	
	public SensorDataBool2Bool(Resource parentAsSensor, BooleanResource setpoint,
			SetpointControlManager<BooleanResource> ctrl) {
		this(parentAsSensor, setpoint, parentAsSensor.getSubResource("stateFeedback", BooleanResource.class), ctrl);
	}
	public SensorDataBool2Bool(Resource parentAsSensor, BooleanResource setpoint,
				BooleanResource feedback, SetpointControlManager<BooleanResource> ctrl) {
		super(ctrl);
		parentAsSensor = parentAsSensor.getLocationResource();
		this.parentAsSensor = parentAsSensor;
		this.setpoint = setpoint;
		this.feedback = feedback;
		if(setpoint == null)
			UserServlet.logGeneralReport("Init SensorDataBool2Bool with setpoint null, parent:"+parentAsSensor.getLocation(), -34, ctrl.appMan);
		if(feedback == null)
			UserServlet.logGeneralReport("Init SensorDataBool2Bool with feedback null, parent:"+parentAsSensor.getLocation(), -35, ctrl.appMan);
		this.setpointListener = new ResourceValueListener<BooleanResource>() {
			@Override
			public void resourceChanged(BooleanResource resource) {
				if(ccu() != null) {
					boolean newVal = setpoint.getValue();
					if((valueFeedbackPending != null) && ((valueFeedbackPending > 0.5f) != newVal)) {
						//request from different source => we do not check for feedback
						valueFeedbackPending = null;
					}
					ctrl.reportSetpointRequest(ccu());
if(Boolean.getBoolean("org.smartrplace.util.virtualdevice.setpointControlManager.debug"))
	HmSetpCtrlManager.log().trace(" New setpoint BOOL on "+resource.getLocation());
				}
			}
		};
		this.setpoint.addValueListener(setpointListener, true);

		this.feedbackListener = new ResourceValueListener<BooleanResource>() {
			@Override
			public void resourceChanged(BooleanResource resource) {
				if(valueFeedbackPending != null) {
					float valFb = feedback.getValue()?1.0f:0.0f;
					if(valFb == valueFeedbackPending) {
						valueFeedbackPending = null;
						reportFbConfirmed(valFb);
					}
				} else {
					float valFb = feedback.getValue()?1.0f:0.0f;
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
	public BooleanResource setpoint() {
		return setpoint;
	}

	@Override
	public BooleanResource feedback() {
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
		setpoint.setValue(value > 0.5f);
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
