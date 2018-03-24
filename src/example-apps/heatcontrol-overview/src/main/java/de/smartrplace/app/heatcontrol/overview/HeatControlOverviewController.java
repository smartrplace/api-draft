package de.smartrplace.app.heatcontrol.overview;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.logging.OgemaLogger;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtPoint;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtPoint.HeatControlExtRoomListener;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtRoomData;

import de.smartrplace.app.heatcontrol.overview.config.HeatcontrolOverviewData;
import de.smartrplace.app.heatcontrol.overview.gui.MainPage;

public class HeatControlOverviewController {
	public static final long MANUAL_UPDATE_INTERVAL = 10*60000;

	public OgemaLogger log;
    public ApplicationManager appMan;

	public final HeatControlOverviewApp serviceAccess;
	public final HeatControlExtPoint extPoint;
	private final HeatControlExtRoomListener roomListener;
	
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
	
	class RoomControlled implements TimerListener {
		private final HeatControlExtRoomData data;
		HeatcontrolOverviewData myData;
		List<TemperatureResource> timerBasedManualSetters = null;
		List<ResourceValueListener<IntegerResource>> hmModeListeners = new ArrayList<>();
		List<ResourceValueListener<IntegerResource>> myModeListeners = new ArrayList<>();
		Timer timer = null;

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
			myData.controlManualMode().addValueListener(l, true);
			
			initThermostats();
		}
		
		private void initThermostats() {
			for(Thermostat th: data.getThermostats()) {
				if(myData.controlManualMode().getValue() < 1) continue;
				TemperatureResource manualControl = MainPage.getActiveManualModeControl(th);
				if(manualControl != null) {
					manualControl.setValue(data.getCurrentTemperatureSetpoint());
				}
				IntegerResource modeFB = MainPage.getActiveModeFeedback(th);
				if(modeFB != null) {
					ResourceValueListener<IntegerResource> l = new ResourceValueListener<IntegerResource>() {

						@Override
						public void resourceChanged(IntegerResource resource) {
							detectedManualEvent();					
						}
					};
					hmModeListeners.add(l);
					modeFB.addValueListener(l, true);
				} else {
					if(timerBasedManualSetters == null) timerBasedManualSetters = new ArrayList<>();
					timerBasedManualSetters.add(manualControl);					
				}
				if(timerBasedManualSetters != null) timer = appMan.createTimer(MANUAL_UPDATE_INTERVAL, this);
			}
		}
		private void closeThermostats() {
			for(Thermostat th: data.getThermostats()) {
				IntegerResource modeFB = MainPage.getActiveModeFeedback(th);
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
			for(Thermostat th: data.getThermostats()) {
				TemperatureResource manualControl = MainPage.getActiveManualModeControl(th);
				if(manualControl == null) continue;
				if(myData.controlManualMode().getValue() < 2) {
					manualControl.setValue(data.getCurrentTemperatureSetpoint());
					continue;
				}
				float curDiff = Math.abs(data.getCurrentTemperatureSetpoint() - myData.comfortTemperature().getValue());
				if(curDiff < 0.2) {
					//switch manual off
					manualControl.setValue(data.getCurrentTemperatureSetpoint());
					resetManualOperation(data);
				} else {
					//switch on
					manualControl.setValue(myData.comfortTemperature().getValue());
					data.setManualTemperatureSetpoint(myData.comfortTemperature().getValue(),data.getAtThermostatManualSettingDuration());
				}
			}
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
				if(old != null) {
					old.close();
					roomsControlled.remove(old);
				}
			}
			RoomControlled r = new RoomControlled(roomData);
			roomsControlled.add(r);
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
}
