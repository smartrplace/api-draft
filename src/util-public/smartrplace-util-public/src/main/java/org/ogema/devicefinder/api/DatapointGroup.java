package org.ogema.devicefinder.api;

import java.util.Collection;
import java.util.List;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.template.LabelledItem;

public interface DatapointGroup extends LabelledItem {
	public static final String DEFAULT_PLOT_CONFIG_PAGE = "DevicePlotPage";
	/** Set label for the group that is used as label for the chart if a
	 * chart is configured
	 * @param locale if null the {@link OgemaLocale#ENGLISH} is used. This will also be used if
	 * 		{@link #label(OgemaLocale)}(null) is called
	 */
	void setLabel(OgemaLocale locale, String label);
	
	/** Datagroup types shall be defined in the future. For now the type Strings can be
	 * freely defined by applications
	 */
	String getType();
	boolean setType(String type);
	
	List<Datapoint> getAllDatapoints();
	boolean addDatapoint(Datapoint dp);
	void addAll(Collection<Datapoint> datapoints);
	
	/** Register the group to represent a chart option
	 * 
	 * @param cofigurationPage may be null. Shall define a chart configuration page on which the plot shall
	 * 		appear. For now just the default page is supported.
	 * @return
	 */
	boolean registerAsChart(String configurationPage);
	
	List<String> getChartConfigPagesRegistered();
}
