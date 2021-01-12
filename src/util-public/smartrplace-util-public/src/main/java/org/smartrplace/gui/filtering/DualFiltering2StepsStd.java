package org.smartrplace.gui.filtering;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.internationalization.util.LocaleHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** Slightly simplified version of the standard {@link DualFiltering2Steps}. If special multi-relations and/or
 * performance optimization is required, the base version should be used.
 * 
 * @author dnestle
 *
 * @param <A>
 * @param <G>
 * @param <T>
 */
@SuppressWarnings("serial")
public abstract class DualFiltering2StepsStd<A, G, T> extends DualFiltering2Steps<A, G, T> {

	protected abstract Collection<G> getAllGroups();
	/** Get name to be displayed in first dropdown for group*/
	protected abstract String getGroupLabel(G group);
	//protected abstract GenericFilterFixedGroup<A, G> getGroupOptionDynamic(G group);
	
	/** Get all attributes assigned to each group selectable in the first dropdown
	 * @return map providing item name in second dropdown -> item to be selected*/
	protected abstract Map<String, A> getAttributesByGroup(G group);
	
	@Override
	protected List<GenericFilterFixedGroup<A, G>> getGroupOptionsDynamic() {
		List<GenericFilterFixedGroup<A, G>> result = new ArrayList<>();
		Collection<G> all = getAllGroups();
		for(G grp: all) {
			GenericFilterFixedGroup<A, G> newOption = getGroupOptionDynamic(grp);
			if(newOption == null)
				continue;
			result.add(newOption);			
		}
		return result;
	}

	
	public DualFiltering2StepsStd(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode) {
		this(page, id, saveOptionMode, 10000, true, true);
	}


	public DualFiltering2StepsStd(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode,
			long optionSetUpdateRate, boolean addAllOptionFirstDrop, boolean addAllOptionSecondDrop) {
		super(page, id, saveOptionMode, optionSetUpdateRate, addAllOptionFirstDrop, addAllOptionSecondDrop);
	}

	@Override
	protected List<GenericFilterOption<A>> getOptionsDynamic(G group,
			OgemaHttpRequest req) {
		List<GenericFilterOption<A>> result = new ArrayList<>();
		Map<String, A> all = getAttributesByGroup(group);
		for(Entry<String, A> dev: all.entrySet()) {
			GenericFilterOption<A> option = new GenericFilterFixedSingle<A>(
					dev.getValue(), LocaleHelper.getLabelMap(dev.getKey()));
			result.add(option);
		}
		return result;
	}

	//@Override
	protected GenericFilterFixedGroup<A, G> getGroupOptionDynamic(G grp) {
		String label = getGroupLabel(grp);
		GenericFilterFixedGroup<A, G> newOption = new GenericFilterFixedGroup<A, G>(
				grp, LocaleHelper.getLabelMap(label)) {

			@Override
			public boolean isInSelection(A object, G group) {
				throw new UnsupportedOperationException("Should not be used in first dropdown!");
				//return (devGrp.getSubGroup(object.getLocation()) != null);
			}
		};
		return newOption;
	}

}
