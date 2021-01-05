package org.smartrplace.gui.filtering;

import java.util.Map;

import org.ogema.internationalization.util.LocaleHelper;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** A generic filter with a fixed list of items*/
public class GenericFilterFixedSingle<A> extends GenericFilterBase<A> {
	protected final A value;
	
	public GenericFilterFixedSingle(A object, Map<OgemaLocale, String> optionLabel) {
		super(optionLabel);
		value = object;
	}
	
	public GenericFilterFixedSingle(A object, String optionLabel) {
		this(object, LocaleHelper.getLabelMap(optionLabel));
	}

	@Override
	public boolean isInSelection(A object, OgemaHttpRequest req) {
		return value.equals(object);
	}
	
	public A getValue() {
		return value;
	}
}
