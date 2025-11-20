package org.smartrplace.gui.tablepages;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.form.textfield.TextField;

public abstract class TextFieldSetpoint extends TextField {
	protected final Alert alert;
	protected final Float min;
	protected final float max;
	protected abstract float getValuePreset(OgemaHttpRequest req);
	protected abstract boolean setValueOnPost(float value, OgemaHttpRequest req);
	
	public TextFieldSetpoint(OgemaWidget parent, String id, 
			Alert alert, Float min, float max,
			OgemaHttpRequest req) {
		super(parent, id, req);
		this.alert = alert;
		this.max = max;
		this.min = min;
		if(alert != null)
			this.registerDependentWidget(alert, req);
	}

	@Override
	public void onGET(OgemaHttpRequest req) {
		setValue(String.format("%.1f", getValuePreset(req)), req);
	}
	@Override
	public void onPOSTComplete(String data, OgemaHttpRequest req) {
		String val = getValue(req);
		val = val.replaceAll("[^\\d.-]", "");
		try {
			float value  = Float.parseFloat(val);
			if(min != null && value < min || value> max) {
				alert.showAlert(String.format("Allowed range: %.1f to %.1f", min, max), false, req);
			} else
				setValueOnPost(value, req);
		} catch (NumberFormatException | NullPointerException e) {
			if(alert != null) alert.showAlert("Entry "+val+" could not be processed!", false, req);
			return;
		}
	}
	
}
