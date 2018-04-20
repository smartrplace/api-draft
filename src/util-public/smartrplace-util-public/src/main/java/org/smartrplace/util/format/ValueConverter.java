package org.smartrplace.util.format;

import java.util.HashMap;
import java.util.Map;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;

/** Standard class that is intended to be inherited, checkMethods overwritten for different applications*/
public class ValueConverter {
	public static final Map<OgemaLocale, String> FORMATFAILED = new HashMap<>();
	//public static final Map<OgemaLocale, String> TOOLARGE = new HashMap<>();
	//public static final Map<OgemaLocale, String> TOOSMALL = new HashMap<>();
	public static final Map<OgemaLocale, String> NEWVALUE = new HashMap<>();
	
	static  {
		FORMATFAILED.put(OgemaLocale.ENGLISH, "Could not read");
		FORMATFAILED.put(OgemaLocale.GERMAN, "Falsches Format");

		//TOOLARGE.put(OgemaLocale.ENGLISH, "Value too large");
		//TOOLARGE.put(OgemaLocale.GERMAN, "Wert zu groß");
		
		//TOOSMALL.put(OgemaLocale.ENGLISH, "Value too small");
		//TOOSMALL.put(OgemaLocale.GERMAN, "Wert zu klein");

		NEWVALUE.put(OgemaLocale.ENGLISH, "New value:");
		NEWVALUE.put(OgemaLocale.GERMAN, "Neuer Wert:");
	}
	
	private final Alert alert;
	private final float minimumAllowed;
	private final float maximumAllowed;
	private final String fieldName;
	
	/**
	 * 
	 * @param fieldName
	 * @param alert may be null
	 * @param minimumAllowed may be null
	 * @param maximumAllowed may be null
	 */
	public ValueConverter(String fieldName, Alert alert, Float minimumAllowed, Float maximumAllowed) {
		this.fieldName = fieldName;
		this.alert = alert;
		if(minimumAllowed == null) this.minimumAllowed = 0;
		else this.minimumAllowed = minimumAllowed;
		if(maximumAllowed == null) this.maximumAllowed = 999999;
		else this.maximumAllowed = maximumAllowed;
	}

	/** Parse input and process
	 * 
	 * @param input
	 * @return null if not successful
	 */
	public Integer checkNewValueInt(String val, OgemaHttpRequest req) {
		int value;
		try {
			value  = Integer.parseInt(val);
		} catch (NumberFormatException | NullPointerException e) {
			if(alert != null) alert.showAlert(ValueFormat.getLocaleString(req, FORMATFAILED)+"("+getFieldName(req)+")", false, req);
			return null;
		}
		if (value < minimumAllowed) {
			if(alert != null) alert.showAlert(getValueFailedMessage(true, req), false, req);
			return null;
		}
		if (value > maximumAllowed) {
			if(alert != null) alert.showAlert(getValueFailedMessage(false, req), false, req);
			return null;
		}
		if(alert != null) alert.showAlert(ValueFormat.getLocaleString(req, NEWVALUE) + value+"("+getFieldName(req)+")", true, req);
		return value;
	}
	
	public Alert getAlert() {
		return alert;
	}
	
	protected String getValueFailedMessage(boolean tooSmall, OgemaHttpRequest req) {
		OgemaLocale locale = req.getLocale();
		if(locale == OgemaLocale.GERMAN) return "Zulässige Werte für "+getFieldName(req)+":"+minimumAllowed+" bis "+maximumAllowed;
		return getFieldName(req)+" limits:"+minimumAllowed+" to "+maximumAllowed;
	}
	protected String getFieldName(OgemaHttpRequest req) {
		return fieldName;
	}
}
