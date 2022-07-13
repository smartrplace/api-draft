package org.ogema.accessadmin.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.SubcustomerUtil.SettingsBaseData;
import org.ogema.accessadmin.api.SubcustomerUtil.ValueVariance;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;

public class HeatCoolData extends SettingsBaseData {
	public float comfortTemperature;
	public float ecoTemperatureV2;
	public float windowOpenTemperatureV2;

	//public float ecoTemperatureV1;
	public float setSingleSetpoint_Window_open;
	/**Both minSetpoint and minSetpointAuto*/
	public float minSetpointAuto;
	public float maxSetpointAuto;
	
	public boolean ecoEqualsOff;
	public Integer dayTypeSettingMode;
	
	public ValueVariance[] valueEval;
	/** Number of rooms with special day settings*/ 
	public int specialDaySettingsRooms = 0;
	/** Number of rooms with nonworking-day temperature unequal to eco-tem*/ 
	public int specialNonWorkingRooms = 0;
	/** Number of rooms with that cannot be mapped to standard start/end setpoint curve characteristics*/
	public int specialStructureRooms = 0;
	
	public boolean showComfortTemperature = false;
	
	public HeatCoolData() {
		this(SubcustomerUtil.VALUE_IDX_NUM);
	}
	/** More general approach could also be used in other applications*/
	public HeatCoolData(int valueNum) {
		valueEval = new ValueVariance[valueNum];
		for(int i=0; i<valueNum; i++)
			valueEval[i] = new ValueVariance(i);
	}
	
	// TODO: The following values are to be removed
	public int specialSettingsStartTime;
	public int specialSettingsEndTime;
	public int specialSettingsUsageTemperature;
	public int specialSettingsNonUsageTemperature;
	public int specialSettingsComfortTemperature;
	public int specialSettingsEcoTemperature;

	
	public float getComfortTemperature() {
		return comfortTemperature;
	}
	public float getEcoTemperature() {
		return ecoTemperatureV2;
	}
	public float getSetSingleSetpoint_Window_open() {
		return windowOpenTemperatureV2;
	}
	//public float getSetSingleSetpoint_Window_open() {
	//	return setSingleSetpoint_Window_open;
	//}
	public float getMinSetpointAuto() {
		return minSetpointAuto;
	}
	public float getMaxSetpointAuto() {
		return maxSetpointAuto;
	}
	public int getSpecialSettingsStartTime() {
		return specialSettingsStartTime;
	}
	public int getSpecialSettingsEndTime() {
		return specialSettingsEndTime;
	}
	public int getSpecialSettingsUsageTemperature() {
		return specialSettingsUsageTemperature;
	}
	public int getSpecialSettingsNonUsageTemperature() {
		return specialSettingsNonUsageTemperature;
	}
	public int getSpecialSettingsComfortTemperature() {
		return specialSettingsComfortTemperature;
	}
	public int getSpecialSettingsEcoTemperature() {
		return specialSettingsEcoTemperature;
	}
	public boolean getEcoEqualsOff() {
		return ecoEqualsOff;
	}
	
	public int getDayTypeSettingMode() {
		return dayTypeSettingMode;
	}
	
	public boolean getShowCalendarTemperature() {
		return showComfortTemperature;
	}
	
	public static int STARTEND_WORKINGDAY_IDX = 7;
	public static int STARTEND_WEEKEND_IDX = 8;
	public static int STARTEND_TIMES_SIZE = 9;
	public Map<Integer, List<Long>> startEndTimes = new HashMap<>();
	
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
	
	public long getStartTime() {
		List<Long> workingdayTime = startEndTimes.get(STARTEND_WORKINGDAY_IDX);
		if(workingdayTime == null || workingdayTime.isEmpty())
			return 1440*TimeProcUtil.MINUTE_MILLIS;
		return workingdayTime.get(0);
	}
	public long getEndTime() {
		List<Long> workingdayTime = startEndTimes.get(STARTEND_WORKINGDAY_IDX);
		if(workingdayTime == null || (workingdayTime.size() < 2))
			return 1440*TimeProcUtil.MINUTE_MILLIS;
		return workingdayTime.get(1);
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
