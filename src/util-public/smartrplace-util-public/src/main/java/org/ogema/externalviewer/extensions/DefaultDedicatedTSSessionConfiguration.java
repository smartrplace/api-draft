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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.ogema.core.timeseries.ReadOnlyTimeSeries;

import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfiguration;
import de.iwes.widgets.reswidget.scheduleviewer.api.TimeSeriesFilter;
import de.iwes.widgets.reswidget.scheduleviewer.utils.DefaultSessionConfiguration;

public class DefaultDedicatedTSSessionConfiguration extends DefaultSessionConfiguration {
	protected final List<ReadOnlyTimeSeries> timeSeriesSelected;
	//protected final List<ReadOnlyTimeSeries> timeSeriesOffered;
	protected final ScheduleViewerConfiguration viewerConfiguration;

	public DefaultDedicatedTSSessionConfiguration(List<ReadOnlyTimeSeries> timeSeriesSelected,
			//List<ReadOnlyTimeSeries> timeSeriesOffered,
			ScheduleViewerConfiguration viewerConfiguration) {
		super();
		this.timeSeriesSelected = timeSeriesSelected;
		//this.timeSeriesOffered = timeSeriesOffered;
		this.viewerConfiguration = viewerConfiguration;
	}

	@Override
	public ScheduleViewerConfiguration viewerConfiguration() {
		if(viewerConfiguration != null)
			return viewerConfiguration;
		else return super.viewerConfiguration();
	}
	
	@Override
	public List<ReadOnlyTimeSeries> timeSeriesSelected() {
		return timeSeriesSelected;
	}

	/*@Override
	public List<ReadOnlyTimeSeries> timeSeriesOffered() {
		return timeSeriesOffered;
	}*/

	@Override
	public boolean overwritePrograms() {
		return true;
	}
	@Override
	public boolean overwriteDefaultTimeSeries() {
		return true;
	}

	//Pre-select first program by default
	@Override
	public List<Collection<TimeSeriesFilter>> programsPreselected() {
		if(viewerConfiguration != null && viewerConfiguration.getPrograms() != null && (!viewerConfiguration.getPrograms().isEmpty())) {
			List<Collection<TimeSeriesFilter>> result = new ArrayList<>();
			result.add(viewerConfiguration.getPrograms().get(0).values());
			return result;
		} else
			return super.programsPreselected();
	}
}
