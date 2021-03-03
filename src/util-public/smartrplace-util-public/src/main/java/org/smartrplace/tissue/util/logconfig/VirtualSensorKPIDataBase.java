package org.smartrplace.tissue.util.logconfig;

import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.units.EnergyResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.devicefinder.api.Datapoint;

public class VirtualSensorKPIDataBase {
	public ResourceValueListener<EnergyResource> aggListener;
	public Datapoint evalDp;		
	public Integer absoluteTiming;
	public Datapoint resourceDp;
	
	/** see VirtualScheduleService: We assume that only a single schedule is possible here.
	 * If no governedSchedule is registered, this is null*/
	public Schedule governedSchedule;
}
