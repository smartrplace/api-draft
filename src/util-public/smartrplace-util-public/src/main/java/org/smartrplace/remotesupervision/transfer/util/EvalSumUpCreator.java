package org.smartrplace.remotesupervision.transfer.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.application.Timer;
import org.ogema.core.application.TimerListener;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.schedule.Schedule;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.model.eval.base.EvaluationAvMMSConfig;
import org.ogema.model.gateway.remotesupervision.GatewayTransferInfo;
import org.ogema.util.eval.ContinuousAvMMSEvalCreator;
import org.ogema.util.eval.EvalScheduleSumUpCreator;
import org.ogema.util.eval.SimpleAggregation;

import de.iwes.tools.statistics.StatisticalProvider;
import de.iwes.tools.statistics.StatisticsHelper;

/**Aggregate a list of FloatResources into a new result. The aggregation is done based on
 * a timer or an external Action. If the aggregation shall be done based on a listener
 * another implementation has to be used.
  */
public class EvalSumUpCreator implements StatisticalProvider {
	//final private int mode;
	//final private FloatResource destination;
	
	FloatResource destinationMin;
	FloatResource destinationMax;
	FloatResource destinationAv;
	
//	private AbsolutePersistentTimer timer;
	private Timer timer;
	
	public class SourceElementInfo {
		ContinuousAvMMSEvalCreator eval = null;
		List<Schedule> schedules;
	}
	protected Map<FloatResource, SourceElementInfo> sources = new HashMap<>();
	public class ScheduleSumUpIntervalType {
		int type;
		EvalScheduleSumUpCreator floatValueSumUpMin;
		EvalScheduleSumUpCreator floatValueSumUpMax;
		EvalScheduleSumUpCreator floatValueSumUpAv;
	}
	protected List<ScheduleSumUpIntervalType> scheduleSumUps = new ArrayList<>();
	long currentAlignedTime;
	
	EvalScheduleSumUpCreator scheduleSumpUp;
	final protected ApplicationManager appMan;
	final Resource lastTimeParent;
	final int[] intervals;
	final ResourceList<EvaluationAvMMSConfig> evalList;
	final String id;
	final boolean addSchedule;
	private GatewayTransferInfo remoteTransfer = null;
	
	/**
	 * @param mode primary aggregation mode:
	 * 10: standard<br>
	 * 11: primary sources are counters, minimum/maximum should be calculated based on differences<br>
	 * 12: like 11, but instead of average integrals shall be calculated<br>
	 * @param destination resource to write into
	 * @param id description or path of source resources
	 * @param sources initial sources, if null initialized without sources
	 * @param appMan
	 * @param instantInterval if positive the scheduleHolder Resource is updated with this interval
	 * @param lastTimeParent may be null if no schedules configured
	 * @param intervals may be null if no schedules configured
	 * @param evalList may be null if no schedules configured
	 */
	public EvalSumUpCreator(int mode, String id, List<FloatResource> sources, 
			final ApplicationManager appMan,
			long instantInterval, boolean addSchedule,
			Resource lastTimeParent, int[] intervals, ResourceList<EvaluationAvMMSConfig> evalList) {
		super();
		//this.mode = mode;
		//this.destination = destination;
		this.appMan = appMan;
		this.lastTimeParent = lastTimeParent;
		this.intervals = intervals;
		this.evalList = evalList;
		this.id = id;
		this.addSchedule = addSchedule;
		if(sources != null) {
			for(FloatResource s: sources) {
				addResource(s);
			}
		}
		if(instantInterval > 0) {
			timer = appMan.createTimer(instantInterval, new TimerListener() {
				@Override
				public void timerElapsed(Timer timer) {
					if(destinationMin == null) {
						EvalSumUpCreator.this.appMan.getLogger().warn("In EvalSumCreator timer destinationMin... null!");
						return;
					}
					updateDestination(destinationMin, 1);
					updateDestination(destinationMax, 2);
					updateDestination(destinationAv, 3);
				}
			});
		}
	}
	
