package org.smartrplace.alarming.check;

public class IssueAnalysisResultBase {
	/** High level generic category*/
	public static enum AlarmCheckCategory {
		NONE,
		SMART_ALARM,
		SETP_REACT,
		THERMOSTAT_OTHER,
		BATTERY,
		CCU,
		OTHER
	}

	/** Optional **/
	public String generalResultMessage = null;
	
	/** Requires manual analysis to proceed:<br> 
	 * If true the following fields may not be set if the
	 * respective analysis could not be done automatically.*/
	public boolean requestHumanAnaylsis = false;

	public AlarmCheckCategory highLevelCategory = AlarmCheckCategory.NONE;
}
