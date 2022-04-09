package org.smartrplace.appstore.api.versionmanagement;

import org.smartrplace.appstore.api.AppstoreBundle;

public class RundirBundleData {
	public String xmlFilePath;
	public AppstoreBundle bundle;

	public RundirBundleData(String xmlFilePath, AppstoreBundle bundle) {
		this.xmlFilePath = xmlFilePath;
		this.bundle = bundle;
	}
}
