package org.smartrplace.autoconfig.api;

import java.util.ArrayList;
import java.util.Collection;
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
	protected final ResourceList<T> deviceList;
	protected final String configPlaceholder;
	
	protected abstract String getNewResourceName(DeviceTypeConfigData<T> configData);
	protected abstract void configureResource(DeviceTypeConfigData<T> configData);
	protected abstract DeviceTypeConfigData<T> getKnownConfig(T device);

	public DeviceTypeProviderBase(Class<T> deviceType, ResourceList<T> deviceList, ApplicationManager appMan) {
		this(null, deviceType, deviceList, appMan);
	}
	public DeviceTypeProviderBase(String label, Class<T> deviceType, ResourceList<T> deviceList,
			ApplicationManager appMan) {
		this(label, deviceType, deviceList, null, appMan);
	}
	@SuppressWarnings("unchecked")
	public DeviceTypeProviderBase(String label, Class<T> deviceType, String topResourceListName,
			String configPlaceholder,
			ApplicationManager appMan) {
		this(label, deviceType,
				appMan.getResourceManagement().createResource(topResourceListName, ResourceList.class),
				configPlaceholder, appMan);
	}
	public DeviceTypeProviderBase(String label, Class<T> deviceType, ResourceList<T> deviceList,
			String configPlaceholder,
			ApplicationManager appMan) {
		this.appMan = appMan;
		this.label = label;
		this.deviceType = deviceType;
		this.deviceList = deviceList;
		this.configPlaceholder = configPlaceholder;
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
			deviceList.create();
			if(deviceList.getElementType() == null)
				deviceList.setElementType(deviceType);
			configData.governingResource = deviceList.addDecorator(ResourceUtils.getValidResourceName(name), deviceType);
		}
		configureResource(configData);
		configData.governingResource.activate(true);
		
		result.resultConfig = configData;
		result.resultMessage = "Created device on "+configData.governingResource.getLocation();
		return result ;
	}

	@Override
	public Collection<DeviceTypeConfigData<T>> getKnownConfigs() {
		List<DeviceTypeConfigData<T>> result = new ArrayList<>();
		List<T> allRes = deviceList.getAllElements();
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
