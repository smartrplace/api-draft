package org.smartrplace.util.frontend.servlet;

import java.util.Map;

import org.ogema.core.timeseries.ReadOnlyTimeSeries;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;

/** General parameter information required by various ServletValueProviders*/
public class UserServletParamData {
	public UserServletParamData() {
	}
	public UserServletParamData(Map<String, String[]> paramMap) {
		this.paramMap = paramMap;
		suppressNan = UserServletUtil.suppressNan(paramMap);
		provideExtended = UserServlet.getBoolean("extendedData", paramMap);
	}
	public UserServletParamData(Map<String, String[]> paramMap, Boolean hasWritePermission) {
		this(paramMap);
		this.hasWritePermission = hasWritePermission;
	}
	public UserServletParamData(Map<String, String[]> paramMap, Boolean hasWritePermission,
			Map<String, TimeSeriesDataImpl> tsMap) {
		this(paramMap);
		this.hasWritePermission = hasWritePermission;
		this.tsMap = tsMap;
	}

	public Map<String, String[]> paramMap;
	public boolean suppressNan = false;
	
	/** If true then extended information like time series and user write permission is added to the result
	 * if supported by the ServletValueProvider
	 */
	public boolean provideExtended;
	
	/** Either a TimeSeriesDataImpl can be provided or a ReadOnlyTimeSeries*/
	public TimeSeriesDataImpl tsData = null;
	public ReadOnlyTimeSeries tsDataRaw = null;
	public Map<String, TimeSeriesDataImpl> tsMap = null;
	public String tsLocationOrBaseId = null;
	
	public Boolean hasWritePermission = null;
}
