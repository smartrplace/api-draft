package org.ogema.accessadmin.api;

import org.ogema.accessadmin.api.SubcustomerUtil.SettingsBaseData;
import org.ogema.accessadmin.api.SubcustomerUtil.ValueVariance;

public class HeatCoolData extends SettingsBaseData {
	public float comfortTemperature;
	public float ecoTemperature;
	public float setSingleSetpoint_Window_open;
	/**Both minSetpoint and minSetpointAuto*/
	public float minSetpointAuto;
	public float maxSetpointAuto;
	
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
		return ecoTemperature;
	}
	public float getSetSingleSetpoint_Window_open() {
		return setSingleSetpoint_Window_open;
	}
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

}
