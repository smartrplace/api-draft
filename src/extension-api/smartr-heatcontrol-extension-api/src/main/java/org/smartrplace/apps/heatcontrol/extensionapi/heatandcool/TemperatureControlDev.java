package org.smartrplace.apps.heatcontrol.extensionapi.heatandcool;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.model.units.TemperatureResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.smartrplace.apps.heatcontrol.extensionapi.KnownValue;

/** Interface to be implemented by resource pattern or other access of single devices*/
public interface TemperatureControlDev extends TemperatureControlBase {
    public ResourcePattern getPattern();
    
    public static class ThermostatPatternExtension {
    	/** keep feedbackValues and setPoint values from last 20 seconds to determine values
    	 * not relevant for manual setting*/
    	public List<KnownValue> knownValues = new ArrayList<>();
    	public float currentSetpointBeforeOffset;
    	public ResourceValueListener<TemperatureResource> feedbackListener = null;
    	public boolean receivedFirstFBValue = false;
    	public Boolean lastBangBangState = null;    	
    }
    
    public ThermostatPatternExtension getExtension();
}
