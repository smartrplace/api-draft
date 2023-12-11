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
package org.ogema.externalviewer.extensions;

import org.ogema.core.application.ApplicationManager;
import org.ogema.model.scheduleviewer.config.ScheduleViewerConfig;

import de.iwes.util.timer.AbsoluteTimeHelper;
import de.iwes.util.timer.AbsoluteTiming;
import de.iwes.widgets.html.schedulemanipulator.ScheduleManipulator;

/** Configuration of evaluation intervals*/
public class IntervalConfiguration {
	public long start = 0;
	public long end = 0;
	/** If multiStart is not null, also multiEnd must exist and have the same array length.
	 * In this case start and end are not used.
	 */
	public long[] multiStart = null;
	public long[] multiEnd = null;
	/** These suffixes will be checked when searching for non-existing file names for
	 * result creation
	 */
	public String[] multiFileSuffix = null;
	
    public static final String ALL_DATA = "All Data";
    public static final String ONE_DAY = "One Day";
    public static final String ONE_WEEK = "One Week";
    public static final String THREE_WEEKS = "Three Weeks";
    public static final String LAST_PLOT = "Last Chart Interval";
    public static final String[] OPTIONS = {ALL_DATA, ONE_DAY, ONE_WEEK, THREE_WEEKS, LAST_PLOT};

	public static IntervalConfiguration getDefaultDuration(String config, ApplicationManager appMan) {
    	switch(config) {
    	case ALL_DATA:
        	IntervalConfiguration r = new IntervalConfiguration();
			r.start = 0;
			//r.end = startEnd[1];
			r.end = appMan.getFrameworkTime();
        	return r;
		case ONE_DAY:
			r = new IntervalConfiguration();
			long now = appMan.getFrameworkTime();
			long startOfDay = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.DAY);
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startOfDay, -1, AbsoluteTiming.DAY);
			r.end = now;
			return r;
		case ONE_WEEK:
			r = new IntervalConfiguration();
			now = appMan.getFrameworkTime();
			startOfDay = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.DAY);
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startOfDay, -7, AbsoluteTiming.DAY);
			r.end = now;
			return r;
		case THREE_WEEKS:
			r = new IntervalConfiguration();
			now = appMan.getFrameworkTime();
			startOfDay = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.DAY);
			r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startOfDay, -21, AbsoluteTiming.DAY);
			r.end = now;
			return r;
		case LAST_PLOT:
			r = new IntervalConfiguration();
			if(ScheduleManipulator.lastPlotStart > 0) {
				r.start = ScheduleManipulator.lastPlotStart;
				r.end = ScheduleManipulator.lastPlotEnd;
			} else {
				now = appMan.getFrameworkTime();
				startOfDay = AbsoluteTimeHelper.getIntervalStart(now, AbsoluteTiming.DAY);
				r.start = AbsoluteTimeHelper.addIntervalsFromAlignedTime(startOfDay, -1, AbsoluteTiming.DAY);
				r.end = now;
			}
			return r;
    	}
    	return null;
    }

}

