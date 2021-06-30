package org.smartrplace.gui.tablepages;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.checkbox.SimpleCheckbox;

@SuppressWarnings("serial")
public abstract class SimpleCheckboxSetpoint extends SimpleCheckbox {
	protected final Alert alert;
	protected abstract boolean getValuePreset(OgemaHttpRequest req);
	protected abstract boolean setValueOnPost(boolean value, OgemaHttpRequest req);

	public SimpleCheckboxSetpoint(OgemaWidget parent, String id,
			Alert alert, OgemaHttpRequest req) {
		super(parent, id, "", req);
		this.alert = alert;
	}

	@Override
	public void onGET(OgemaHttpRequest req) {
		setValue(getValuePreset(req), req);
	}
	@Override
	public void onPOSTComplete(String data, OgemaHttpRequest req) {
		boolean val = getValue(req);
		setValueOnPost(val, req);
	}

}
