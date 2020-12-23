package org.ogema.devicefinder.api;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.timeseries.ReadOnlyTimeSeries;
import org.ogema.model.gateway.KPIData;
import org.ogema.timeseries.eval.simple.api.ProcessedReadOnlyTimeSeries;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatSchedules;
import org.smartrplace.tissue.util.logconfig.VirtualSensorKPIMgmt;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;

/** Management for schedules that are copies of memory timeseries usually operated for virtual sensors and
 * similar KPIs<br>
 * Currently this is used to notify all schedules registered in the management that the referenceMeteringTime changed.
 * This is done via the method {@link #resetAll()}. This means all data in the time series is marked for recalculation
 * and all ViaHeartbeatSchdules providers (sending datapoints, not schedules) are marked for "resend all". It also
 * updates values in local "mirror schedules" to the datapoints, which is currently not used as we just use sending data
 * to remote schedules.
 */
public class VirtualScheduleService {
	/** List of schedules governed by a datapoint. Note that it is possible that one
	 * datapoint governs several schedules (although this usually does not make much sense),
	 * but each schedule can only be governed by a single schedule, otherwise the data would
	 * be mixed up or the result would be unpredictable*/
	public static class DpGovernData {
		Set<Schedule> schedules = new HashSet<>();
		long intervalToStayBehindNow;
	}
	
	private final Map<Datapoint, DpGovernData> schedules = new HashMap<>();
	private final DatapointService dpService;
	private final ApplicationManager appMan;
	
	public VirtualScheduleService(DatapointService dpService, ApplicationManager appMan) {
		this.dpService = dpService;
		this.appMan = appMan;
	}

	/**
	 * 
	 * @param dp
	 * @param sched may be null if just the datapoint shall be updated on a reset
	 * @return true if the schedule was not yet governed by this datapoint
	 */
	public boolean add(Datapoint dp, Schedule sched, long intervalToStayBehindNow) {
		DpGovernData mySet = schedules.get(dp);
		if(mySet == null) {
			mySet = new DpGovernData();
			mySet.intervalToStayBehindNow = intervalToStayBehindNow;
			schedules.put(dp, mySet);
		} else if(intervalToStayBehindNow > mySet.intervalToStayBehindNow)
			mySet.intervalToStayBehindNow = intervalToStayBehindNow;
		if(sched != null)	
			return mySet.schedules.add(sched);
		return false;
	}
	
	public Schedule addDefaultSchedule(Datapoint dp, long intervalToStayBehindNow) {
		KPIData evc = ResourceHelper.getOrCreateTopLevelResource(KPIData.class, appMan);
		evc.ts().create();
		String label = dp.label(null);
		String resName = ResourceUtils.getValidResourceName(label);
		FloatResource fres = ResourceListHelper.getOrCreateNamedElementFlex(resName, evc.ts());
		fres.program().create();
		evc.ts().activate(true);
		add(dp, fres.program(), intervalToStayBehindNow);
		return fres.program();
	}
	
	/**
	 * 
	 * @return number of time series reset
	 */
	public int resetAll() {
		int count = 0;
		for(Entry<Datapoint, DpGovernData> set: schedules.entrySet()) {
			ReadOnlyTimeSeries dpTs = set.getKey().getTimeSeries();
			if(dpTs == null || (!(dpTs instanceof ProcessedReadOnlyTimeSeries)))
				throw new IllegalStateException("Datapoint without ProcessedReadOnlyTimeSeries registered for schedule governance:"+set.getKey().getLocation());
				//continue;
			long nowReal = dpService.getFrameworkTime();
			ProcessedReadOnlyTimeSeries proc = (ProcessedReadOnlyTimeSeries) dpTs;
			/** In updateScheduleFromDatapoint values will be recalculated anyways, so we do not have to do it here*/
			proc.reset(null);
			ViaHeartbeatSchedules sprov = (ViaHeartbeatSchedules) set.getKey().getParameter(Datapoint.HEARTBEAT_STRING_PROVIDER_PARAM);
			if(sprov != null) {
				sprov.resendAllOnNextOccasion();
			}
			for(Schedule sched: set.getValue().schedules) {
				sched.deleteValues();
				VirtualSensorKPIMgmt.updateScheduleFromDatapoint(sched, set.getKey(), nowReal, set.getValue().intervalToStayBehindNow, true);
			}
			count++;
		}
		return count;
	}
}
