package org.smartrplace.gui.filtering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.internationalization.util.LocaleHelper;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.template.DefaultDisplayTemplate;

/** Provides a dropdown for filtering, typically for filtering of the objects used to generate table lines.
 *
 * @param <A> attribute type for which the filtering shall take place
 * @param <T> type of object returned as filtering result (typically type of object used in table)
 */
public abstract class SingleFilteringGroupBased<A, G, T> extends SingleFiltering<A, T> {
	public abstract boolean isInSelection(T object, G group);
	//protected abstract List<GenericFilterFixedGroup<A, G>> getGroupOptionsDynamic();
	//protected abstract GenericFilterFixedGroup<A, G> getGroupOptionDynamic(G group);
	//protected abstract List<G> getGroups(A object);
	
	public SingleFilteringGroupBased(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode,
			long optionSetUpdateRate, boolean addAllOption) {
		super(page, id, saveOptionMode, optionSetUpdateRate, addAllOption);
	}

}
