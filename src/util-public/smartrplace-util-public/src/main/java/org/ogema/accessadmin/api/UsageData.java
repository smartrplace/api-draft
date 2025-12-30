package org.ogema.accessadmin.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.SubcustomerUtil.SettingsBaseData;

public class UsageData extends SettingsBaseData {
	public static int STARTEND_WORKINGDAY_IDX = 7;
	public static int STARTEND_WEEKEND_IDX = 8;
	public static int STARTEND_TIMES_SIZE = 9;
	
	/** Day type (0..6 = Monday...Sunday) -> even-idx: startTime, odd-idx: endTime*/
	public Map<Integer, List<Long>> startEndTimes = new HashMap<>();
	public boolean isCurveSimple = true;
	
	public List<Long> getStartEndTimesMonday() {
		return startEndTimes.get(0);
	}
	public List<Long> getStartEndTimesTuesday() {
		return startEndTimes.get(1);
	}
	public List<Long> getStartEndTimesWednesday() {
		return startEndTimes.get(2);
	}
	public List<Long> getStartEndTimesThursday() {
		return startEndTimes.get(3);
	}
	public List<Long> getStartEndTimesFriday() {
		return startEndTimes.get(4);
	}
	public List<Long> getStartEndTimesSaturday() {
		return startEndTimes.get(5);
	}
	public List<Long> getStartEndTimesSunday() {
		return startEndTimes.get(6);
	}
	public List<Long> getStartEndTimesWorkingday() {
		return startEndTimes.get(STARTEND_WORKINGDAY_IDX);
	}
	public List<Long> getStartEndTimesWeekend() {
		return startEndTimes.get(STARTEND_WEEKEND_IDX);
	}
	
	/** Legacy support: receive startTime / endTime from GUI*/
	public void setStartTime(long startTime) {
		startTimeFromRoomData = startTime;
	}
	public void setEndTime(long endTime) {
		endTimeFromRoomData = endTime;
	}
	
	public void setStartEndTimesMonday(List<Long> tm) {
		startEndTimes.put(0, tm);
	}
	public void setStartEndTimesTuesday(List<Long> tm) {
		startEndTimes.put(1, tm);
	}
	public void setStartEndTimesWednesday(List<Long> tm) {
		startEndTimes.put(2, tm);
	}
	public void setStartEndTimesThursday(List<Long> tm) {
		startEndTimes.put(3, tm);
	}
	public void setStartEndTimesFriday(List<Long> tm) {
		startEndTimes.put(4, tm);
	}
	public void setStartEndTimesSaturday(List<Long> tm) {
		startEndTimes.put(5, tm);
	}
	public void setStartEndTimesSunday(List<Long> tm) {
		startEndTimes.put(6, tm);
	}
	public void setStartEndTimesWorkingday(List<Long> tm) {
		startEndTimes.put(STARTEND_WORKINGDAY_IDX, tm);
	}
	public void setStartEndTimesWeekend(List<Long> tm) {
		startEndTimes.put(STARTEND_WEEKEND_IDX, tm);
	}
}
