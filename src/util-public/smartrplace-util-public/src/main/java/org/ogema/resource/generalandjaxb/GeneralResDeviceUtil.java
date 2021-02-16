package org.ogema.resource.generalandjaxb;

import org.ogema.core.model.Resource;
import org.ogema.model.prototypes.PhysicalElement;

import de.iwes.util.resource.ResourceHelper;

/** Same functionality as JAXBResDeviceUtil, but for normal resources*/
public class GeneralResDeviceUtil {
	/** Get a device that is a super-resource of res that contains room information.
	 *  Get device with room if possible, other device without room. It two results are possible return most top-level device
	 *  as we usually do not want to separate into sub-devices.
	 * @param res
	 * @return
	 */
	public static PhysicalElement getDeviceWithRoomForResource(Resource res) {
		String path = res.getLocation();
		PhysicalElement result = null;
		PhysicalElement device = ResourceHelper.getFirstParentOfType(res, PhysicalElement.class);
		boolean initDone = false;
		while(device != null) {
			if(initDone) {
				device = ResourceHelper.getFirstParentOfType(device, PhysicalElement.class);
				if(device == null)
					break;
			} else
				initDone = true;
			if(!device.location().room().exists()) {
				continue;
			}
			String egetKey = device.getLocation();
			if((path.length() > egetKey.length()) && (path.charAt(egetKey.length()-1) != '/') && (path.charAt(egetKey.length()) != '/'))
				continue;
			if(result == null || result.getPath().length() > egetKey.length())
				result = device;
		}
		if(result != null)
			return result;
		device = ResourceHelper.getFirstParentOfType(res, PhysicalElement.class);
		initDone = false;
		while(device != null) {
			if(initDone) {
				device = ResourceHelper.getFirstParentOfType(device, PhysicalElement.class);
				if(device == null)
					break;
			} else
				initDone = true;
			String egetKey = device.getLocation();
			if((path.length() > egetKey.length()) && (path.charAt(egetKey.length()-1) != '/') && (path.charAt(egetKey.length()) != '/'))
				continue;
			if(result == null || result.getPath().length() > egetKey.length())
				result = device;
		}
		return result;
	}

}
