package org.smartrplace.util.virtualdevice;

import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.model.sensors.TemperatureSensor;
import org.smartrplace.util.frontend.servlet.UserServlet;

public abstract class SensorDataTemperature extends SensorData {
	TemperatureSensor sensorRes;
	final TemperatureResource setpoint;
	final TemperatureResource feedback;
	//CCUInstance ccu;
	ResourceValueListener<TemperatureResource> setpointListener;
	ResourceValueListener<TemperatureResource> feedbackListener;
	
	public SensorDataTemperature(TemperatureSensor sensorRes, SetpointControlManager<TemperatureResource> ctrl) {
		super(ctrl);
		sensorRes = sensorRes.getLocationResource();
		this.sensorRes = sensorRes;
		this.setpoint = sensorRes.settings().setpoint();
		this.feedback = sensorRes.deviceFeedback().setpoint();
		if(setpoint == null)
			UserServlet.logGeneralReport("Init SensorDataTemperature with setpoint null, parent:"+sensorRes.getLocation(), -36, ctrl.appMan);
		if(feedback == null)
			UserServlet.logGeneralReport("Init SensorDataTemperature with feedback null, parent:"+sensorRes.getLocation(), -37, ctrl.appMan);
		this.setpointListener = new ResourceValueListener<TemperatureResource>() {
			@Override
			public void resourceChanged(TemperatureResource resource) {
				if(ccu() != null) {
					float newVal = setpoint.getValue();
					if((valueFeedbackPending != null) && (valueFeedbackPending != newVal)) {
						//request from different source => we do not check for feedback
						valueFeedbackPending = null;
						reportSetpoint(newVal);
					}
					ctrl.reportSetpointRequest(ccu());
					if(Boolean.getBoolean("org.smartrplace.util.virtualdevice.setpointControlManager.debug"))
						HmSetpCtrlManager.log().trace(" New setpoint TEMPERATURE on "+resource.getLocation());
				}
			}
		};
		this.setpoint.addValueListener(setpointListener, true);

		this.feedbackListener = new ResourceValueListener<TemperatureResource>() {
			@Override
			public void resourceChanged(TemperatureResource resource) {
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
	public TemperatureSensor sensor() {
		return sensorRes;
	}

	@Override
	public TemperatureResource setpoint() {
		return setpoint;
	}

	@Override
	public TemperatureResource feedback() {
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

}
