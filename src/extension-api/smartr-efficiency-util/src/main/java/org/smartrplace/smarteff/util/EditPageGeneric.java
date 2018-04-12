package org.smartrplace.smarteff.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.StringResource;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.button.RedirectButton;
import de.iwes.widgets.html.form.label.Label;

public abstract class EditPageGeneric<T extends Resource> extends EditPageBase<T> {
	public static OgemaLocale EN = OgemaLocale.ENGLISH;
	public static OgemaLocale DE = OgemaLocale.GERMAN;
	public static OgemaLocale FR = OgemaLocale.FRENCH;
	public static OgemaLocale CN = OgemaLocale.CHINESE;
	public static final Map<OgemaLocale, String> LINK_BUTTON_TEXTS = new HashMap<>();
	static {
		LINK_BUTTON_TEXTS.put(EN, "Info in Wiki");
		LINK_BUTTON_TEXTS.put(DE, "Info im Wiki");
		LINK_BUTTON_TEXTS.put(FR, "Info en Wiki");
	}

	public abstract void setData();
	
	Map<String, Map<OgemaLocale, String>> labels = new LinkedHashMap<>();
	Map<String, Map<OgemaLocale, String>> links = new HashMap<>();
	Map<String, Float> lowerLimits = new HashMap<>();
	Map<String, Float> upperLimits = new HashMap<>();
	Map<String, Map<String, String>> displayOptions = new HashMap<>();
	OgemaLocale localeDefault = OgemaLocale.ENGLISH;
	
	Map<String, Class<? extends Resource>> types;
	
	protected void setLabel(String resourceName, OgemaLocale locale, String text) {
		Map<OgemaLocale, String> innerMap = labels.get(resourceName);
		if(innerMap == null) {
			innerMap = new HashMap<>();
			labels.put(resourceName, innerMap);
		}
		innerMap.put(locale, text);
	}
	protected void setLabel(String resourceName, OgemaLocale locale, String text, OgemaLocale locale2, String text2) {
		setLabel(resourceName, locale, text);
		setLabel(resourceName, locale2, text2);
	}
	protected void setLabel(String resourceName, OgemaLocale locale, String text,
			OgemaLocale locale2, String text2, float min, float max) {
		setLabel(resourceName, locale, text);
		setLabel(resourceName, locale2, text2);
		setLimits(resourceName, min, max);
	}
	protected void setLink(String resourceName, OgemaLocale locale, String text) {
		Map<OgemaLocale, String> innerMap = links.get(resourceName);
		if(innerMap == null) {
			innerMap = new HashMap<>();
			links.put(resourceName, innerMap);
		}
		innerMap.put(locale, text);		
	}
	protected void setLimits(String resourceName, float min, float max) {
		lowerLimits.put(resourceName, min);		
		upperLimits.put(resourceName, max);		
	}
	
	public void setDefaultLocale(OgemaLocale locale) {
		localeDefault = locale;
	}
	
	@Override
	public String label(OgemaLocale locale) {
		return typeClass().getSimpleName()+" Edit Page";
	}
	
	public boolean checkResource(T res) {
		if(!checkResourceBase(res, false)) return false;
		String newName = CapabilityHelper.getnewDecoratorName(typeClass().getSimpleName(), res.getParent());
		Resource name = res.getSubResource("name");
		if ((name != null) && (name instanceof StringResource)) {
			ValueResourceHelper.setIfNew((StringResource)name, newName);
		}
		for(String sub: labels.keySet()) {
			Class<? extends Resource> type = types.get(sub);
			if(type == null) continue;
			if(FloatResource.class.isAssignableFrom(type)) {
				Float low = lowerLimits.get(sub);
				Float up = upperLimits.get(sub);
				float lowv = (low!=null)?up:0;
				float upv = (up!=null)?up:999999f;
				FloatResource valRes = res.getSubResource(sub);
				if((valRes.getValue() < lowv)||(valRes.getValue() > upv)) {
					return false;
				}
			}
		}
		return true;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void getEditTableLines(EditPageBase<T>.EditTableBuilder etb) {
		types = new HashMap<>();
		for(Method m: typeClass().getDeclaredMethods()) {
			Class<?> rawClass = m.getReturnType();
			if(Resource.class.isAssignableFrom(rawClass)) {
				types.put(m.getName(), (Class<? extends Resource>) rawClass);				
			}
		}
		for(String sub: labels.keySet()) {
			OgemaWidget widget;
			Class<? extends Resource> type = types.get(sub);
			if(type == null) continue;
			if(StringResource.class.isAssignableFrom(type)) {
				widget = mh.stringEdit(sub, alert);
			} else if(FloatResource.class.isAssignableFrom(type)) {
				Float low = lowerLimits.get(sub);
				Float up = upperLimits.get(sub);
				float lowv = (low!=null)?up:0;
				float upv = (up!=null)?up:999999f;
				widget = mh.floatEdit((String)sub, alert, lowv, upv,
						sub+" limits:"+lowv+" to "+upv);
			} else {
				continue;
			}

			final Map<OgemaLocale, String> innerMap = labels.get(sub);
			Label label = new Label(page, "label"+pid()) {
				private static final long serialVersionUID = -2849170377959516221L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					String text = innerMap.get(req.getLocale());
					if(text == null) text = innerMap.get(localeDefault);
					if(text != null) setText(text, req);
					else setText("*"+sub+"*", req);
				}
			};
			RedirectButton linkButton = null;
			final Map<OgemaLocale, String> linkMap = links.get(sub);
			if((linkMap != null) && (!linkMap.isEmpty())) {
				linkButton = new RedirectButton(page, "linkButton"+pid(), "") {
					private static final long serialVersionUID = 1L;
					@Override
					public void onGET(OgemaHttpRequest req) {
						String text = LINK_BUTTON_TEXTS.get(req.getLocale());
						if(text == null) text = LINK_BUTTON_TEXTS.get(localeDefault);
						if(text != null) setText(text, req);
						else setText("*"+sub+"*", req);
					}
					@Override
					public void onPrePOST(String data, OgemaHttpRequest req) {
						String text = linkMap.get(req.getLocale());
						if(text == null) text = linkMap.get(localeDefault);
						if(text != null) setUrl(text, req);
						else setUrl("*"+sub+"*/error.html", req);
					}
				};
			}
			etb.addEditLine(label, widget, linkButton);
		}
	}
}
