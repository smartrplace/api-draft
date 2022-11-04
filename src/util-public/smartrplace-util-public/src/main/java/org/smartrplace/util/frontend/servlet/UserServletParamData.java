package org.smartrplace.util.frontend.servlet;

import java.util.Map;

import org.ogema.core.timeseries.ReadOnlyTimeSeries;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** General parameter information required by various ServletValueProviders*/
public class UserServletParamData {
	public UserServletParamData() {
	}
	public UserServletParamData(Map<String, String[]> paramMap) {
		this.paramMap = paramMap;
		suppressNan = UserServletUtil.suppressNan(paramMap);
		//String structureStr = UserServlet.getParameter("structure", paramMap);
		//if(structureStr != null && structureStr.equals("list"))
		if(isStructureList(paramMap))
			structureList = true;
		provideExtended = UserServlet.getBoolean("extendedData", paramMap);
		timeString = UserServlet.getParameter("time", paramMap);
		// supported values: de=de, en=en, fr=fr, zh=zh
		String localStr = UserServlet.getParameter("locale", paramMap);
		if(localStr != null) for(OgemaLocale lo: OgemaLocale.getAllLocales()) {
			if(localStr.equals(lo.getLanguage())) {
				locale = lo;
				break;
			}
		}
	}
	public UserServletParamData(Map<String, String[]> paramMap, Boolean hasWritePermission) {
		this(paramMap);
		this.hasWritePermission = hasWritePermission;
	}

	public Map<String, String[]> paramMap;
	public boolean suppressNan = false;
	public String timeString;

	
	/** If true then extended information like time series and user write permission is added to the result
	 * if supported by the ServletValueProvider
	 */
	public boolean provideExtended;
	public boolean structureList;
	public OgemaLocale locale = OgemaLocale.ENGLISH;
	
	/** Either a TimeSeriesDataImpl can be provided or a ReadOnlyTimeSeries*/
	public TimeSeriesDataImpl tsData = null;
	public ReadOnlyTimeSeries tsDataRaw = null;
	//public Map<String, TimeSeriesDataImpl> tsMap = null;
	public String tsLocationOrBaseId = null;
	
	public Boolean hasWritePermission = null;
	
	public static boolean isStructureList(Map<String, String[]> paramMap) {
		String structureStr = UserServlet.getParameter("structure", paramMap);
		if(structureStr != null && structureStr.equals("list"))
			return true;
		return false;
	}
}
