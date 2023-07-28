package org.smartrplace.tissue.util.logconfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
	
	final static Map<ResourceList<DataLogTransferInfo>, Map<String, DataLogTransferInfo>> dltis = new ConcurrentHashMap<>();
	final static AtomicInteger callcounter = new AtomicInteger();
	
	static Map<String, DataLogTransferInfo> createDltiMap(ResourceList<DataLogTransferInfo> l) {
		Map<String, DataLogTransferInfo> m = new ConcurrentHashMap<>();
			for(DataLogTransferInfo dlti : l.getAllElements()) {
				m.put(dlti.clientLocation().getValue(), dlti);
		}
		return m;
	}
	
	static DataLogTransferInfo findTransferInfoInList(ResourceList<DataLogTransferInfo> l, String targetLoc) {
		return l.getAllElements().stream()
				.filter(dlti -> targetLoc.equals(dlti.clientLocation().getValue()))
				.findFirst().orElse(null);
	}
	
	static DataLogTransferInfo findTransferInfo(ResourceList<DataLogTransferInfo> l, String targetLoc) {
		Map<String, DataLogTransferInfo> location2dlti = dltis.computeIfAbsent(l, LogTransferUtil::createDltiMap);
		return location2dlti.computeIfAbsent(targetLoc, loc -> findTransferInfoInList(l, loc));
	}
	
	public static void startTransmitLogData(SingleValueResource resource,
			ResourceList<DataLogTransferInfo> dataLogs) {
		//System.err.printf("startTrasmitLogData(%s, %s) [%d]%n", resource.getPath(), dataLogs.getPath(), callcounter.incrementAndGet());
		//DataLogTransferInfo log = null;
		if(dataLogs == null) {
			if(!warningDone) {
				System.out.println("No DataLogTransferInfo available! Skipping transfer...");
				warningDone = true;
			}
			return;
		}
		
		Map<String, DataLogTransferInfo> location2dlti = dltis.computeIfAbsent(dataLogs, LogTransferUtil::createDltiMap);
		
		//log = location2dlti.get(resource.getPath());
		
		location2dlti.computeIfAbsent(resource.getPath(), p -> {
			DataLogTransferInfo log = findTransferInfoInList(dataLogs, p);
			if (log != null) {
				location2dlti.put(p, log);
				return log;
			}
			log = dataLogs.add();

			StringResource clientLocation = log.clientLocation().create();
			clientLocation.setValue(resource.getPath());

			TimeIntervalLength tLength = log.transferInterval().timeIntervalLength().create();
			IntegerResource type = tLength.type().create();
			type.setValue(10);
			log.activate(true);
			//System.err.printf("activated new DataLogTransferInfo for %s: %s%n", resource.getPath(), log.getPath());
			return log;
		});
		/*
		for(DataLogTransferInfo dl : dataLogs.getAllElements()) {
			if(dl.clientLocation().getValue().equals(resource.getPath()))
				log = dl;
		}
		*/
/*
		if(log == null){
			log = dataLogs.add();
		}
		
		StringResource clientLocation = log.clientLocation().create();
		clientLocation.setValue(resource.getPath());
		
		TimeIntervalLength tLength = log.transferInterval().timeIntervalLength().create();
		IntegerResource type = tLength.type().create();
		type.setValue(10);
		log.activate(true);
		location2dlti.put(resource.getPath(), log);
		System.err.printf("activated new DataLogTransferInfo for %s: %s%n", resource.getPath(), log.getPath());
*/
	}
	
	public static void stopTransmitLogData(SingleValueResource resource,
			ResourceList<DataLogTransferInfo> dataLogs) {
		DataLogTransferInfo log = null;
		log = findTransferInfo(dataLogs, resource.getPath());
		/*
		for(DataLogTransferInfo dl : dataLogs.getAllElements()) {
			if(dl.clientLocation().getValue().equals(resource.getPath()))
				log = dl;
		}
		*/
		if(log == null) return;
		log.delete();
	}
	
	public static boolean isResourceTransferred(SingleValueResource resource,
			ResourceList<DataLogTransferInfo> dataLogs) {
		if(dataLogs == null) return false;
		DataLogTransferInfo log = null;
		/*
		for(DataLogTransferInfo dl : dataLogs.getAllElements()) {
			if(dl.clientLocation().getValue().equals(resource.getPath()))
				log = dl;
		}
		*/
		log = findTransferInfo(dataLogs, resource.getPath());
		
		return(log != null && log.isActive());
	}
	

}
