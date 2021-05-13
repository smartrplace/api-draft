package org.smartrplace.util.virtualdevice;

import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.model.sensors.TemperatureSensor;
import org.smartrplace.util.virtualdevice.SetpointControlManager.SensorData;

public abstract class SensorDataTemperature extends SensorData {
	TemperatureSensor sensor;
	TemperatureResource setpoint;
	TemperatureResource feedback;
	//CCUInstance ccu;
	ResourceValueListener<TemperatureResource> setpointListener;
	ResourceValueListener<TemperatureResource> feedbackListener;
	
	public SensorDataTemperature(TemperatureSensor sensor, SetpointControlManager<TemperatureResource> ctrl) {
		sensor = sensor.getLocationResource();
		this.sensor = sensor;
		this.setpoint = sensor.settings().setpoint();
		this.feedback = sensor.deviceFeedback().setpoint();
		this.setpointListener = new ResourceValueListener<TemperatureResource>() {
			@Override
			public void resourceChanged(TemperatureResource resource) {
				if(ccu() != null) {
					float newVal = setpoint.getValue();
					if((valueFeedbackPending != null) && (valueFeedbackPending != newVal)) {
						//request from different source => we do not check for feedback
						valueFeedbackPending = null;
					}
					ctrl.reportSetpointRequest(ccu());
				}
			}
		};
		this.setpoint.addValueListener(setpointListener, true);

		this.feedbackListener = new ResourceValueListener<TemperatureResource>() {
			@Override
			public void resourceChanged(TemperatureResource resource) {
				if(valueFeedbackPending != null) {
					float valFb = feedback.getValue();
					if(valFb == valueFeedbackPending)
						valueFeedbackPending = null;
				}
			}
		};
		this.feedback.addValueListener(feedbackListener, true);
	}

	@Override
	public TemperatureSensor sensor() {
		return sensor;
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
