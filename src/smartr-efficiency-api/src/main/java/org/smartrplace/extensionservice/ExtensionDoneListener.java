package org.smartrplace.extensionservice;

public interface ExtensionDoneListener<T> {
	void done(ExtensionCapability capability, T itemProcessed);
}
