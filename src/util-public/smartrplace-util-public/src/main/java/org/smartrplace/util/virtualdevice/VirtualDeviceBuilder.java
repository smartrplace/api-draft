package org.smartrplace.util.virtualdevice;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.smartrplace.tissue.util.resource.ResourceHelperSP;

import de.iwes.util.resource.ResourceHelper;

/** A virtual device builder uses existing devices that usually receive data from a hardware driver.
 * The VirtualDeviceBuilder checks for the input devices on startup, but does not register a
 * pattern listener for performance reasons. The input devices are not specified as resource types,
 * but as paths with types, so this model is intended for virtual devices on specific installations.
 * The output device is created by the VirtualDeviceBuilder.<br>
 * By using path and type for the definition of input resources we avoid the creation of
 * unnecessary virtual resources on systems where the virtual device cannot be generated.
 */
public abstract class VirtualDeviceBuilder<T extends Resource> {
	
	/** Implementation shall provide paths for input devices here*/
	protected abstract String[] getInputDevicePaths();
	
	/** Implementation shall provide input device types here in the same order as {@link #getInputDevicePaths()}
	 */
	protected abstract List<Class<? extends Resource>> getInputDeviceTypes();
	
	protected abstract Class<T> getVirtualDeviceType();
	
	/** Implementation can provide logic for virtual device here, e.g. registration of value listeners
	 * on input devices and handling of new values
	 * @param virtualDevice virtualDevice created
	 * @param inputDevices instances of input devices found
	 */
	protected abstract void virtualDeviceAvailable(T virtualDevice, List<Resource> inputDevices);
	

	/** The virtualDevice is created as soon as all input devices are available*/
	protected T virtualDevice;
	private final ApplicationManager appMan;
	
	public VirtualDeviceBuilder(ApplicationManager appMan) {
		this.appMan = appMan;
		
		String[] inpStrs = getInputDevicePaths();
		List<Class<? extends Resource>> inpTypes = getInputDeviceTypes();
		if(inpStrs.length != inpTypes.size()) {
			throw new IllegalArgumentException("Input paths and types size differ:"+inpStrs.length+" / "+inpTypes.size());
		}
		for(int i=0; i<inpStrs.length; i++) {
			inputList.add(new InputDevices(inpStrs[i], inpTypes.get(i)));
		}
		checkInput();
		//TODO: Create timer for additional checks if also new resources shall be discovered during runtime
	}

	protected class InputDevices {
		public final String path;
		public final Class<? extends Resource> type;
		
		// The device field is set as soon as the device is found
		public Resource device = null;
		
		public InputDevices(String path, Class<? extends Resource> type) {
			this.path = path;
			this.type = type;
		}
	}
	protected final List<InputDevices> inputList = new ArrayList<>();
	
	/** Check if virtual device is available
	 * @return true if all input was found and the virtualDevice is newly available (callback is
	 * initiated in this case by the method)
	 */
	protected boolean checkInput() {
		if(virtualDevice != null) return false;
		for(InputDevices inp: inputList) {
			if(inp.device != null) continue;
			inp.device = ResourceHelperSP.getSubResource(null, inp.path, inp.type, appMan.getResourceAccess());
			if(inp.device == null) return false;
		}
		
		//virtualDevice available
		virtualDevice = ResourceHelper.getSampleResource(getVirtualDeviceType());
		List<Resource> inputDevices = new ArrayList<>();
		for(InputDevices inp: inputList) {
			inputDevices.add(inp.device);
		}
		virtualDeviceAvailable(virtualDevice, inputDevices );
		return true;
	}
}
