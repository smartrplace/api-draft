package org.smartrplace.apps.heatcontrol.extensionapi;

public interface GUIInitDataProvider {
	/** Services providing a list of objects may provide a sample here for
	 * {@link ObjectGUITablePages}
	 * @return sample object independently from real data objects available for
	 * initialization of the table
	 */
	<T> T getInitObject(Class<T> objectClass);
}
