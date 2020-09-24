package org.ogema.devicefinder.api;

public interface DatapointInfoProvider extends DatapointDesc {
	
	/** Some datapoints support a current values besides time series. For datapoints based on SingleValueResource
	 * implementation is simple, otherwise the implementation should be provided via a {@link DatapointInfoProvider}*/
	Float getCurrentValue();
	boolean setCurrentValue(Float value);	
}
