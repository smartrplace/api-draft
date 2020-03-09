package org.smartrplace.apps.heatcontrol.extensionapi;

public class KnownValue {
	
	public final long time;
	public final float value;
	
	public KnownValue(float value, long time) {
		this.time = time;
		this.value = value;
	}
	
}
