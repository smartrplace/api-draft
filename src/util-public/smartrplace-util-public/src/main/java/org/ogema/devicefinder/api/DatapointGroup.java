package org.ogema.devicefinder.api;

import java.util.Collection;
import java.util.List;

import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatUtil;

import de.iwes.timeseries.eval.garo.api.base.GaRoMultiEvalDataProvider;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.template.LabelledItem;

/** Datapoint groups can be registered with an arbitraty ID, e.g. to register a plot of a set of datapoints.<br>
 * DatepointGroups are also used to represent devices. In this case the resource location of the device registered with {@link InstallAppDevice#device()}
 * is used as ID. The type of these groups shall be "DEVICE".*/
public interface DatapointGroup extends LabelledItem {
	public static final String DEFAULT_PLOT_CONFIG_PAGE = "DevicePlotPage";
	public static final String LOCAL_SHORT_NAME = "Local";
	
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
	
	Object getParameter(String id);
	void setParameter(String id, Object param);
	
	/** Groups can aggregate datapoints from several gateways. Still in many cases each group is
	 * assigned to a certain gateway. This method provides a standardized group id for a group
	 * on a remote gateway. For groups aggregating over several gateways a custom app specification
	 * has to be made
	 * @param localId
	 * @param gwId
	 * @return
	 */
	public static String getGroupIdForGw(String localId, String gwId) {
		if(gwId == null)
			return localId;
		String gwToUse = ViaHeartbeatUtil.getBaseGwId(gwId);
		return gwToUse+"::"+localId;
	}
	public static String[] getGroupIdAndGw(String groupId) {
		return getGroupIdAndGwForDp(groupId, LOCAL_SHORT_NAME);
	}
	public static String[] getGroupIdAndGwForDp(String groupId) {
		return getGroupIdAndGwForDp(groupId, GaRoMultiEvalDataProvider.LOCAL_GATEWAY_ID);
	}
	public static String[] getGroupIdAndGwForDp(String groupId, String localGwName) {
		String[] els = groupId.split("::");
		if(els.length == 1)
			return new String[] {groupId, LOCAL_SHORT_NAME};
		if(els.length == 2)
			return new String[]{els[1], els[0]};
		throw new IllegalStateException("GroupId cannot be split into gateway and local id: "+groupId);
	}
	public static class GroupGwResult {
		List<String> gwIdOptions;
		String localId;
	}
	public static GroupGwResult getGroupIdAndGwOptions(String groupId) {
		GroupGwResult result = new GroupGwResult();
		String[] baseResult = getGroupIdAndGw(groupId);
		result.localId = baseResult[1];
		result.gwIdOptions = ViaHeartbeatUtil.getAlternativeGwIds(baseResult[0]);
		return result;
	}
	
	/** Property names for DatapointGroups*/
	public static final String DEVICE_TYPE_SHORT_PARAM = "DeviceTypeShortId";
	public static final String DEVICE_TYPE_FULL_PARAM = "deviceType";
	public static final String DEVICE_UNIQUE_ID_PARAM = "deviceId";
	
	/** Values for {@link DatapointGroup#getType()}*/
	public static final String DEVICE = "DEVICE";
	public static final String DEVICE_TYPE = "DEVICE_TYPE";
}
