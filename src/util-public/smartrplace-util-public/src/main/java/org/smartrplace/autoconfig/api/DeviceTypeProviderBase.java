package org.smartrplace.autoconfig.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.tools.resource.util.ResourceUtils;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** Standard template for device type providers generating a device resource in a top-level resource list*/
public abstract class DeviceTypeProviderBase<T extends Resource> implements DeviceTypeProvider<T> {
	protected final ApplicationManager appMan;
	protected final String label;
	protected final Class<T> deviceType;
	protected final String topResourceListName;
	protected final String configPlaceholder;
	
	protected ResourceList<T> deviceListIfFound = null;

	protected abstract String getNewResourceName(DeviceTypeConfigData<T> configData);
	/**
	 * 
	 * @param configData
	 * @return null on success or error message
	 */
	protected abstract String configureResource(DeviceTypeConfigData<T> configData);
	protected abstract DeviceTypeConfigData<T> getKnownConfig(T device);

	public DeviceTypeProviderBase(Class<T> deviceType, String topResourceListName, ApplicationManager appMan) {
		this(null, deviceType, topResourceListName, appMan);
	}
	public DeviceTypeProviderBase(String label, Class<T> deviceType, String topResourceListName,
			ApplicationManager appMan) {
		this(label, deviceType, topResourceListName, null, appMan);
	}
	public DeviceTypeProviderBase(String label, Class<T> deviceType, String topResourceListName,
			String configPlaceholder,
			ApplicationManager appMan) {
		this.appMan = appMan;
		this.label = label;
		this.deviceType = deviceType;
		this.topResourceListName = topResourceListName;
		this.configPlaceholder = configPlaceholder;
		
		deviceListIfFound = appMan.getResourceAccess().getResource(topResourceListName);
		if(deviceListIfFound != null && deviceListIfFound.size() == 0)
			deviceListIfFound.delete();
	}

	@Override
	public String id() {
		return this.getClass().getName();
	}

	@Override
	public String label(OgemaLocale arg0) {
		return label;
	}

	@Override
	public Class<T> getDeviceType() {
		return deviceType;
	}

	@SuppressWarnings("unchecked")
	@Override
	public CreateAndConfigureResult<T> addAndConfigureDevice(
			DeviceTypeConfigData<T> configData) {
		
		CreateAndConfigureResult<T> result = new CreateAndConfigureResult<T>();

		if(configData.address.isEmpty()) {
			result.resultMessage = "Empty address cannot be processed!";
			return result;
		}
		
		checkConfiguration(configData);

		if(configData.governingResource == null) {
			String name = getNewResourceName(configData);
			deviceListIfFound = appMan.getResourceManagement().createResource(topResourceListName, ResourceList.class);
			if(deviceListIfFound.getElementType() == null)
				deviceListIfFound.setElementType(deviceType);
			configData.governingResource = deviceListIfFound.addDecorator(ResourceUtils.getValidResourceName(name), deviceType);
		}
		String message = configureResource(configData);
		configData.governingResource.activate(true);
		
		result.resultConfig = configData;
		result.resultMessage = "Created device on "+configData.governingResource.getLocation();
		return result ;
	}

	@Override
	public Collection<DeviceTypeConfigData<T>> getKnownConfigs() {
		List<DeviceTypeConfigData<T>> result = new ArrayList<>();
		if(deviceListIfFound == null)
			deviceListIfFound = appMan.getResourceAccess().getResource(topResourceListName);
		if(deviceListIfFound == null)
			return Collections.emptyList();
		List<T> allRes = deviceListIfFound.getAllElements();
		for(T con: allRes) {
			DeviceTypeConfigData<T> config = getKnownConfig(con);
			config.governingResource = con;
			config.dtbProvider = this;
			result.add(config);
		}
		return result;
	}

	@Override
	public boolean deleteConfig(DeviceTypeConfigData<T> configData) {
		configData.governingResource.delete();
		return true;
	}

	@Override
	public DeviceTypeConfigDataBase getPlaceHolderData() {
		return new DeviceTypeConfigDataBase("192.168.0.99", null, configPlaceholder);
	}	

}
