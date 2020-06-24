package org.smartrplace.gui.filtering;

import java.util.Map;

import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** A generic filter with a fixed group of items of type G. In some cases
 * GenericFilterFixedSingle typed to G should be used instead, see DualFiltering2Steps.
 * It makes sense when the group type shall be stored that is different from the selection
 * attribute type like in {@link UserFilteringWithGroups}
 * @author dnestle
 *
 * @param <T>
 * @param <G>
 */
public abstract class GenericFilterFixedGroup<T, G> extends GenericFilterBase<T> {
	protected final G group;
	
	public GenericFilterFixedGroup(G group, Map<OgemaLocale, String> optionLabel) {
		super(optionLabel);
		this.group = group;
	}
	
	public G getGroup() {
		return group;
	}
	
	public abstract boolean isInSelection(T object, G group);

	@Override
	public boolean isInSelection(T object, OgemaHttpRequest req) {
		return isInSelection(object, group);
	}
}
