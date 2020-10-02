package org.ogema.timeseries.eval.simple.api;

import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;

public class RemoteTimeseriesUtil {
	public static String[] getGatewayLocationForMirror(SingleValueResource sres) {
		String gwId;
		String location;
		String[] els = sres.getLocation().split("/", 4);
		if(els[0].equals("serverMirror") && (els.length >= 4)) {
			gwId = els[1].substring(1);
			location = els[3];
		} else  {
			gwId = "Local";
			location = sres.getLocation();
		}
		return new String[] {gwId, location};
	}
	
	public static Datapoint getRemoteDpForMirror(SingleValueResource sres, DatapointService dpService,
			DatapointGroup dpg) {
		String[] gwLoc = getGatewayLocationForMirror(sres);
		if(gwLoc[0].equals("Local"))
			return dpService.getDataPointStandard(gwLoc[1]);
		
		String fullPathPre = null;
		if(dpg.getAllDatapoints().isEmpty())
			fullPathPre = "";
		else {
			String[] sresEls = gwLoc[1].split("/");
			for(Datapoint dpInDev: dpg.getAllDatapoints()) {
				String fullAnyDeviceSensor = dpInDev.getLocation();
				String[] fullEls = fullAnyDeviceSensor.split("/");
				for(int idx2=0; idx2<fullEls.length; idx2++) {
					if(areAllRemainingElementsEqual(sresEls, 0, fullEls, idx2)) {
						fullPathPre = "";
						for(int j=0; j<idx2; j++)
							fullPathPre += fullEls[j]+"/";
						break;
					}
				}
				if(fullPathPre != null)
					break;
			}
			if(fullPathPre == null) {
				//try to get prePath from devices
				String[] idGw = DatapointGroup.getGroupIdAndGw(dpg.id());
				String[] devEls = idGw[0].split("/");
				fullPathPre = "";
				if(devEls.length >= 2) {
					for(int j=0; j<(devEls.length-1); j++)
						fullPathPre += devEls[j]+"/";
				}
				/*String[] idGw = DatapointGroup.getGroupIdAndGw(dpg.id());
				String[] devEls = idGw[0].split("/");
				for(int idx2=0; idx2<devEls.length; idx2++) {
					if(areAllRemainingElementsInEl1(sresEls, devEls, idx2)) {
						fullPathPre = "";
						for(int j=0; j<idx2; j++)
							fullPathPre += devEls[j]+"/";
						break;
					}
				}*/
			}
		}
		if(fullPathPre == null)
			throw new IllegalStateException("Full path does not fit to mirrorResource!");
		
		return dpService.getDataPointStandard(fullPathPre+gwLoc[1], gwLoc[0]);
	}
	
	public static boolean areAllRemainingElementsEqual(String[] els1, int index1, String[] els2, int index2) {
		int idx2 = index2;
		for(int idx1=index1; idx1<els1.length; idx1++) {
			if(idx2 >= els2.length)
				return false;
			if(!els1[idx1].equals(els2[idx2]))
				return false;
			idx2++;
		}
		if(idx2 != (els2.length))
			return false;
		return true;
	}
	
	public static boolean areAllRemainingElementsInEl1(String[] els1, String[] els2, int index2) {
		int idx2 = index2;
		for(int idx1=0; idx1<els1.length; idx1++) {
			if(idx2 >= els2.length)
				return true;
			if(!els1[idx1].equals(els2[idx2]))
				return false;
			idx2++;
		}
		return true;
	}

}
