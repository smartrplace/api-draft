package org.smartrplace.tissue.util.format;

/** Content here may be moved to {@link de.iwes.util.format.StringFormatHelper} in the future
 */
public class StringFormatHelperSP {
	@Deprecated
	/** Use {@link de.iwes.util.format.StringFormatHelper#getFormattedTimeOfDay}*
	 */
	public static String getFormattedTimeOfDay(long timeOfDay) {
		return getFormattedTimeOfDay(timeOfDay, false);
	}

	/**Get string representation for a time value relative to the beginning of a day
	 * @param timeOfDay time compared to beginning of a day in milliseconds
	 * @param printSeconds if true also seconds of minute will be included into
	 * 		the result
	 * @return String representation in the format HH:MM or HH:MM:SS
	 */
	public static String getFormattedTimeOfDay(long timeOfDay, boolean printSeconds) {
    	if(timeOfDay < 0) {
    		return "--";
    	}
    	long hours = timeOfDay / (60*60000);
    	long minutes = (timeOfDay  - hours*(60*60000))/(60000);
    	if(printSeconds) {
    		long seconds = (timeOfDay  - hours*(60*60000) - minutes*60000)/(1000);
       		return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    	}
   		return String.format("%02d:%02d", hours, minutes);
	}

}
