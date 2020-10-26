package org.ogema.recordreplay.testing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.IntegerValue;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.recplay.testing.RecReplayAlarmingBaseData;
import org.ogema.model.recplay.testing.RecReplayData;
import org.ogema.model.recplay.testing.RecReplayDeviation;
import org.ogema.model.recplay.testing.RecReplayObserverData;
import org.ogema.tools.resource.util.ValueResourceUtils;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.util.resource.ResourceHelper;
import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.util.resourcelist.ResourceListHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class RecReplayAlarmingBaseObserver implements RecReplayObserver {
	protected final long maxDeviationTime = Long.getLong("org.ogema.recordreplay.testing.alarmingbase.maxdeviation", 500);
	
	protected final List<AlarmConfiguration> alarms;
	protected final RecReplayAlarmingBaseData observerData;
	protected final boolean isInReplayMode;
	protected final Map<String, IntegerResource> resForRecording = new HashMap<>();
	protected final Map<String, AlarmConfigReplayData> eventsByAlarmConfig = new HashMap<>();
	protected AlarmConfigReplayData getACData(String acLoc) {
		AlarmConfigReplayData result = eventsByAlarmConfig.get(acLoc);
		if(result == null) {
			result = new AlarmConfigReplayData();
			eventsByAlarmConfig.put(acLoc, result);
		}
		return result;
	}
	
	protected static class EventAlarmingBase extends EventRecorded {
		public AlarmConfiguration ac;
		public int value;
	}
	
	protected static class AlarmConfigReplayData {
		List<EventAlarmingBase> events = new ArrayList<>();
		int lastEventIndexFound = -1;
	}
	
	protected final List<EventAlarmingBase> events = new ArrayList<>();
	
	@Override
	public String id() {
		return this.getClass().getName();
	}

	@Override
	public String label(OgemaLocale locale) {
		return "Alarming Base RecReply";
	}

	@Override
	public Class<? extends RecReplayObserverData> resourceType() {
		return RecReplayAlarmingBaseData.class;
	}

	public RecReplayAlarmingBaseObserver(final List<AlarmConfiguration> alarms, ApplicationManager appMan) {
		if(!Boolean.getBoolean("org.ogema.recordreplay.testing.alarmingbase"))
			throw new IllegalStateException("RecReplay Observer not configured!");
		this.alarms = alarms;
		RecReplayData rrData = ResourceHelper.getTopLevelResource(RecReplayData.class, appMan.getResourceAccess());
		observerData = (RecReplayAlarmingBaseData) ResourceListHelper.getOrCreateNamedElementFlex(rrData.observerData(), resourceType());
		isInReplayMode = Boolean.getBoolean("org.ogema.recordreplay.testing.replaytestmode");
		if(!isInReplayMode) {
			ResourceList<IntegerResource> rlist = observerData.alarms();
			rlist.create();
			observerData.alarmingConigPaths().create();
			if(!(rlist.size() == 0))
				throw new IllegalStateException("Resource must be empty intially for recording!");
			if(!(observerData.alarmingConigPaths().size() == 0))
				throw new IllegalStateException("Resource must be empty initially for recording!");
			for(AlarmConfiguration ac: alarms) {
				IntegerResource res = rlist.add();
				resForRecording.put(ac.getLocation(), res);
				IntegerResource alStatus = AlarmingConfigUtil.getAlarmStatus(ac.sensorVal().getLocationResource());
				ValueResourceHelper.setCreate(res, alStatus.getValue());
				ValueResourceUtils.appendValue(observerData.alarmingConigPaths(), ac.getLocation());
			}
			observerData.activate(true);
		} else {
			ResourceList<IntegerResource> rlist = observerData.alarms();
			List<IntegerResource> allEls = rlist.getAllElements();
			if(!(rlist.size() == observerData.alarmingConigPaths().size()))
				throw new IllegalStateException("Size of lists must be equal!");
			for(int idx=0; idx<rlist.size(); idx++) {
				IntegerResource res = allEls.get(idx);
				String alLoc = observerData.alarmingConigPaths().getElementValue(idx);
				resForRecording.put(alLoc, res);
			}			
		}
		//we do this at the end as this is the most relevant point for further processing
		checkStartTime(appMan);
	}
	
	protected void checkStartTime(ApplicationManager appMan) {
		long now = appMan.getFrameworkTime();
		if(!isInReplayMode) {
			ValueResourceHelper.setCreate(observerData.observerStartTime(), now);
		} else {
			long diff = now - observerData.observerStartTime().getValue();
			if(diff > (maxDeviationTime/2))
				System.out.println("   WARNING : Observer startup deviation for "+label(null)+" of "+diff+" msec");
			else
				System.out.println("     INFO  : Observer startup deviation for "+label(null)+" of "+diff+" msec");
		}
		
	}
	
	//public void startRecording() {
	//}
	
	public void recordNewAlarm(AlarmConfiguration ac, long now) {
		IntegerResource alStatus = AlarmingConfigUtil.getAlarmStatus(ac.sensorVal().getLocationResource());
		IntegerResource res = resForRecording.get(ac.getLocation());
		int statVal = alStatus.getValue();
		if(!res.program().isActive()) {
			res.program().create();
			res.program().addValue(now, new IntegerValue(statVal));
			res.program().activate(false);
		} else
			res.program().addValue(now, new IntegerValue(statVal));
		addExpectedEvent(ac, statVal, now);
	}
	
	protected void addExpectedEvent(AlarmConfiguration ac, int statVal, long now) {
		EventAlarmingBase ev = new EventAlarmingBase();
		ev.timeExpected = now;
		ev.description = "Alarm at "+ac.getName()+" expected:"+statVal;
		ev.ac = ac;
		ev.value = statVal;
		
		if(isInReplayMode) {
			ev.timeLatest = now + maxDeviationTime;
			ev.timeEarliest = now - maxDeviationTime;
		}
		events.add(ev);
		getACData(ac.getLocation()).events.add(ev);
	}
	
	@Override
	public List<RecReplayDeviation> checkInitialReplay() {
		List<RecReplayDeviation> result = new ArrayList<>();
		ResourceList<RecReplayDeviation> devlist = observerData.deviations();
		devlist.create();
		if(!(devlist.size() == 0))
			throw new IllegalStateException("Resource must be empty intially for replay!");
		for(AlarmConfiguration ac: alarms) {
			IntegerResource res = resForRecording.get(ac.getLocation());
			IntegerResource alStatus = AlarmingConfigUtil.getAlarmStatus(ac.sensorVal().getLocationResource());
			if(!(res.getValue() != alStatus.getValue())) {
				RecReplayDeviation dev = devlist.add();
				getNewDeviation(ac, "Init-AC:"+ac.getName()+" expected:"+res.getValue()+" Found:"+alStatus.getValue(), null, null);
				result.add(dev);
			}
			if(res.program().exists()) for(SampledValue sv: res.program().getValues(0)) {
				addExpectedEvent(ac, sv.getValue().getIntegerValue(), sv.getTimestamp());				
			}
		}
		events.sort(new Comparator<EventAlarmingBase>() {

			@Override
			public int compare(EventAlarmingBase o1, EventAlarmingBase o2) {
				return Long.compare(o1.timeExpected, o2.timeExpected);
			}
		});
		devlist.activate(true);
		return result;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List<EventRecorded> events() {
		return (List)events;
	}
	
	//Just check new event without expired
	protected RecReplayDeviation newEvent(AlarmConfiguration ac, long now) {
		IntegerResource res = resForRecording.get(ac.getLocation());
		AlarmConfigReplayData data = getACData(ac.getLocation());
		List<SampledValue> svs = null;
		EventAlarmingBase ev = null;
		if(data.events.size() > (data.lastEventIndexFound+1)) {
			ev = data.events.get(data.lastEventIndexFound+1);
		}			
		if(ev == null)
			return getNewDeviation(ac, "Unexpected alarm for "+ac.getName()+" at "+StringFormatHelper.getTimeDateInLocalTimeZone(now), now, null);
		ev.isFound = true;
		ev.success = false;
		(data.lastEventIndexFound)++;
		if(now < ev.timeEarliest)
			return getNewDeviation(ac, "Alarm too early for "+ac.getName()+" by "+StringFormatHelper.getFormattedValue(ev.timeEarliest - now)+
					" at "+StringFormatHelper.getTimeDateInLocalTimeZone(now), now, ev.timeExpected);
		if(now > ev.timeLatest)
			return getNewDeviation(ac, "Alarm too late for "+ac.getName()+" by "+StringFormatHelper.getFormattedValue(now - ev.timeLatest)+
					" at "+StringFormatHelper.getTimeDateInLocalTimeZone(now), now, ev.timeExpected);
		ev.success = true;
		return null;
	}
	
	@Override
	public List<RecReplayDeviation> consolidateMissingEvents(long now) {
		List<RecReplayDeviation> result = new ArrayList<>();
		for(EventAlarmingBase ev: events) {
			if(ev.timeLatest >= now)
				break;
			if(ev.isFound)
				continue;
			ev.success = false;
			RecReplayDeviation dev = getNewDeviation(ev.ac, "Alarm not found for "+ev.ac.getName()+
					" at "+StringFormatHelper.getTimeDateInLocalTimeZone(now), null, ev.timeExpected);
			result.add(dev);
		}
		return result;
	}
	
	protected RecReplayDeviation getNewDeviation(AlarmConfiguration ac, String description, Long now, Long expected) {
		RecReplayDeviation result = observerData.deviations().add();
		result.reference().setAsReference(ac);
		ValueResourceHelper.setCreate(result.description(), description);
		if(now != null)
			ValueResourceHelper.setCreate(result.timeOccured(), now);
		if(expected != null)
			ValueResourceHelper.setCreate(result.timeExpected(), now);
		return result;
	}
}
