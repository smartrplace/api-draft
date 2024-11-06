/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartrplace.tissue.util.format;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.List;

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

	/**Convert interval duration to a flexible string giving seconds, minutes, hours, days,
	 * 		months or years
	 * as the most readable choice
	 * @param deltaT interval duration in milliseconds
	 * @param switchLimit maximum number of seconds, minutes, hours, days etc. shown before
	 * 		switching to the next higher unit type. Default value is 100, which limits the
	 * 		number of digits to 2 (except for very large numbers). For edit fields a higher value
	 * 		(e.g. 360) is recommended.
	 * @return a one or two digit value plus the time unit chosen by the method
	 */
	public static String getFormattedValue(long deltaT, int switchLimit) {
    	if(deltaT < 0) {
    		return "--";
    	}
		deltaT = deltaT / 1000;
		if(deltaT < switchLimit) {
			return String.format("%d sec", deltaT);
		}
		deltaT /= 60;
		if(deltaT < switchLimit) {
			return String.format("%d min", deltaT);
		}
		deltaT /= 60;
		if(deltaT < switchLimit) {
			return String.format("%d h", deltaT);
		}
		deltaT /= 24;
		if(deltaT < switchLimit) {
			return String.format("%d d", deltaT);
		}
		float deltaTf = deltaT / (365.25f/12f);
		if(deltaTf < switchLimit) {
			return String.format("%d month", Math.round(deltaTf));
		}
		deltaTf /= 12;
		if(deltaTf < 100) {
			return String.format("%d a", Math.round(deltaTf));
		}
		return (">99a");
	}

	public static String list2string(List<?> data, int maxEl) {
		String result = "#:"+data.size();
		if(!result.isEmpty() && maxEl > 0)
			result += " [0]:"+data.get(0).toString();
		int limit = Math.min(data.size(), maxEl);
		for(int i=1; i<limit; i++) {
			result += ",["+i+"]:"+data.get(i).toString();			
		}
		return result;
	}

	public static String getCamelCase(String init) {
		if (init == null)
			return null;

		final StringBuilder ret1 = new StringBuilder(init.length());

		for (final String word : init.split(" ")) {
			if (!word.isEmpty()) {
				ret1.append(Character.toUpperCase(word.charAt(0)));
				ret1.append(word.substring(1).toLowerCase());
			}
		}

		String retval1 = ret1.toString();
		final StringBuilder ret = new StringBuilder(retval1.length());
		for (final String word : init.split("_")) {
			if (!word.isEmpty()) {
				ret.append(Character.toUpperCase(word.charAt(0)));
				ret.append(word.substring(1).toLowerCase());
			}
		}
		return ret1.toString();
	}
	
	public static String getTimeDateInLocalTimeZoneMinutes(long millisUTCSinceEpoc) {
		//Date date = new Date(millisUTCSinceEpoc-100);
		DateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");
		return formatter.format(millisUTCSinceEpoc);
	}
}
