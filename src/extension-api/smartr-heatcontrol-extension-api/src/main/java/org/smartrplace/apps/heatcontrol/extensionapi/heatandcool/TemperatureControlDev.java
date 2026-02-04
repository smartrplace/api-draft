package org.smartrplace.apps.heatcontrol.extensionapi.heatandcool;

import java.util.ArrayList;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.smartrplace.apps.heatcontrol.extensionapi.KnownValue;
import org.smartrplace.util.virtualdevice.SetpointControlManager;

import de.iwes.util.logconfig.CountdownTimerMulti2Single;

/** Interface to be implemented by resource pattern or other access of single devices*/
public interface TemperatureControlDev extends TemperatureControlBase {
    @SuppressWarnings("rawtypes")
	public ResourcePattern getPattern();
    
    public static class ThermostatPatternExtension {
    	/** keep feedbackValues and setPoint values from last 20 seconds to determine values
    	 * not relevant for manual setting*/
    	public List<KnownValue> knownValues = new ArrayList<>();
    	public float currentSetpointBeforeOffset;
    	public ResourceValueListener<TemperatureResource> feedbackListener = null;
    	public boolean receivedFirstFBValue = false;
    	
    	public final CountdownTimerMulti2Single manualRetardTimer;
    	//public CountDownDelayedExecutionTimer manualRetardTimer;

    	/** When auto-mode is activated we do not accept manual values for some time*/
    	public long blockedForRemoteManualUntil = -1;
    	public boolean requestedManuMode = false;
    	public float prevManualSetpoint = -1;
    	public long blockedForOnThermostatManuModeSwitchUntil = -1;
    	public IntegerResource controlMode = null;
		public IntegerResource controlModeFeedback = null;
    	
    	public LogicProviderTP logicProvider = null;
		public ThermostatPatternExtension(ApplicationManager appMan, TimeResource manualRetard) {
			if(appMan == null) {
				this.manualRetardTimer = null;
				return;
			}
			this.manualRetardTimer = new CountdownTimerMulti2Single(appMan, -1) {

				@Override
				public void delayedExecution() {
					logicProvider.setRemoteManualSetting();
				}
				
				@Override
				protected long getVariableTimerDuration() {
					return manualRetard.getValue();
				}
			};
		}
    }
    
    public ThermostatPatternExtension getExtension();
    
	public SetpointControlManager<TemperatureResource> getSetpMan();
	public SetpointControlManager<TemperatureResource> getSetpMan(ApplicationManagerPlus appManPlus);
}
