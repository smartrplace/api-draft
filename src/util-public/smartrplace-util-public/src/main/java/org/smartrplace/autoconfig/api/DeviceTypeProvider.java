package org.smartrplace.autoconfig.api;

import java.util.Collection;

import org.ogema.core.model.Resource;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.template.LabelledItem;

public interface DeviceTypeProvider<T extends Resource>  extends LabelledItem {
	/** Type of governing resource (see {@link #addAndConfigureDevice(String, String, Resource)})
	 * 
	 * @return
	 */
	Class<T> getDeviceType();
	
	/** Get human readable description of the configuration String that can be provided via web interface
	 *  
	 * @return if null then no configuration can be given by the user
	 */
	@Override
	String description(OgemaLocale locale);
	
	/** Check configuration without actually performing the resource creation and configuration. Note that this 
	 * does not have to be implemented by all providers
	 * @param <T>
	 * @param address
	 * @param configuration
	 * @param governingResource
	 * @return
	 */
	default String checkConfiguration(DeviceTypeConfigData<T> configData) {
		return "No testing supported!";
	}
	
	public static class CreateAndConfigureResult<T extends Resource> {
		public DeviceTypeConfigData<T> resultConfig;
		public String resultMessage;
	}
	public static class DeviceTypeConfigDataBase {
		public DeviceTypeConfigDataBase(String address, String password, String configuration) {
			this.address = address;
			this.password = password;
			this.configuration = configuration;
		}
		public String address;
		public String password;
		public String configuration;		
	}
	public static class DeviceTypeConfigData<T extends Resource> extends DeviceTypeConfigDataBase {
		public DeviceTypeConfigData(String address, String password, String configuration) {
			super(address, password, configuration);
		}

		/** The governing resource
	    * 	may e.g. be the device created or a ValueResource to which a communication decorator shall be added
	 	*/
		public T governingResource;
		
		/** The provider shall add a reference to itself*/
		public DeviceTypeProvider<T> dtbProvider;
	}
	/** Create device if not yet existing and configure it
	 * 
	 * @param <T>
	 * @param configData: Non-required data may be null; if governingResource is null then the resource location shall be chosen automatically.
	 * @return may be the same object as input (which content may have changed) or may be new object
	 */
	CreateAndConfigureResult<T> addAndConfigureDevice(DeviceTypeConfigData<T> configData);
	
	Collection<DeviceTypeConfigData<T>> getKnownConfigs();
	
	boolean deleteConfig(DeviceTypeConfigData<T> configData);
	
	DeviceTypeConfigDataBase getPlaceHolderData();
}
