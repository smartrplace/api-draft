package org.smartrplace.gui.filtering;

import de.iwes.widgets.api.widgets.WidgetPage;

/** A {@link SingleFiltering} that allows to define groups of the filtering attributes A.
 * A {@link SingleFiltering} can be easily extended to a SingleFilteringGroupBased and this can
 * be used as part of {@link DualFiltering2Steps}. Currently DualFiltering2Steps does not
 * support yet to add a SingleFilteringGroupBased directly, but this should be implemented in
 * the future.
 *
 * @param <A> attribute type for which the filtering shall take place
 * @param <G> type of groups of A used for filtering
 * @param <T> type of object returned as filtering result (typically type of object used in table)
 */
public abstract class SingleFilteringGroupBased<A, G, T> extends SingleFiltering<A, T> {
	private static final long serialVersionUID = 1L;

	public abstract boolean isInSelection(T object, G group);
	
	public SingleFilteringGroupBased(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode,
			long optionSetUpdateRate, boolean addAllOption) {
		super(page, id, saveOptionMode, optionSetUpdateRate, addAllOption);
	}

}
