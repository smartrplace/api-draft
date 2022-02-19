package org.smartrplace.util.directobjectgui;

import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.label.Label;

/** For now the formatter can update float and String labels and use the state colors
 * 0=orange
 * 1=green (no color as default)
 * 2=red
 *
 */
public interface LabelFormatter {
	/** Perform additional operations in onGET
	 * @param myLabel */
	default void onGETAdditional(Label myLabel, OgemaHttpRequest req) {};
	
	//int getState(float value, OgemaHttpRequest req);
	
	public class OnGETData {
		public OnGETData() {}
		public OnGETData(String text) {
			this.text = text;
		}
		public OnGETData(String text, int state) {
			this.text = text;
			this.state = state;
		}
		String text;
		//set this negative to avoid processing.
		int state = -1;
	}
	OnGETData getData(OgemaHttpRequest req);
}