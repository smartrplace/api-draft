/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.smartrplace.app.heatcontrol.overview;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtPoint;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtPoint.HeatControlExtRoomListener;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtRoomData;
import org.smartrplace.apps.heatcontrol.extensionapi.ThermostatPattern;

import de.smartrplace.app.heatcontrol.overview.config.HeatcontrolOverviewData;
import de.smartrplace.app.heatcontrol.overview.gui.MainPage;

public class HeatControlOverviewController {
	public static final long MANUAL_UPDATE_INTERVAL = 10*60000;
	public static final long MIN_BOOST_INTERVAL = 20*60000;
	public static final long MIN_BOOST_BLOCKER = 60000;
	public static final float MIN_TEMPDIFF = 0.2f;

	public OgemaLogger log;
    public ApplicationManager appMan;

	public final HeatControlOverviewApp serviceAccess;
	public final HeatControlExtPoint extPoint;
	private final RoomListenerOverview roomListener;
	
    public HeatControlOverviewController(ApplicationManager appMan,HeatControlOverviewApp evaluationOCApp) {
		this.appMan = appMan;
		this.log = appMan.getLogger();
		this.serviceAccess = evaluationOCApp;
		this.extPoint = evaluationOCApp.heatExtPoint;
		roomListener = new RoomListenerOverview();
		extPoint.registerRoomListener(roomListener);
	}

    
	public void close() {
		extPoint.unregisterRoomListener(roomListener);
    }
	
	/**TODO: Check this: We expect that all methods in this class are triggered via
	 * the framework smartrplace-heatcontrol thread, so we do not have to care about
	 * synchronization etc.
	 */
	class RoomControlled implements TimerListener {
		private HeatControlExtRoomData data;
		HeatcontrolOverviewData myData;
		List<TemperatureResource> timerBasedManualSetters = null;
		List<ResourceValueListener<IntegerResource>> hmModeListeners = new ArrayList<>();
		List<ResourceValueListener<IntegerResource>> myModeListeners = new ArrayList<>();
		Timer timer = null;
		long lastBoostStarted = -1;

		public RoomControlled(HeatControlExtRoomData data) {
			this.data = data;
			myData = data.getRoomExtensionData(true, HeatcontrolOverviewData.class);
			
			//init data
			if(Float.isNaN(myData.comfortTemperature().getCelsius()) || (myData.comfortTemperature().getCelsius() < MainPage.MIN_COMFORT_TEMP) ||
					(myData.comfortTemperature().getCelsius() > MainPage.MAX_COMFORT_TEMP)) {
				myData.comfortTemperature().create();
				myData.comfortTemperature().setCelsius(MainPage.DEFAULT_COMFORT_TEMP);
			}
			if(!myData.controlManualMode().isActive()) {
				myData.controlManualMode().create();
				myData.controlManualMode().setValue(0);
				myData.controlManualMode().activate(false);
			}
			
			//init listener to my control mode specifying how to react on changes in homematic thermostat
			//control modes
			ResourceValueListener<IntegerResource> l = new ResourceValueListener<IntegerResource>() {

				@Override
				public void resourceChanged(IntegerResource resource) {
					closeThermostats();
					initThermostats();					
				}
			};
			myModeListeners.add(l);
			myData.controlManualMode().addValueListener(l, false);
			
			initThermostats();
		}
		
		private void initThermostats() {
			System.out.println("HeatControlOverview: initThermostats in "+
					ResourceUtils.getHumanReadableName(data.getRoom()));
			for(ThermostatPattern th: data.getThermostats()) {
				System.out.println("HeatControlOverview: initThermostat mode:"+myData.controlManualMode().getValue()+" loc: "+th.model.getLocation());
				if(myData.controlManualMode().getValue() < 1) continue;
				TemperatureResource manualControl = MainPage.getActiveManualModeControl(th.model);
				if(manualControl != null) {
					manualControl.setValue(data.getCurrentTemperatureSetpoint());
				}
				IntegerResource modeFB = MainPage.getActiveModeFeedback(th.model);
				if(modeFB != null) {
					ResourceValueListener<IntegerResource> l = new ResourceValueListener<IntegerResource>() {

						@Override
						public void resourceChanged(IntegerResource resource) {
							if((modeFB.getValue() != 1)&&(modeFB.getValue() != 3)) detectedManualEvent();					
						}
					};
					hmModeListeners.add(l);
					modeFB.addValueListener(l, true);
					System.out.println("HeatControlOverview: Listening for "+modeFB.getLocation()+" in "+
							ResourceUtils.getHumanReadableName(data.getRoom()));
				} else {
					if(timerBasedManualSetters == null) timerBasedManualSetters = new ArrayList<>();
					timerBasedManualSetters.add(manualControl);					
				}
				if(timerBasedManualSetters != null) timer = appMan.createTimer(MANUAL_UPDATE_INTERVAL, this);
			}
		}
		private void closeThermostats() {
			for(ThermostatPattern th: data.getThermostats()) {
				IntegerResource modeFB = MainPage.getActiveModeFeedback(th.model);
				if(modeFB != null) {
					for(ResourceValueListener<IntegerResource> ml: hmModeListeners) modeFB.removeValueListener(ml);
				} 
				if(timer != null) timer.destroy();
			}
		}
		
