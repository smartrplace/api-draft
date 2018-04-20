package org.smartrplace.util.format;

import java.util.Map;

import de.iwes.util.resourcelist.SensorResourceListHelper;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

public class ValueFormat {
	public static String floatVal(float value) {
		return floatVal(value, null);
	}
	public static String floatVal(float value, String format) {
		if (Float.isNaN(value)) {
			return "n.a.";
		}
		String val;
		if(format != null) {
			val = String.format(format, value);
		} else {
			val = String.format("%.1f", value);
		}
		return val;
	}
	/** Print OGEMA temperature value in K as degree celsius
	 *  TODO: Check with {@link SensorResourceListHelper}
	 */
	public static String celsius(float value) {
		return celsius(value, 0);
	}
	/** Print celsius value of OGEMA temperater in K
	 * 
	 * @param value
	 * @param mode 0:add °C unit, 1: without unit
	 * @return string representation
	 */
	public static String celsius(float value, int mode) {
		return floatVal(value - 273.15f, (mode==0)?"%.1f °C":"%.1f");
	}
	public static String relativeTemperature(float tempVal) {
		return SensorResourceListHelper.printRelativeTempVal(tempVal);				
	}
	
	/** Provide structured/fail-safe string representing the value of a humidity resource*/
	public static String humidity(float humidityVal) {
		if(!Float.isNaN(humidityVal) || (humidityVal < 0)) {
			return String.format("%.0f %%", humidityVal*100);
		} else {
			return "n/a";
		}
	}

	public static String firstLowerCase(String in) {
		return in.substring(0, 1).toLowerCase()+in.substring(1);		
	}
	public static String firstUpperCase(String in) {
		return in.substring(0, 1).toUpperCase()+in.substring(1);		
	}
	
	public static String getLocaleString(OgemaHttpRequest req, Map<OgemaLocale, String> texts) {
		String text = texts.get(req.getLocale());
		if(text == null) return texts.get(OgemaLocale.ENGLISH);
		return text;
	}
	public static String getLocaleString(OgemaLocale locale, Map<OgemaLocale, String> texts) {
		String text = texts.get(locale);
		if(text == null) return texts.get(OgemaLocale.ENGLISH);
		return text;
	}

}
