package org.smartrplace.tissue.util.logconfig;

import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.devicefinder.api.Datapoint;

public class VirtualSensorKPIDataBase {
	public ResourceValueListener<FloatResource> aggListener;
	/** TS-ANY-Datapoint providing the result timeseries for a virtual datapoint
	 * The location of this datapoint should represent the input time series of the calculation of the virtual
	 * with a pre and/or post fix for the calculation method.<br>
	 * Both evalDp and resourceDp have the same time series representing the virtual sensor.
	 */
	public Datapoint evalDp;		
	public Integer absoluteTiming;
	
	/** Datapoint with location of the resource representing the virtual schedule.
	 */
	public Datapoint resourceDp;
	
	/** see VirtualScheduleService: We assume that only a single schedule is possible here.
	 * If no governedSchedule is registered, this is null*/
	public Schedule governedSchedule;
}
