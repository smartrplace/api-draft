package org.ogema.devicefinder.api;

import java.util.Collection;
import java.util.List;

import org.smartrplace.apps.hw.install.config.InstallAppDevice;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.template.LabelledItem;

/** Datapoint groups can be registered with an arbitraty ID, e.g. to register a plot of a set of datapoints.<br>
 * DatepointGroups are also used to represent devices. In this case the resource location of the device registered with {@link InstallAppDevice#device()}
 * is used as ID. The type of these groups shall be "DEVICE".*/
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
	
	List<DatapointGroup> getSubGroups();
	boolean addSubGroup(DatapointGroup dpGrp);
	boolean removeSubGroup(DatapointGroup dpGrp);
	DatapointGroup getSubGroup(String id);
}
