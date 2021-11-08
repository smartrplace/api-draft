package org.ogema.accessadmin.api;

import java.util.HashMap;
import java.util.Map;

import de.iwes.widgets.api.extended.util.UserLocaleUtil;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.template.LabelledItem;

public class NamedIntegerType implements LabelledItem {
	protected final int id;
	protected final String idPrefix;
	Map<OgemaLocale, String> name;
	
	public NamedIntegerType(int id, Map<OgemaLocale, String> name) {
		this(id, name, null);
	}

	public NamedIntegerType(int id, Map<OgemaLocale, String> name, String idPrefix) {
		this.id = id;
		this.name = name;
		this.idPrefix = idPrefix;
	}

	public NamedIntegerType(int id, String englishName) {
		this.id = id;
		this.name = new HashMap<>();
		name.put(OgemaLocale.ENGLISH, englishName);
		this.idPrefix = null;
	}
	
	public NamedIntegerType(int id, String englishName, String germanName) {
		this.id = id;
		this.name = new HashMap<>();
		name.put(OgemaLocale.ENGLISH, englishName);
		name.put(OgemaLocale.GERMAN, germanName);
		this.idPrefix = null;
	}
	
	@Override
	public String id() {
		return ""+id;
	}

	@Override
	public String label(OgemaLocale locale) {
		if(locale != null)
			return name.get(locale);
		OgemaLocale defaultLocale = UserLocaleUtil.getSystemDefaultLocale();
		String result = name.get(defaultLocale);
		if(result != null)
			return result;
		if(name.isEmpty())
			return null;
		return name.entrySet().iterator().next().getValue();
	}

	public String labelReq(OgemaHttpRequest req) {
		OgemaLocale locale = UserLocaleUtil.getLocale(req);
		return label(locale);
	}
}
