package org.ogema.devicefinder.api;

public class DpUpdateAPI {
	/** Gap or interval requiring recalculation requirement*/
	public static class DpGap {
		public long start;
		public long end;
	}
	
	public static class DpUpdated extends DpGap {
		//Time when datapoint time series as updated for a certain interval
		public long updateTime;
	}
}
