package org.smartrplace.tissue.util.resource;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.model.locations.Location;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.slf4j.LoggerFactory;

import de.iwes.util.logconfig.LogHelper;
import de.iwes.util.resource.ResourceHelper;

/** Intended to move into {@link ResourceHelper} in the future
 *
 */
public class ResourceHelperSP {
	public static class DeviceInfo {
		String deviceName;
		String deviceResourceLocation;
		Class<? extends PhysicalElement> deviceType;
		Location deviceLocation;
		
		
		// getters and setters
		public String getDeviceName() {
			return deviceName;
		}
		public void setDeviceName(String deviceName) {
			this.deviceName = deviceName;
		}
		public String getDeviceResourceLocation() {
			return deviceResourceLocation;
		}
		public void setDeviceResourceLocation(String deviceResourceLocation) {
			this.deviceResourceLocation = deviceResourceLocation;
		}
		public Class<? extends PhysicalElement> getDeviceType() {
			return deviceType;
		}
		public void setDeviceType(Class<? extends PhysicalElement> deviceType) {
			this.deviceType = deviceType;
		}
		public Location getDeviceLocation() {
			return deviceLocation;
		}
		public void setDeviceLocation(Location deviceLocation) {
			this.deviceLocation = deviceLocation;
		}
	}

	public static DeviceInfo getDeviceInformation(Resource subResource) {
		return getDeviceInformation(subResource, true);
	}
	public static DeviceInfo getDeviceInformation(Resource subResource, boolean locationRelevant) {
		return AccessController.doPrivileged(new PrivilegedAction<DeviceInfo>() {
			@SuppressWarnings("unchecked")
			public DeviceInfo run() {
				ApplicationManager appManPriv = UtilExtendedApp.getApplicationManager();
				final Resource inputResourceToUse;
				if(appManPriv != null) {
					inputResourceToUse = appManPriv.getResourceAccess().getResource(subResource.getPath());
				} else {
					inputResourceToUse = subResource;
					LoggerFactory.getLogger("UtilExtended").warn("Could not use Application Manager of util-extended for resource permissions");
				}
				PhysicalElement device = LogHelper.getDeviceResource(inputResourceToUse, locationRelevant);
				if(device == null) return null;
				DeviceInfo result = new DeviceInfo();
				result.deviceName = ResourceUtils.getHumanReadableShortName(device);
				result.deviceType = (Class<? extends PhysicalElement>) device.getResourceType();
				result.deviceLocation = device.location();
				result.deviceResourceLocation = device.getLocation();
				return result;			}
		});		
	}

}
