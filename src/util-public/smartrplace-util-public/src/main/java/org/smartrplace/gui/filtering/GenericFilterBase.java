package org.smartrplace.gui.filtering;

import java.util.Map;

import org.ogema.internationalization.util.LocaleHelper;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;

/** A generic filter with a fixed list of items*/
public abstract class GenericFilterBase<A> implements GenericFilterOption<A> {
	protected final Map<OgemaLocale, String> optionLabel;
	
	public GenericFilterBase(Map<OgemaLocale, String> optionLabel) {
		this.optionLabel = optionLabel;
	}
	
	public GenericFilterBase(String optionLabel) {
		this.optionLabel = LocaleHelper.getLabelMap(optionLabel);
	}

	@Override
	public Map<OgemaLocale, String> optionLabel() {
		return optionLabel;
	}
	
}