		void close() {
			closeThermostats();
		}
		
		@Override
		public void timerElapsed(Timer timer) {
			for(TemperatureResource manSet: timerBasedManualSetters) {
				manSet.setValue(data.getCurrentTemperatureSetpoint());
			}
		}
		private void detectedManualEvent() {
			if(appMan.getFrameworkTime() - lastBoostStarted < MIN_BOOST_BLOCKER) {
				System.out.println("HeatControlOverview: Still blocked by boost for "+
					((MIN_BOOST_BLOCKER- (appMan.getFrameworkTime() - lastBoostStarted))/1000)+" seconds.");
				return;
			};
			final boolean lastBoostStartLongAgo = (appMan.getFrameworkTime()-lastBoostStarted > MIN_BOOST_INTERVAL);
			System.out.println("HeatControlOverview: Detected manual event for "+
					ResourceUtils.getHumanReadableName(data.getRoom())+" mode:"+myData.controlManualMode().getValue());
			for(ThermostatPattern th: data.getThermostats()) {
				TemperatureResource manualControl = MainPage.getActiveManualModeControl(th.model);
				if(manualControl == null) continue;
				if(myData.controlManualMode().getValue() < 2) {
					manualControl.setValue(data.getCurrentTemperatureSetpoint());
					continue;
				}
				float curDiff = Math.abs(data.getCurrentTemperatureSetpoint() - myData.comfortTemperature().getValue());
				if((myData.controlManualMode().getValue() == 3) && (curDiff >= MIN_TEMPDIFF) &&
						lastBoostStartLongAgo) {
					//Note: Errors occured when starting boost, but unclear if this was really the problem
					//Suspect that homematic cannot handle to set manual mode and boost mode almost at the same time
					
					if(startBoost(th.model)) {
						continue;
					}
				}
				if(curDiff < MIN_TEMPDIFF) {
					//switch manual off
					manualControl.setValue(data.getCurrentTemperatureSetpoint());
					resetManualOperation(data);
					System.out.println("HeatControlOverview: Switched manuel OFF with setpoint "+data.getCurrentTemperatureSetpoint());
				} else {
					//switch on
					manualControl.setValue(myData.comfortTemperature().getValue());
					data.setManualTemperatureSetpoint(myData.comfortTemperature().getValue(),data.getAtThermostatManualSettingDuration());
					System.out.println("HeatControlOverview: Set to comfort temp:"+myData.comfortTemperature().getValue()+" for "+data.getAtThermostatManualSettingDuration());
				}
			}
		}

		public void updateData(HeatControlExtRoomData roomData) {
			this.data = roomData;		
		}

		public void startBoost() {
			if(appMan.getFrameworkTime() - lastBoostStarted < MIN_BOOST_BLOCKER) {
				System.out.println("HeatControlOverview: Still blocked by boost for "+
					((MIN_BOOST_BLOCKER- (appMan.getFrameworkTime() - lastBoostStarted))/1000)+" seconds.");
				return;
			}
			for(ThermostatPattern th: data.getThermostats()) {
				startBoost(th.model);
			}
		}
		private boolean startBoost(Thermostat th) {
			BooleanResource boostControl = MainPage.getBoostControl(th);
			if(boostControl != null) {
				boostControl.setValue(true);
				lastBoostStarted = appMan.getFrameworkTime();
				System.out.println("HeatControlOverview: Started boost");
				return true;
			}
			return false;
		}
	}
	private void resetManualOperation(HeatControlExtRoomData data) {
		data.setManualTemperatureSetpoint(0,-1);			
	}
	class RoomListenerOverview implements HeatControlExtRoomListener {
		public List<RoomControlled> roomsControlled = new ArrayList<>();
		public RoomControlled getRoomData(Room room) {
			for(RoomControlled r: roomsControlled) {
				if(r.data.getRoom().equalsLocation(room)) return r;
			}
			return null;
		}
		
		@Override
		public void roomAvailable(HeatControlExtRoomData roomData, CallbackReason reason) {
			if(reason == CallbackReason.UPDATE) {
				RoomControlled old = getRoomData(roomData.getRoom());
				old.updateData(roomData);
				old.initThermostats();
				//if(old != null) {
				//	old.close();
				//	roomsControlled.remove(old);
				//}
			} else {
				RoomControlled r = new RoomControlled(roomData);
				roomsControlled.add(r);
			}
		}

		@Override
		public void roomUnavailable(HeatControlExtRoomData roomData, CallbackReason reason) {
			RoomControlled old = getRoomData(roomData.getRoom());
			if(old != null) {
				//TODO: Only perform this operation if the room is currently controlled by this extension app
				resetManualOperation(roomData);
				old.close();
				roomsControlled.remove(old);
			}
		}
		
	}
	public void startBoost(HeatControlExtRoomData object) {
		RoomControlled rc = roomListener.getRoomData(object.getRoom());
		rc.startBoost();
	}
}
