package org.smartrplace.util.frontend.servlet;

import java.util.Collection;
import java.util.List;

import org.ogema.widgets.configuration.service.OGEMAConfigurationProvider;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** Base config provider for information to the monitoring time series servlet
 * TODO: This should not be inherited more than once without switching to an own knownTS.
 *
 */
@Deprecated // Implementatoins should not be required anymore
public class TimeseriesConfigProviderBase implements OGEMAConfigurationProvider {
	@Override
	public String className() {
		return UserServlet.TimeSeriesServletImplClassName;
	}

	@Override
	public int priority() {
		return 1000;
	}

	@Override
	public List<OGEMAConfigurationProvider> additionalProviders() {
		return null;
	}

	@Override
	public Collection<String> propertiesProvided() {
		return null;
	}

	@Override
	public String getProperty(String property, OgemaLocale locale, OgemaHttpRequest req, Object context) {
		return null;
	}

	@Override
	public Object getObject(String property, OgemaLocale locale, OgemaHttpRequest req, Object context) {
		return UserServlet.knownTS.get(property);
	}
}
