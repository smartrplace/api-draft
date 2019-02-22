package org.smartrplace.smarteff.access.api;

import java.util.List;

import org.ogema.externalviewer.extensions.IntervalConfiguration;

import de.iwes.timeseries.eval.api.TimeSeriesData;
import de.iwes.timeseries.eval.api.extended.util.TimeSeriesDataExtendedImpl;
import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;

public interface EvalButtonConfig {
	/** Text to show on button - keep short!*/
	String buttonText();
	/** If label / description is null it shall be replaced by default. Usually this should be
	 * an instance of {@link TimeSeriesDataImpl} oder {@link TimeSeriesDataExtendedImpl}*/
	List<TimeSeriesData> timeSeriesToOpen();

	/** If null the previous day and the current day up to now shall be shown*/
	IntervalConfiguration getDefaultInterval();
}
