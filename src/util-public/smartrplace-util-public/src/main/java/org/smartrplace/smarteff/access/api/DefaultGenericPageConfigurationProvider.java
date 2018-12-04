package org.smartrplace.smarteff.access.api;

import java.util.Map;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public abstract class DefaultGenericPageConfigurationProvider implements GenericPageConfigurationProvider {

	public static final int MAX_ID = 100;

	protected abstract Map<String, ConfigInfoExt> configs();
	protected abstract String getNextId();

	/** Add this to implementation class*/
	/*public static DefaultGenericPageConfigurationProvider getInstance() {
		if(instance == null) instance = MyGenericPageConfigurationProvider();
		return instance;
	}*/
	
	public String addConfig(ConfigInfoExt sc) {
		String id = getNextId();
		configs().put(id, sc);
		return id;
	}

	protected static volatile DefaultGenericPageConfigurationProvider instance = null;
	
	/** Note that OSGi creates new instance some times when old instances are still
	 * on the system. So we make configs static, the static instance just reduces the number
	 * of objects around, but there will be several instances
	 */
	public DefaultGenericPageConfigurationProvider() {
		super();
		if(instance == null)
			instance = this;
	}
	
	@Override
	public ConfigInfoExt getSessionConfiguration(String configurationId) {
		if(configurationId == null) {
			if(configs().isEmpty()) return null;
			return configs().values().iterator().next();
		}
		return configs().get(configurationId);
	}
	
	protected static int getNextId(int currentId, int maxId) {
		currentId++;
		if(currentId > maxId) currentId = 1;
		return currentId;
	}

	@Override
	public String id() {
		return "DefaultPageConfiguration";
	}

	@Override
	public String label(OgemaLocale locale) {
		return id();
	}
	
	@Override
	public void saveCurrentConfiguration(ConfigInfoExt currentConfiguration, String configurationId) {
		//do nothing for now
	}

}