	/**Call this when the {@link GatewayTransferInfo} resource is available.
	 * @param intervalsToTransfer should be a subset of the intervals specified
	 * in the constructor (we can calculate more interval types than we transfer)*/
	public void gatewayTransferAvailable(GatewayTransferInfo remoteTransfer,
			int[] intervalsToTransfer) {
		if(this.remoteTransfer != null) return;
		this.remoteTransfer = remoteTransfer;
		if(addSchedule) {
			boolean init = false;
			for(int type: intervalsToTransfer) {
				ScheduleSumUpIntervalType ssit = new ScheduleSumUpIntervalType();
				ssit.type = type;
				scheduleSumUps.add(ssit);
				ssit.floatValueSumUpMin = new EvalScheduleSumUpCreator(1, 
						EvalSetup.addOrCreateScheduleTransfer("1:Min"+id+"_T"+type, remoteTransfer.valueData(), null).scheduleHolder().historicalData(),
						null, type,
						lastTimeParent.getSubResource("lastTimeMin_"+id+"_T"+type, TimeResource.class), appMan);
				ssit.floatValueSumUpMax = new EvalScheduleSumUpCreator(2, 
						EvalSetup.addOrCreateScheduleTransfer("2:Max"+id+"_T"+type, remoteTransfer.valueData(), null).scheduleHolder().historicalData(),
						null, type,
						lastTimeParent.getSubResource("lastTimeMax_"+id+"_T"+type, TimeResource.class), appMan);
				ssit.floatValueSumUpAv = new EvalScheduleSumUpCreator(3, 
						EvalSetup.addOrCreateScheduleTransfer("3:Av"+id+"_T"+type, remoteTransfer.valueData(), null).scheduleHolder().historicalData(),
						null, type,
						lastTimeParent.getSubResource("lastTimeAv_"+id+"_T"+type, TimeResource.class), appMan);
				if(!init) {
					destinationMin = ssit.floatValueSumUpMin.getDestinationParent();
					destinationMax = ssit.floatValueSumUpMax.getDestinationParent();
					destinationAv = ssit.floatValueSumUpAv.getDestinationParent();
					init = true;
				}
			}
		} else {
			destinationMin = EvalSetup.addOrCreateValueTransfer("1:Min"+id,
					remoteTransfer.valueData(), "1:Min"+id).value();
			destinationMax = EvalSetup.addOrCreateValueTransfer("2:Max"+id,
					remoteTransfer.valueData(), "2:Max"+id).value();
			destinationAv = EvalSetup.addOrCreateValueTransfer("3:Av"+id,
					remoteTransfer.valueData(), "3:Av"+id).value();
		}		
		for(Entry<FloatResource, SourceElementInfo> temp: sources.entrySet()) {
			addFloatSensorEval(temp.getKey(), temp.getValue());
		}
	}
	
	public void addFloatSensorEval(FloatResource resource, SourceElementInfo sei) {
		if(remoteTransfer == null) return;
		sei.eval = new ContinuousAvMMSEvalCreator(resource.getLocation(), evalList,
				EvaluationAvMMSConfig.class, "sensor-actor-eval",
				intervals, true, resource, appMan);
		for(ScheduleSumUpIntervalType ssit: scheduleSumUps) {
			ssit.floatValueSumUpMin.addSchedule(StatisticsHelper.getIntervalTypeStatistics(
					ssit.type, sei.eval.getEvalResource().min()).historicalData());
			ssit.floatValueSumUpMax.addSchedule(StatisticsHelper.getIntervalTypeStatistics(
					ssit.type, sei.eval.getEvalResource().max()).historicalData());
			ssit.floatValueSumUpAv.addSchedule(StatisticsHelper.getIntervalTypeStatistics(
					ssit.type, sei.eval.getEvalResource().destinationStat()).historicalData());
		}
	}

	/**Call this whenever update shall be triggered from outside*/
	public void updateDestination(FloatResource destination, int mode) {
		//we got all values, we can transfer now
		float sumUp = (new SimpleAggregation<FloatResource>() {
			@Override
			protected float getValue(FloatResource element) {
				return element.getValue();
			}
		}).getAggregatedValue(sources.keySet(), mode);
		destination.setValue(sumUp);
	}
	
	public SourceElementInfo addResource(FloatResource res) {
		if(sources.containsKey(res)) return null;
		SourceElementInfo result = new SourceElementInfo();
		sources.put(res, result);
		return result;
	}
	public boolean removeResource(FloatResource res) {
		return (sources.remove(res)) != null;
	}
	public SourceElementInfo addResourceAndSchedule(FloatResource res) {
		SourceElementInfo sei = addResource(res);
		addFloatSensorEval(res, sei);
		return sei;
	}
	public boolean removeResourceAndSchedule(FloatResource res) {
		SourceElementInfo sei = sources.get(res);
		if(sei!= null) {
			for(ScheduleSumUpIntervalType eval: scheduleSumUps) {
				for(Schedule s: sei.schedules) {
					eval.floatValueSumUpMin.removeSchedule(s);
					eval.floatValueSumUpMax.removeSchedule(s);
					eval.floatValueSumUpAv.removeSchedule(s);
				}
			}
		}
		return removeResource(res);
	}

	public boolean addSchedule(Schedule s) {
		if(scheduleSumpUp != null) return scheduleSumpUp.addSchedule(s);
		return false;
	}
	public boolean removeSchedule(Schedule schedule) {
		if(scheduleSumpUp != null) return scheduleSumpUp.removeSchedule(schedule);
		return false;
	}
	
	@Override
	public void close() {
		if (timer != null)
			timer.stop();
	}
}
