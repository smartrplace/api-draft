package org.ogema.externalviewer.extensions;

import java.util.Map;

import de.iwes.widgets.reswidget.scheduleviewer.utils.DefaultScheduleViewerConfigurationProvider;

/** TODO: Note that such a "static class" cannot be instantiated by several apps using the
 * schedule viewer expert. Check if several classes derived from {@link DefaultScheduleViewerConfigurationProviderExtended}
 * use different static objects?
 *
 */
public abstract class DefaultScheduleViewerConfigurationProviderExtended extends DefaultScheduleViewerConfigurationProvider {

	public static final int MAX_ID = 100;

	protected abstract Map<String, SessionConfiguration> configs();
	protected abstract String getNextId();
	public String addConfig(SessionConfiguration config) {
		String id = getNextId();
		configs().put(id, config);
		return id;
	}

	protected static volatile DefaultScheduleViewerConfigurationProviderExtended instance = null;
	
	/** Note that OSGi creates new instance some times when old instances are still
	 * on the system. So we make configs static, the static instance just reduces the number
	 * of objects around, but there will be several instances
	 */
	public DefaultScheduleViewerConfigurationProviderExtended() {
		super();
		if(instance == null)
			instance = this;
		//else
		//	throw new IllegalStateException("ScheduleViewerConfigProvEvalOff shall behave like singleton,"
		//			+ "but needs public condstructor for OSGi service");
	}
	//Required by implementation
	/*public static DefaultScheduleViewerConfigurationProviderExtended getInstance() {
		if(instance == null) instance = new ScheduleViewerConfigProvEvalOff();
		return instance;
	}*/
	
	@Override
	public SessionConfiguration getSessionConfiguration(String configurationId) {
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
}
