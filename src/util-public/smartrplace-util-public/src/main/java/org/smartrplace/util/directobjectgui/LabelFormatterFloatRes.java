package org.smartrplace.util.directobjectgui;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.label.Label;

/** For now the formatter can update float and String labels and use the state colors
 * 0=orange
 * 1=green (no color as default)
 * 2=red
 *
 */
public interface LabelFormatterFloatRes {
	/** Perform additional operations in onGET
	 * @param myLabel */
	default void onGETAdditional(Label myLabel, OgemaHttpRequest req) {};
	
	/**
	 * 
	 * @param value NOTE: For temperature resources the value is provided in CELSIUS
	 * @param req
	 * @return
	 */
	int getState(float value, OgemaHttpRequest req);
}