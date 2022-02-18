package org.smartrplace.tissue.util.logconfig;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.StringResource;
import org.ogema.model.alignedinterval.TimeIntervalLength;
import org.ogema.model.gateway.remotesupervision.DataLogTransferInfo;
import org.ogema.model.gateway.remotesupervision.GatewayTransferInfo;

public class LogTransferUtil {
	public static ResourceList<DataLogTransferInfo> getDataLogTransferInfo(ApplicationManager appMan) {
    	List<GatewayTransferInfo> gwtis = appMan.getResourceAccess().getResources(GatewayTransferInfo.class);
    	//This app will present the first GatewayTransferInfo in the UI.
		
    	ResourceList<DataLogTransferInfo> dataLogs;
    	if(gwtis.size() > 1) appMan.getLogger().error("More than one element of type DataLogTransferInfo on system, just using first element!");
    	if(gwtis.isEmpty())  dataLogs = null;
    	else { 
    		GatewayTransferInfo gwti = gwtis.get(0);
    		dataLogs = gwti.getSubResource("dataLogs", ResourceList.class).create();
    		dataLogs.setElementType(DataLogTransferInfo.class);
    	}
		return dataLogs;
	}
	
	static boolean warningDone = false;
	public static void startTransmitLogData(SingleValueResource resource,
			ResourceList<DataLogTransferInfo> dataLogs) {
		DataLogTransferInfo log = null;
		if(dataLogs == null) {
			if(!warningDone) {
				System.out.println("No DataLogTransferInfo available! Skipping transfer...");
				warningDone = true;
			}
			return;
		}
		for(DataLogTransferInfo dl : dataLogs.getAllElements()) {
			if(dl.clientLocation().getValue().equals(resource.getPath()))
				log = dl;
		}

		if(log == null) log = dataLogs.add();
		
		StringResource clientLocation = log.clientLocation().create();
		clientLocation.setValue(resource.getPath());
	
		TimeIntervalLength tLength = log.transferInterval().timeIntervalLength().create();
		IntegerResource type = tLength.type().create();
		type.setValue(10);
		log.activate(true);
	}
	
	public static void stopTransmitLogData(SingleValueResource resource,
			ResourceList<DataLogTransferInfo> dataLogs) {
		DataLogTransferInfo log = null;
		for(DataLogTransferInfo dl : dataLogs.getAllElements()) {
			if(dl.clientLocation().getValue().equals(resource.getPath()))
				log = dl;
		}
		if(log == null) return;
		log.delete();
	}
	
	public static boolean isResourceTransferred(SingleValueResource resource,
			ResourceList<DataLogTransferInfo> dataLogs) {
		if(dataLogs == null) return false;
		DataLogTransferInfo log = null;
		for(DataLogTransferInfo dl : dataLogs.getAllElements()) {
			if(dl.clientLocation().getValue().equals(resource.getPath()))
				log = dl;
		}
		
		return(log != null && log.isActive());
	}
	

}
