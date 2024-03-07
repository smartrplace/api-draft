package org.ogema.accessadmin.api;

import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.SubcustomerUtil.ValueVariance;
import org.ogema.timeseries.eval.simple.api.TimeProcUtil;

public class HeatCoolData extends UsageData {

	@Deprecated //should not be used anymore, writing to old-style interval-based window temperature
	public float setSingleSetpoint_Window_open;
	
	public Boolean ecoEqualsOff;
	public Integer dayTypeSettingMode;
	
	public ValueVariance[] valueEval;
	
	public float comfortTemperature;
	public float ecoTemperatureV2;
	public float windowOpenTemperatureV2;

	/**Both minSetpoint and minSetpointAuto*/
	public float minSetpointAuto;
	public float maxSetpointAuto;
	
	public float getComfortTemperature() {
		return comfortTemperature;
	}
	public float getEcoTemperature() {
		return ecoTemperatureV2;
	}
	public float getSetSingleSetpoint_Window_open() {
		return windowOpenTemperatureV2;
	}

	public float getMinSetpointAuto() {
		return minSetpointAuto;
	}
	public float getMaxSetpointAuto() {
		return maxSetpointAuto;
	}
	
	public float getUsageTemperature() {
		return usageTemperature;
	}
	public float getNonUsageTemperature() {
		return nonUsageTemperature;
	}		

	public long getStartTime() {
		return SubcustomerUtil.getStartTimeStatic(startEndTimes);
	}

	public long getEndTime() {
		return SubcustomerUtil.getEndTimeStatic(startEndTimes);
	}
	
	
	/** Number of rooms with special day settings*/ 
	public int specialDaySettingsRooms = 0;
	/** Number of rooms with nonworking-day temperature unequal to eco-tem*/ 
	public int specialNonWorkingRooms = 0;
	/** Number of rooms with that cannot be mapped to standard start/end setpoint curve characteristics*/
	public int specialStructureRooms = 0;
	
	public boolean showComfortTemperature = false;
	public boolean showEcoTemperature = false;
	
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
	
	public boolean getShowEcoTemperature() {
		return showEcoTemperature;
	}
}
