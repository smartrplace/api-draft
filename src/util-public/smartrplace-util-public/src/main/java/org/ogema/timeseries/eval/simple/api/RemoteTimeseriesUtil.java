package org.ogema.timeseries.eval.simple.api;

import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.devicefinder.api.Datapoint;
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
	
	public static Datapoint getRemoteDpForMirror(SingleValueResource sres, DatapointService dpService) {
		String[] gwLoc = getGatewayLocationForMirror(sres);
		if(gwLoc[0].equals("Local"))
			return dpService.getDataPointStandard(gwLoc[1]);
		return dpService.getDataPointStandard(gwLoc[1], gwLoc[0]);
	}
}
