package org.ogema.timeseries.eval.simple.api;

import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointGroup;
import org.ogema.devicefinder.api.DatapointService;

public class RemoteTimeseriesUtil {
	public static String[] getGatewayLocationForMirror(SingleValueResource sres) {
		return getGatewayLocationForMirror(sres.getLocation());
	}
	/** Return remote location for a location in the serverMirror resource structure
	 * 
	 * @param mirrorResourceLocation
	 * @return [0]: gatewayId, [1]: remote location or local location if the location is not in serverMirror
	 */
	public static String[] getGatewayLocationForMirror(String mirrorResourceLocation) {
		String gwId;
		String location;
		String[] els = mirrorResourceLocation.split("/", 4);
		if(els[0].equals("serverMirror") && (els.length >= 4)) {
			gwId = els[1].substring(1);
			location = els[3];
		} else  {
			gwId = "Local";
			location = mirrorResourceLocation;
		}
		return new String[] {gwId, location};
	}
	
	/** Get remote slotsDB datapoint corresponding to a mirror resource
	 * 
	 * @param sres
	 * @param dpService
	 * @return corresponding remote datapoint. If sres is not in the serverMirror structure then a local datapoint is returned
	 */
	public static Datapoint getRemoteDpForMirrorExisting(SingleValueResource sres, DatapointService dpService) {
		String[] gwLoc = getGatewayLocationForMirror(sres);
		if(gwLoc[0].equals("Local"))
			return dpService.getDataPointAsIs(gwLoc[1]);
		String[] check = gwLoc[1].split("\\$X\\$");
		String dpLocEnd;
		if(check.length == 2) {
			dpLocEnd = check[1];
		} else
			dpLocEnd = gwLoc[1];
		for(Datapoint dp: dpService.getAllDatapoints(gwLoc[0])) {
			if(dp.getLocation().endsWith(dpLocEnd))
				return dp;
		}
		return null;
	}
	
	public static Datapoint getRemoteDpForMirror(SingleValueResource sres, DatapointService dpService,
			DatapointGroup dpg) {
		String[] gwLoc = getGatewayLocationForMirror(sres);
		if(gwLoc[0].equals("Local"))
			return dpService.getDataPointStandard(gwLoc[1]);
		
		String[] check = gwLoc[1].split("\\$X\\$");
		//String dpLocEnd;
		if(check.length == 2) {
			int idx = check[0].lastIndexOf('/');
			if(idx >= 0)
				gwLoc[1] = check[0].substring(0, idx+1)+check[1];
			else
				gwLoc[1] = check[1];
			//dpLocEnd = check[1];
		} //else
			//dpLocEnd = gwLoc[1];

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
		//if(fullPathPre == null)
		//	throw new IllegalStateException("Full path does not fit to mirrorResource!");
		
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
