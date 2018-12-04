package org.ogema.externalviewer.extensions;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider;
import de.iwes.widgets.reswidget.scheduleviewer.api.ScheduleViewerConfigurationProvider.SessionConfiguration;

public class ScheduleViewerOpenButton extends RedirectButton {
	private static final long serialVersionUID = 1L;
	public static final String URL_TO_SCHEDULEVIEWER = ScheduleViewerConfigurationProvider.VIEWER_URL_PATH + "/index.html"
			+ "?providerId="; //+ ScheduleViewerConfigurationProviderImpl.CONFIGURATION_PROVIDER_ID;	
	//public static final int maxId = 100;
	
	protected final String providerId;
	//protected Map<String, SessionConfiguration> configs = new HashMap<>();
	//protected static int lastConfig = 0;
	protected final DefaultScheduleViewerConfigurationProviderExtended schedConfigProv;

	
	public ScheduleViewerOpenButton(WidgetPage<?> page, String id, String text,
			String providerId, DefaultScheduleViewerConfigurationProviderExtended schedConfigProv) {
		super(page, id, text, URL_TO_SCHEDULEVIEWER+providerId);
		this.providerId = providerId;
		this.schedConfigProv = schedConfigProv;
	}
	public ScheduleViewerOpenButton(OgemaWidget parent, String id, String text,
			String providerId, DefaultScheduleViewerConfigurationProviderExtended schedConfigProv,
			OgemaHttpRequest req) {
		super(parent, id, text, URL_TO_SCHEDULEVIEWER+providerId, req);
		this.providerId = providerId;
		this.schedConfigProv = schedConfigProv;
	}
	
	public String addConfig(SessionConfiguration sc) {
		//lastConfig++;
		//if(lastConfig > maxId) lastConfig = 1;
		//String result = ""+lastConfig;
		String result = schedConfigProv.addConfig(sc);
		return result;
	}

	protected void setConfigId(String configurationId, OgemaHttpRequest req) {
		setUrl(URL_TO_SCHEDULEVIEWER+providerId+"&configId="+configurationId, req);
	}
}
