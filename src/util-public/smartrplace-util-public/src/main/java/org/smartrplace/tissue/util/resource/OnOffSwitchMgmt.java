package org.smartrplace.tissue.util.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.units.PowerResource;
import org.ogema.devicefinder.util.DpGroupUtil;
import org.ogema.model.actors.OnOffSwitch;
import org.ogema.model.locations.Room;
import org.ogema.model.sensors.PowerSensor;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.util.format.StringFormatHelper;

public class OnOffSwitchMgmt {
	public static final long CACHING_TIME = 10000;
	
	public static class OnOffSwitchData {
		public OnOffSwitch swtch;
		public BooleanResource stateControl;
		public BooleanResource stateFeedback;
		public PowerResource powerSensor;
		
		public Resource parent;
		
		public String getLocation() {
			return swtch.getLocation();
		}
		
		public InstallAppDevice getIAD(ApplicationManager appMan) {
			InstallAppDevice result = DpGroupUtil.getInstallAppDeviceForSubCashed(swtch, appMan);
			if(result != null)
				return result;
			if(parent != null)
				return DpGroupUtil.getInstallAppDeviceForSubCashed(parent, appMan);
			return null;
		}
	}
	
	/** Overwrite to sort out some OnOffSwitches*/
	protected boolean toBeUsed(OnOffSwitch onOff) {
		return true;
	};
	
	protected final boolean maintainListIfNumberOfSwitchesUnchanged;
	
	protected final ApplicationManager appMan;
	protected final List<String> normallyExcluded;
	protected final List<String> normallyExcludedButInclusionForced;
	
	/** By default all OnOffSwitches that are somehow assigned to the room are included, but 
	 * this is determined via {@link #toBeUsed(OnOffSwitch)}. Additional exclusions and inclusions
	 * can be defined via the String-based lists 
	 * @param appMan
	 * @param normallyExcludedFromListAsString exclude even if toBeUsed says something different
	 * @param normallyExcludedButInclusionForcedFromListAsString if a String in this list is part of the
	 * 		location of an OnOffSwitch then it will be included for sure no matter what the other indicators say
	 * 		(if it is assigned to the room requested)
	 */
	public OnOffSwitchMgmt(ApplicationManager appMan, String normallyExcludedFromListAsString,
			String normallyExcludedButInclusionForcedFromListAsString,
			boolean maintainListIfNumberOfSwitchesUnchanged) {
		this.appMan = appMan;
		normallyExcluded = StringFormatHelper.getListFromString(normallyExcludedFromListAsString);
		normallyExcludedButInclusionForced = StringFormatHelper.getListFromString(normallyExcludedButInclusionForcedFromListAsString);
		this.maintainListIfNumberOfSwitchesUnchanged = maintainListIfNumberOfSwitchesUnchanged;
	}

	protected class CachedResult {
		long created = -1;
		List<OnOffSwitchData> data;
		
		/** All OnOffSwitches in the room, not sorted out*/
		List<OnOffSwitch> dataRawAll;
	}
	Map<String, CachedResult> cashedResults = new HashMap<>();
	
	public List<OnOffSwitchData> getAllSwitches(Room room, boolean forceUpdate) {
		CachedResult cache = cashedResults.get(room.getLocation());
		if(cache == null) {
			cache = new CachedResult();
			cashedResults.put(room.getLocation(), cache);
		}
		long now = appMan.getFrameworkTime();
		if((!forceUpdate) && (now - cache.created <= CACHING_TIME))
			return cache.data;
		List<OnOffSwitch> onOffs = ResourceUtils.getDevicesFromRoom(appMan.getResourceAccess(), OnOffSwitch.class, room);
		if(maintainListIfNumberOfSwitchesUnchanged && cache.dataRawAll != null && cache.dataRawAll.size() == onOffs.size())
			return cache.data;
		cache.dataRawAll = onOffs;
		
		List<OnOffSwitch> toUse = new ArrayList<>();
		for(OnOffSwitch onOff: onOffs) {
			if(isMatchedByList(onOff.getLocation(), normallyExcludedButInclusionForced))
				toUse.add(onOff);
			if(isMatchedByList(onOff.getLocation(), normallyExcluded))
				continue;
			if(toBeUsed(onOff))
				toUse.add(onOff);
		}
		
		List<OnOffSwitchData> result = new ArrayList<>();
		for(OnOffSwitch onOff: toUse) {
			OnOffSwitchData el = new OnOffSwitchData();
			el.swtch = onOff;
			el.stateControl = onOff.stateControl();
			el.stateFeedback = onOff.stateFeedback();
			el.parent = onOff.getParent();
			if(el.parent != null) {
				List<PowerSensor> sens = el.parent.getSubResources(PowerSensor.class, false);
				if(!sens.isEmpty())
					el.powerSensor = sens.get(0).reading();
			}
			result.add(el);
		}
		cache.data = result;
		cache.created = now;
		return result;
	}
	
	protected boolean isMatchedByList(String location, List<String> pats) {
		for(String pat: pats) {
			if(location.contains(pat))
				return true;
		}
		return false;
	}
}
