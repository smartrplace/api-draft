package org.ogema.devicefinder.util;

import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.api.AlarmingExtension;
import org.ogema.devicefinder.api.AlarmingExtensionListener;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.tools.resourcemanipulator.timer.CountDownDelayedExecutionTimer;

public abstract class AlarmingExtensionBase implements AlarmingExtension {

	public static class ValueListenerDataBase {
		public ValueListenerDataBase(FloatResource res) {
			this.res = res;
			this.bres = null;
			this.ires = null;
		}
		public ValueListenerDataBase(BooleanResource bres) {
			this.res = null;
			this.bres = bres;
			this.ires = null;
		}
		public ValueListenerDataBase(IntegerResource ires) {
			this.res = null;
			this.bres = null;
			this.ires = ires;
		}
		//public AlarmValueListenerI listener;
		public final FloatResource res;
		public final BooleanResource bres;
		public final IntegerResource ires;
		public CountDownDelayedExecutionTimer timer = null;
		public CountDownDelayedExecutionTimer alarmReleaseTimer = null;
		public long nextTimeAlarmAllowed = -1;
		public boolean isAlarmActive = false;
		public boolean isNoValueAlarmActive = false;
		
		//supervision for last data received
		public long lastTimeOfNewData = -1;
		public long maxIntervalBetweenNewValues;
	}

	protected class AlarmExtListenerBase implements AlarmingExtensionListener {
		public final ValueListenerDataBase valueListener;
		
		public AlarmExtListenerBase(SingleValueResource res, AlarmConfiguration ac) {
			if(ac.sensorVal() instanceof FloatResource)
				valueListener = new ValueListenerDataBase((FloatResource) ac.sensorVal());
			else if(ac.sensorVal() instanceof BooleanResource)
				valueListener = new ValueListenerDataBase((BooleanResource) ac.sensorVal());
			else if(ac.sensorVal() instanceof IntegerResource)
				valueListener = new ValueListenerDataBase((IntegerResource) ac.sensorVal());
			else
				throw new IllegalArgumentException("Only Float-, Integer- and BooleanResource supported!");
		}
		
		@Override
		public <T extends SingleValueResource> AlarmResult resourceChanged(T resource, float value, long now) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
	
	@Override
	public AlarmingExtensionListener getListener(SingleValueResource res, AlarmConfiguration ac) {
		final Long maxTime = (long) Math.max(ac.maxIntervalBetweenNewValues().getValue(),
				ac.maxViolationTimeWithoutAlarm().getValue())*60000;
		return new AlarmExtListenerBase(res, ac);
	}

}
