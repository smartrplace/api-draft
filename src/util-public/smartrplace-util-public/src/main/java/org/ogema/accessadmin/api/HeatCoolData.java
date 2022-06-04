package org.ogema.accessadmin.api;

import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.SubcustomerUtil.SettingsBaseData;
import org.ogema.accessadmin.api.SubcustomerUtil.ValueVariance;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;

public class HeatCoolData extends SettingsBaseData {
	public float comfortTemperature;
	public float ecoTemperatureV2;
	public float windowOpenTemperatureV2;

	public float ecoTemperatureV1;
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
		//TODO
		return false;
	}
	
	public static int STARTEND_WORKINGDAY_IDX = 7;
	public static int STARTEND_TIMES_SIZE = 8;
	public Map<Integer, List<Long>> startEndTimes;
	
	public List<Long> startEndTimesMonday() {
		return startEndTimes.get(0);
	}
	public List<Long> startEndTimesTuesday() {
		return startEndTimes.get(1);
	}
	public List<Long> startEndTimesWednesday() {
		return startEndTimes.get(2);
	}
	public List<Long> startEndTimesThursday() {
		return startEndTimes.get(3);
	}
	public List<Long> startEndTimesFriday() {
		return startEndTimes.get(4);
	}
	public List<Long> startEndTimesSaturday() {
		return startEndTimes.get(5);
	}
	public List<Long> startEndTimesSunday() {
		return startEndTimes.get(6);
	}
	public List<Long> startEndTimesWorkingday() {
		return startEndTimes.get(STARTEND_WORKINGDAY_IDX);
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
}
