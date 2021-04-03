package org.smartrplace.gui.filtering;

import java.util.List;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

@SuppressWarnings("serial")
/** This class is intended to be implemented directly in applications for filtering
 * the objects in a table page
 * @author dnestle
 *
 * @param <T>
 */
public abstract class SingleFilteringDirect<T> extends SingleFiltering<T, T> {

	public SingleFilteringDirect(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode,
			long optionSetUpdateRate, boolean addAllOption) {
		super(page, id, saveOptionMode, optionSetUpdateRate, addAllOption);
	}

	@Override
	protected boolean isAttributeSinglePerDestinationObject() {
		return true;
	}
	
	@Override
	protected T getAttribute(T object) {
		return object;
	}

	@Override
	protected abstract List<GenericFilterOption<T>> getOptionsDynamic(OgemaHttpRequest req);
	
	public T getSelectedItemDirect(OgemaHttpRequest req) {
		GenericFilterOption<T> option = getSelectedItem(req);
		if(option instanceof GenericFilterFixedSingle)
			return ((GenericFilterFixedSingle<T>)option).getValue();
		else
			return null;
	}
}
