package org.smartrplace.util.frontend.servlet;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.widgets.configuration.service.OGEMAConfigurationProvider;

import de.iwes.timeseries.eval.base.provider.utils.TimeSeriesDataImpl;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** Base config provider for information to the monitoring time series servlet
 * TODO: This should not be inherited more than once without switching to an own knownTS.
 *
 */
public class TimeseriesConfigProviderBase implements OGEMAConfigurationProvider {
	public static final Map<String, TimeSeriesDataImpl> knownTS = new HashMap<>();
	
	@Override
	public String className() {
		return "org.smartrplace.app.monbase.gui.TimeSeriesServlet";
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
		return knownTS.get(property);
	}
}
