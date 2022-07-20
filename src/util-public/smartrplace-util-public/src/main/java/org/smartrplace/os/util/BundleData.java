package org.smartrplace.os.util;

import org.ogema.core.administration.AdminApplication;
import org.osgi.framework.Bundle;

public class BundleData {
	public BundleData(Bundle bd) {
		this.bundle = bd;
		this.appTitle = null;
	}
	public BundleData(Bundle bd, String appTitle) {
		this.bundle = bd;
		this.appTitle = appTitle;
	}

	public Bundle bundle;
	public int position;
	
	public String appTitle;
	
	public AdminApplication entry;
}
