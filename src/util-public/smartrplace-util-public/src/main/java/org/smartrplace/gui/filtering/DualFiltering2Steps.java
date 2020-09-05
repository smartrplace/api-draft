package org.smartrplace.gui.filtering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.internationalization.util.LocaleHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;

/** 
 * 
 * @author dnestle
 *
 * @param <A>
 * @param <G> type of groups in first dropdown
 * @param <T>
 */
public abstract class DualFiltering2Steps<A, G, T> extends SingleFiltering<A, T> {
	private static final long serialVersionUID = 1L;
	protected final SingleFilteringGroupBased<A, G, A> firstDropDown;
	
	//protected abstract List<G> getAllGroups(OgemaHttpRequest req);
	//protected abstract List<A> elementsInGroup(G group, OgemaHttpRequest req);
	protected abstract List<GenericFilterOption<A>> getOptionsDynamic(G group, OgemaHttpRequest req);
	protected abstract List<GenericFilterFixedGroup<A, G>> getGroupOptionsDynamic();
	protected abstract GenericFilterFixedGroup<A, G> getGroupOptionDynamic(G group);
	protected abstract List<G> getGroups(A object);
	protected abstract boolean isGroupEqual(G group1, G group2);
	
	//We do not support prelection per user here yet*/
	protected final Map<String, String> preSelectionPerGroup = new HashMap<>();
	protected String preSelectionGroup = null;
	
	/** Settings this to false may save considerable reaction time and performance*/
	public boolean suppressEmptyOptionsInFirstDropdown = true;

	public DualFiltering2Steps(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode) {
		this(page, id, saveOptionMode, -1, true);
	}

	public DualFiltering2Steps(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode, long optionSetUpdateRate,
			boolean addAllOption) {
		super(page, id, saveOptionMode, 1, addAllOption);
		if(saveOptionMode == OptionSavingMode.PER_USER)
			throw new UnsupportedOperationException("PER_USER not supported for DualFiltering2Steps");
		//FIXME: For now we add the all option to the first dropdown always. This should be configurable in the future,
		//maybe also dynamically to detect whether this leads to too large dropdowns
		firstDropDown = new SingleFilteringGroupBased<A, G, A>(page, id+"_first", saveOptionMode, optionSetUpdateRate, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean isAttributeSinglePerDestinationObject() {
				return false;
			}
			
			@Override
			protected A getAttribute(A object) {
				return null;
			}
			
			@Override
			protected List<A> getAttributes(A object) {
				List<A> result = new ArrayList<>();
				result.add(object);
				return result ;
				//return getGroups(object);
			}
			
			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			protected List<GenericFilterOption<A>> getOptionsDynamic(OgemaHttpRequest req) {
			//	return getOptionsDynamic();
			//}
			//protected List<GenericFilterOption<A>> getOptionsDynamic() {
				if(suppressEmptyOptionsInFirstDropdown) {
					List<GenericFilterFixedGroup<A, G>> all = (getGroupOptionsDynamic());
					List<GenericFilterOption<A>> result = new ArrayList<>();
					for(GenericFilterFixedGroup<A, G> a: all) {
						GenericFilterFixedGroup<A, G> opt = getGroupOptionDynamic(a.getGroup());
						List<GenericFilterOption<A>> secondSel = DualFiltering2Steps.this.getOptionsDynamic(a.getGroup(), req);
						if(secondSel == null || secondSel.isEmpty() || secondSel.get(0) == DualFiltering2Steps.this.NONE_OPTION)
							continue;
						result.add(a);
					}
					return result;
				}
				return (List)(getGroupOptionsDynamic());
			}
			
			@Override
			protected long getFrameworkTime() {
				return DualFiltering2Steps.this.getFrameworkTime();
			}

			@Override
			public boolean isInSelection(A object, G group) {
				return isInSelectionObjectInGroup(object, group);
				/*List<G> grpsForObject = getGroups(object);
				for(G grp1: grpsForObject) {
					if(isGroupEqual(grp1, group))
						return true;
				}
				return false;
				//return grpsForObject.contains(group);*/
			}
		};
		firstDropDown.registerDependentWidget(this);
	}

	protected boolean isInSelectionObjectInGroup(A object, G group) {
		List<G> grpsForObject = getGroups(object);
		for(G grp1: grpsForObject) {
			if(isGroupEqual(grp1, group))
				return true;
		}
		return false;
		//return grpsForObject.contains(group);		
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected List<GenericFilterOption<A>> getOptionsDynamic(OgemaHttpRequest req) {
		GenericFilterOption<A> groupFilterSelected = firstDropDown.getSelectedItem(req);
		if(groupFilterSelected == firstDropDown.getAllOption(req) || groupFilterSelected == null)
			return getOptionsDynamic((G)null, req);
		if(!(groupFilterSelected instanceof GenericFilterFixedGroup))
			throw new IllegalStateException();
		return getOptionsDynamic(((GenericFilterFixedGroup<A, G>)groupFilterSelected).getGroup(), req);
	}
	
	@Override
	protected boolean isAttributeSinglePerDestinationObject() {
		return false;
	}
	
	public SingleFiltering<A, A> getFirstDropdown() {
		return firstDropDown;
	}
	
	@Override
	public GenericFilterOption<A> getAllOption(OgemaHttpRequest req) {
		if(req == null)
			return super.getAllOption(req);
		GenericFilterOption<A> groupFilterSelected = firstDropDown.getSelectedItem(req);
		if(groupFilterSelected == firstDropDown.getAllOption(req) || groupFilterSelected == null)
			return super.getAllOption(req);
		return new GenericFilterBase<A>(LocaleHelper.getLabelMap(allOptionsForStandardLocales())) {

			@SuppressWarnings("unchecked")
			@Override
			public boolean isInSelection(A object, OgemaHttpRequest req) {
				return isInSelectionObjectInGroup(object, ((GenericFilterFixedGroup<A, G>)groupFilterSelected).getGroup());
			}
		};
	}
}
