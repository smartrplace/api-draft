package org.smartrplace.os.util;

import java.util.HashMap;
import java.util.Map;

import org.ogema.core.administration.AdminApplication;
import org.osgi.framework.Bundle;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

public class BundleData {
	public BundleData(Bundle bd) {
		this.bundle = bd;
		//this.appTitle = null;
	}
	public BundleData(Bundle bd, String appTitle) {
		this.bundle = bd;
		this.appTitle.put(OgemaLocale.ENGLISH, appTitle);
	}

	public Bundle bundle;
	public int position;
	
	public Map<OgemaLocale, String> appTitle = new HashMap<>();
	public Map<OgemaLocale, String> description = new HashMap<>();
	
	public AdminApplication entry;
}
