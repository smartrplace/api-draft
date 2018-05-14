package org.smartrplace.remotesupervision.transfer.util;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.model.gateway.remotesupervision.ScheduleTransferInfo;
import org.ogema.model.gateway.remotesupervision.SingleValueTransferInfo;
import org.ogema.model.gateway.remotesupervision.ValueTransferInfo;

import de.iwes.util.resource.ValueResourceHelper;

public class EvalSetup {
	public static SingleValueTransferInfo getTransferData(String location, ResourceList<ValueTransferInfo> valueData) {
		for(ValueTransferInfo svti: valueData.getAllElements()) {
			if(svti.clientLocation().getValue().equals(location) && (svti instanceof SingleValueTransferInfo)) return (SingleValueTransferInfo)svti;
		}
		return null;
	}
	public static ScheduleTransferInfo getScheduleTransferData(String location, ResourceList<ValueTransferInfo> valueData) {
		for(ValueTransferInfo svti: valueData.getAllElements()) {
			if(svti.clientLocation().getValue().equals(location) && (svti instanceof ScheduleTransferInfo)) return (ScheduleTransferInfo)svti;
		}
		return null;
	}
	
	//TODO: Do references work here? Differing implementation for schedule and single value!
	public static SingleValueTransferInfo addOrCreateValueTransfer(FloatResource source, ResourceList<ValueTransferInfo> valueData,
			String name) {
		SingleValueTransferInfo svti = EvalSetup.addOrCreateValueTransfer(
				source.getLocation(), valueData, name);
		svti.value().setAsReference(source);
		return svti;
	}
	public static SingleValueTransferInfo addOrCreateValueTransfer(String locationId, ResourceList<ValueTransferInfo> valueData,
				String name) {
		SingleValueTransferInfo svti = EvalSetup.getTransferData(locationId, valueData);
		if(svti == null) {
			svti = valueData.add(SingleValueTransferInfo.class);
			ValueResourceHelper.setIfNew(svti.clientLocation(), locationId);
			if(name != null)
				ValueResourceHelper.setIfNew(svti.name(), name);
			svti.activate(true);
		}
		return svti;
	}
	
	public static ScheduleTransferInfo addOrCreateScheduleTransfer(String sourceId, ResourceList<ValueTransferInfo> valueData,
			String name) {
		ScheduleTransferInfo svti = EvalSetup.getScheduleTransferData(sourceId, valueData);
		if(svti == null) {
			svti = valueData.add(ScheduleTransferInfo.class);
			svti.scheduleHolder().historicalData().create();
			ValueResourceHelper.setIfNew(svti.clientLocation(), sourceId);
			if(name != null)
				ValueResourceHelper.setIfNew(svti.name(), name);
			svti.activate(true);
		}
		return svti;
	}
	
	public static int getMode(String id) {	
		char c = id.charAt(0);
		int mode = 4;
		if((c >= '0') && (c <= '9')) mode = c-'0';
		return mode;
	}
	
    /*@Deprecated
    //Use LogHelper.addSchedToView instead
    public static TimeSeriesPresentationData addSchedToView(ResourceList<TimeSeriesPresentationData> tlist,
    		SingleValueResource fres, String label, GatewayTransferInfo remoteTransfer) {
    	TimeSeriesPresentationData result = null;
    	for(TimeSeriesPresentationData tsp: tlist.getAllElements()) {
    		if(tsp.scheduleLocation().getValue().equals(fres.getLocation())) {
    			if(!label.equals(tsp.name().getValue())) {
    				tsp.name().<StringResource>create().setValue(label);
    				tsp.name().activate(false);
    			}
    			result = tsp;
    		}
    	}
    	if(result == null) result = addSchedToViewForce(tlist, fres, label);
    	if(remoteTransfer != null) {
    		DataLogTransferInfo dltfound = null;
        	for(DataLogTransferInfo dlt: remoteTransfer.dataLogs().getAllElements()) {
        		if(dlt.clientLocation().getValue().equals(fres.getLocation())) {
        			dltfound = dlt;
           			if(!label.equals(dlt.name().getValue())) {
        				dlt.name().<StringResource>create().setValue(label);
        				dlt.name().activate(false);
        			}
        		}
        	}
        	if(dltfound == null) addDataLogRemoteTransferForce(fres, label, remoteTransfer);
    	}
    	return result;
    }
    @Deprecated
    //Use LogHelper.addSchedToViewForce instead
    public static TimeSeriesPresentationData addSchedToViewForce(ResourceList<TimeSeriesPresentationData> tlist,
        		SingleValueResource fres, String label) {
    	TimeSeriesPresentationData tsp = tlist.add();
    	tsp.scheduleLocation().<StringResource>create().setValue(fres.getLocation());
    	tsp.name().<StringResource>create().setValue(label);
    	return tsp;
    }
    @Deprecated
    //Use LogHelper.addDataLogRemoteTransferForce instead
    public static DataLogTransferInfo addDataLogRemoteTransferForce(
    		SingleValueResource fres, String label, GatewayTransferInfo remoteTransfer) {    	
    	// FIXME still required?
    	if(remoteTransfer != null) {
	    	DataLogTransferInfo remoteTS = remoteTransfer.dataLogs().add();
	    	remoteTS.clientLocation().<StringResource>create().setValue(fres.getLocation());
	    	remoteTS.transferInterval().timeIntervalLength().type().<IntegerResource>create().setValue(AbsoluteTiming.DAY);
	    	remoteTS.name().<StringResource>create().setValue(label);
	    	remoteTS.activate(true);
	    	return remoteTS;
    	}

    	return null;
    }
    @Deprecated
    //Use LogHelper.getHmDeviceId instead    
    public static String getHmDeviceId(ResourcePattern<?> homeMaticPattern) {
		Resource hmParent = ResourceHelper.getFirstParentOfType(homeMaticPattern.model, "HmDevice");
    	String name = hmParent.getName();
		String deviceId = name.substring(name.length()-4);
		return deviceId;
    }
 */
	
	@Deprecated
	public static void cleanUpSEMA(ApplicationManager appMan) {
		deleteScheduleTransferInfo("SEMA Heating Tariff", appMan);
	}
	public static void deleteScheduleTransferInfo(String id, ApplicationManager appMan) {
		List<ScheduleTransferInfo> stlist = appMan.getResourceAccess().getResources(ScheduleTransferInfo.class);
		for(ScheduleTransferInfo svti: stlist) {
			if(svti.name().getValue().equals(id)) {
				appMan.getLogger().warn("Deleting "+svti.getLocation());			}
				svti.delete();
		}
	}

}
