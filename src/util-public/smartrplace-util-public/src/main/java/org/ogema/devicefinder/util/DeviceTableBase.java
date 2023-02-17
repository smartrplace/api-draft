package org.ogema.devicefinder.util;

import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.tissue.util.resource.GatewayUtil;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Header;

public abstract class DeviceTableBase extends DeviceTableRaw<InstallAppDevice,InstallAppDevice>  {

	/** Required to generate sample resource*/
	protected abstract Class<? extends Resource> getResourceType();
	
	protected final InstalledAppsSelector appSelector;
	protected final DeviceHandlerProvider<?> devHand;
	protected final TimeResource deviceIdManipulationUntil;
	protected volatile boolean isEmpty = true;
	
	public DeviceTableBase(WidgetPage<?> page, ApplicationManagerPlus appMan, Alert alert,
			InstalledAppsSelector appSelector,
			DeviceHandlerProvider<?> devHand) {
		this(page, appMan, alert, appSelector, devHand, false);
	}
	
	public DeviceTableBase(WidgetPage<?> page, ApplicationManagerPlus appMan, Alert alert,
			InstalledAppsSelector appSelector,
			DeviceHandlerProvider<?> devHand, boolean emptyStateControlledExternally) {
		super(page, appMan, alert, ResourceHelper.getSampleResource(InstallAppDevice.class), emptyStateControlledExternally);
		this.devHand = devHand;
		this.deviceIdManipulationUntil = appMan.getResourceAccess().getResource("hardwareInstallConfig/deviceIdManipulationUntil");
		if(appSelector != null)
			this.appSelector = appSelector;
		else if(this instanceof InstalledAppsSelector)
			this.appSelector = (InstalledAppsSelector) this;
		else
			throw new IllegalStateException("InstalledAppsSelector must be provided or implemented!");
	}

	@Override
	protected String getTableTitleRaw() {
		return devHand.getTableTitle();
	}
	
	protected void addHomematicCCUId(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan,
			Room deviceRoom) {
		if(req != null) {
			String text = getHomematicCCUId(object.device().getLocation());
			vh.stringLabel("RT", id, text, row);
		} else
			vh.registerHeaderEntry("RT");
	}
	
	public Resource addNameWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		return addNameWidget(object, vh, id, req, row, appMan, false);
	}
	public Resource addNameWidget(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice,InstallAppDevice> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan, boolean showOnlyBaseCols) {
		
		final String name;
        final String deviceId;
		if(req == null) {
			name = "initResName"; //ResourceHelper.getSampleResource(getResourceType());
            deviceId = "initId";
        }
		else {
			name = getName(object, appManPlus);
            if (object.deviceId().exists())
                deviceId = object.deviceId().getValue();
            else
                deviceId = "n/a";
        }
		if(!showOnlyBaseCols)
			vh.stringLabel("Name", id, name, row);
		boolean provideEdit = false;
		if(deviceIdManipulationUntil != null && (deviceIdManipulationUntil.getValue() != 0)) {
			long now = appMan.getFrameworkTime();
			if(deviceIdManipulationUntil.getValue() < now)
				deviceIdManipulationUntil.setValue(0);
			else
				provideEdit = true;
		}
		if(provideEdit)
			vh.stringEdit("ID", id, object.deviceId(), row, null);
		else
			vh.stringLabel("ID", id, deviceId, row);
		
		final Resource device;
		if(req == null)
			device = ResourceHelper.getSampleResource(getResourceType());
		else
			device = object.device();
		return device;
	}

	@Override
	public List<InstallAppDevice> getObjectsInTable(OgemaHttpRequest req) {
		List<InstallAppDevice> all = appSelector.getDevicesSelected(devHand, req);
		return all;
		/*List<InstallAppDevice> result = new ArrayList<InstallAppDevice>();
		@SuppressWarnings({ "rawtypes", "unchecked" })
		List<ResourcePattern<?>> allPatterns = (List)devHand.getAllPatterns();
		for(InstallAppDevice dev: all) {
			for(ResourcePattern<?> pat: allPatterns) {
				if(pat.model.equalsLocation(dev.device())) {
					result.add(dev);
					break;
				}
			}
			//if(getResourceType().isAssignableFrom(dev.device().getResourceType())) {
			//	result.add(dev);
			//}
		}
		return result;*/
	}
	
	@Override
	public InstallAppDevice getResource(InstallAppDevice object, OgemaHttpRequest req) {
		return object;
	}
	
	public static String getHomematicCCUId(String location) {
		location = makeDeviceToplevel(location);
		String[] parts = location.split("/");
		String tail;
		if(parts[0].toLowerCase().startsWith("homematicip")) {
			tail = parts[0].substring("homematicip".length());
		} else if(parts[0].startsWith("homematic")) {
			tail = parts[0].substring("homematic".length());			
		} else if(parts[0].startsWith("hm")){
			tail = parts[0].substring("hm".length());						
		} else {
			tail = parts[0];						
		}
		if(tail.isEmpty()) return "n/a";
		else return tail;
	}
	
	public static boolean isHomematic(String location) {
		return (getHomematicType(location) > 0);
	}
	
	/** Types:
	 * 0 : No homematic
	 * 1 : via homegear
	 * 2: via CCU classic
	 * 3: via CCU homematicIP
	 */
	public static int getHomematicType(String location) {
		location = makeDeviceToplevel(location);
		String loclow = location.toLowerCase();
		if(loclow.startsWith("homematichg"))
			return 1;
		if(loclow.contains("homematic") || loclow.startsWith("hm")) {
			if(location.contains("_cc"))
				return 2;
			return 3;
		}
		return 0;
	}
	
	public static String makeDeviceToplevel(String deviceLocation) {
		if(!Boolean.getBoolean("org.ogema.devicefinder.util.supportcascadedccu"))
			return deviceLocation;
		if(!deviceLocation.startsWith("gw"))
			return deviceLocation;
		int firstDel = deviceLocation.indexOf('/');
		// substring(2, firstDel) must return at least 4 characters for a valid gatewayID
		if(firstDel < 6 || firstDel == (deviceLocation.length()-1))
			return deviceLocation;
		String gatewayId = deviceLocation.substring(2, firstDel);
		if(gatewayId.length() > GatewayUtil.GATWAYID_MAX_LENGTH)
			return deviceLocation;
		String result = deviceLocation.substring(firstDel+1);
		return result;
	}
	
	public DynamicTable<InstallAppDevice> getMainTable() {
		return mainTable;
	}
	
	public Header getHeaderWidget() {
		return headerWinSens;
	}
	
	public void setEmpty(boolean empty) {
		this.isEmpty = empty;
	}
	
	/**
	 * Only relevant if constructor parameter emptyControlledExternally is set to true
	 */
	@Override
	protected boolean isEmpty(OgemaHttpRequest req) {
		return this.isEmpty;
	}
	
	@Override
	public String getLineId(InstallAppDevice object) {
		if(appSelector.sortByRoom()) {
			Room room = ResourceUtils.getDeviceLocationRoom(object.device());
			if(room != null) {
				return "_"+ResourceUtils.getHumanReadableShortName(room)+super.getLineId(object);				
			}
		}
		return object.deviceId().getValue()+super.getLineId(object);
	}
}
