package org.smartrplace.gui.filtering;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ogema.internationalization.util.LocaleHelper;
import org.smartrplace.widget.extensions.GUIUtilHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.template.DefaultDisplayTemplate;

/** Provides a dropdown for filtering, typically for filtering of the objects used to generate table lines.
 * Filtering is done for groups of A. These groups are defined by objects of {@link GenericFilterI}. If filtering shall be done directly for instances of A then for each such instance an instance of
 * {@link GenericFilterFixedSingle} can be generated.<br>
 * In the method {@link #getAttribute(Object)} or {@link #getAttributes(Object)} you provide the information which of
 * the finally filtered T objects belong to which attribute of type A. So A already has a "grouping character". But
 * the actual dropdown options allow to further group the attributes of type A into options that are represented by
 * GenericFilterI or the inherited GenericFilterOption. These option groups are defined in {@link #getOptionsDynamic(OgemaHttpRequest)}.<br>
 * 
 * If filtering requires two steps, e.g. providing real groups of A in the left dropdown and single-A options in the
 * right dropdown a {@link DualFiltering2Steps} element usually is the right choice. Here usually the decisive right dropdown only returns true
 * for a single A object in {@link GenericFilterI#isInSelection(Object, OgemaHttpRequest)}.<br>
 * If two independent groups of different types of attributes A shall be combined in an AND-filtering then a
 * {@link DualFiltering} can be used to generate the combined AND filter. This does not create the dropdown widgets
 * directly.
 * 
 * There are two ways of filtering integration:<br>
 * - Filtering of the elements of a table on a page. This filtering usually returns true for several A objects
 * per filter option in {@link GenericFilterI#isInSelection(Object, OgemaHttpRequest)}. In this case you usually have to overwrite
 * {@link #isAttributeSinglePerDestinationObject()}, getAttribute or getAttributes,
 * {@link #getOptionsDynamic(OgemaHttpRequest)} and {@link #getFrameworkTime()}.
 * - Selection of a single object that is configured in the table or otherwise on the page. In this case the decisive
 * dropdown usually should only offer a single A object, which may link to a single T object. So to offer a
 * meaningful navigation usually a {@link DualFiltering2Steps} is used in this case.
 *
 * @param <A> attribute type for which the filtering shall take place
 * @param <T> type of object returned as filtering result (typically type of object used in table)
 */
public abstract class SingleFiltering<A, T> extends TemplateDropdown<GenericFilterOption<A>> implements GenericFilterI<T> {
	private static final long serialVersionUID = 1461509059889019498L;

	public static final OgemaLocale[] defaultOrderedLocales = {OgemaLocale.ENGLISH, OgemaLocale.GERMAN,
			OgemaLocale.FRENCH, OgemaLocale.CHINESE};
	
	/** Just provide the label for the language requested. Usage of default English labels
	 * is handled by the base class
	 * @param object
	 * @param locale
	 * @return
	 */
	//protected abstract String getLabel(A attributeOption, OgemaLocale locale);
	
	/** If true then each destination object of type T has only a single base attribute of type A 
	 * (e.g. the room in which a device is located). If false several base attributes apply per
	 * destination object, e.g. the user groups to which a user applies.<br>
	 * If true then {@link #getAttribute(Object)} is used, otherwise {@link #getAttributes(Object)}.
	 * In the class implementation always {@link #getAttributes(Object)} is used as this is more
	 * generic, but this may be changed in the future to improve performance.
	 */
	protected abstract boolean isAttributeSinglePerDestinationObject();
	
	/** Only relevant if options update is configured, default implementation can just return 0*/
	protected abstract long getFrameworkTime( ); // {return 0;}
	
	/** Overwrite to disable all option for too many elements*/
	protected boolean isAllOptionAllowed(OgemaHttpRequest req) {
		return true;
	}
	
	/**If null is returned the default options set via {@link #addOption(GenericFilterOption, Map)} etc.
	 * are used. Otherwise only the dynamic options are displayed
	 * 
	 * @param req
	 * @return
	 */
	protected List<GenericFilterOption<A>> getOptionsDynamic(OgemaHttpRequest req) {
		return null;
	}

	/** Can just return null if {@link #getAttributes(Object)} is overwritten*/
	protected abstract A getAttribute(T object); /* {
		throw new IllegalStateException("Either getAttribute or getAttributes must be overriden! Not found for:"+this.getClass().getName()+" object:"+object);
	};*/
	protected List<A> getAttributes(T object) {
		List<A> result = new ArrayList<>();
		A attr = getAttribute(object);
		if(attr != null)
			result.add(attr);
		return result ;
	};
	
	public static enum OptionSavingMode {
		//A default is used for each new session
		NONE,
		//A map shall be saved containing the value for each user, if the user has no predefined value use default
		PER_USER,
		//A change in the selection will affect all users
		GENERAL
	}
	protected final OptionSavingMode saveOptionMode;
	protected final long optionSetUpdateRate;
	protected final boolean addAllOption;
	
	public class AllOption implements GenericFilterOption<A> {

		@Override
		public boolean isInSelection(A object, OgemaHttpRequest req) {
			return true;
		}

		@Override
		public Map<OgemaLocale, String> optionLabel() {
			return LocaleHelper.getLabelMap(allOptionsForStandardLocales());
		}
		
	}
	private final AllOption ALL_OPTION = new AllOption();
	public GenericFilterOption<A> getAllOption(OgemaHttpRequest req) {
		return ALL_OPTION;
	};
	public class NoneOption implements GenericFilterOption<A> {

		@Override
		public boolean isInSelection(A object, OgemaHttpRequest req) {
			return false;
		}

		@Override
		public Map<OgemaLocale, String> optionLabel() {
			return LocaleHelper.getLabelMap(noneOptionsForStandardLocales());
		}
		
	}
	public final NoneOption NONE_OPTION = new NoneOption();

	protected final Map<String, String> preSelectionPerUser;
	protected String preSelectionGeneralEnglish = null;
	public void setDefaultPreSelectionGeneral(String label) {
		preSelectionGeneralEnglish = label;
	}
	
	protected String[] allOptionsForStandardLocales() {
		return new String[] {"All", "Alle", "Tous", "All"};
	}
	protected String[] noneOptionsForStandardLocales() {
		return new String[] {"None", "---", "---", "----"};
	}
	
	protected List<GenericFilterOption<A>> filteringOptions = new ArrayList<>();
	//protected final TemplateDropdown<A> dropdown;
	
	//Do not use directly, use #label for reading and #getKnownLabels for writing
	//protected Map<OgemaLocale, Map<String, GenericFilterOption<A>>> knowLabels = new HashMap<>();
	
	public SingleFiltering(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode) {
		this(page, id, saveOptionMode, -1, true);
	}
	/**
	 * 
	 * @param page
	 * @param id
	 * @param saveOptionMode
	 * @param optionSetUpdateRate if negative no updates of filteringOptions will be made. If zero or {@link #getFrameworkTime()} is zero
	 * 		then an update will be triggered on every request. This requires {@link #getOptionsDynamic(OgemaHttpRequest)} to be
	 * 		overridden
	 */
	public SingleFiltering(WidgetPage<?> page, String id, OptionSavingMode saveOptionMode, long optionSetUpdateRate,
			boolean addAllOption) {
		super(page, "SingleFiltDrop"+id);
		this.saveOptionMode = saveOptionMode;
		this.optionSetUpdateRate = optionSetUpdateRate;
		this.addAllOption = addAllOption;
		if(saveOptionMode == OptionSavingMode.PER_USER) {
			preSelectionPerUser = new HashMap<>();
		} else
			preSelectionPerUser = null;
		if(addAllOption) {
			addOption(getAllOption(null));
			preSelectionGeneralEnglish = LocaleHelper.getLabel(getAllOption(null).optionLabel(), null);
		}
		//int idx = 0;
		//for(String allLabel: allOptionsForStandardLocales()) {
		//	getKnownLabels(defaultOrderedLocales[idx]).put(allLabel, null);
		//	idx++;
		//}
		setTemplate(new DefaultDisplayTemplate<GenericFilterOption<A>>() {
			@Override
			public String getLabel(GenericFilterOption<A> object, OgemaLocale locale) {
				return label(object, locale);
			}
		});
		//setDefaultAddEmptyOption(true);
	}
	
	//protected void addLabels(GenericFilterOption<A> object, Map<OgemaLocale, String> optionLabel) {
	//	for(Entry<OgemaLocale, String> lab: optionLabel.entrySet()) {
	//		getKnownLabels(lab.getKey()).put(lab.getValue(), object);
	//	}
	//}
	
	/** Add filtering option that selects a single base object
	 * @param optionLabel Just provide the label for the language you want. Usage of default English labels
	 * is handled by the base class. {@link LocaleHelper} usage is recommended.
	 * @return Further options can be added to the result*/
	public GenericFilterFixed<A> addOptionSingle(A object, Map<OgemaLocale, String> optionLabel) {
		GenericFilterFixed<A> result = new GenericFilterFixed<A>(object, optionLabel);
		addOption(result);
		return result;
	}
	/** Add filtering option that selects several base objects*/
	public GenericFilterFixed<A> addOptionSingle(A[] objects, Map<OgemaLocale, String> optionLabel) {
		GenericFilterFixed<A> result = new GenericFilterFixed<A>(objects, optionLabel);
		addOption(result);
		return result;
	}
	/** Add custom-created filtering option<br>
	 * Note that the filtering on this level should not depend on the {@link OgemaHttpRequest} for now.*/
	public void addOption(GenericFilterOption<A> newOption) {
		filteringOptions.add(newOption);
		checkFilteringOptions();
		//addLabels(newOption, optionLabel);
	}
	
	/** Only relevant if no dynamic option setting is configured*/
	public void finishOptionsSetupOnStartup() {
		setDefaultItems(filteringOptions);
		selectDefaultItem(filteringOptions.get(0));
	}
	
	/*protected Map<String, GenericFilterOption<A>> getKnownLabels(OgemaLocale locale) {
		Map<String, GenericFilterOption<A>> result = knowLabels.get(locale==null?OgemaLocale.ENGLISH:locale);
		if(result == null) {
			result = new HashMap<>();
			knowLabels.put(locale, result);
		}
		return result;
	}*/
	
	protected String labelLocaleOnly(GenericFilterOption<A> object, OgemaLocale locale) {
		return object.optionLabel().get(locale);
		/*Map<String, GenericFilterOption<A>> subMap = getKnownLabels(locale);
		for(Entry<String, GenericFilterOption<A>> lab: subMap.entrySet()) {
			if(lab.getValue() == null) {
				if(object == null)
					return lab.getKey();
				continue;
			}
			if(lab.getValue().equals(object))
				return lab.getKey();
		}
		return null;*/
	}
	protected String label(GenericFilterOption<A> object, OgemaLocale locale) {
		return LocaleHelper.getLabel(object.optionLabel(), locale);
		/*if(locale == null)
			locale = OgemaLocale.ENGLISH;
		String result = labelLocaleOnly(object, locale);
		if(result != null) {
			return result;
		} else if(locale != null && locale != OgemaLocale.ENGLISH) {
			result = labelLocaleOnly(object, OgemaLocale.ENGLISH);
			//if(result != null) {
			//	getKnownLabels(OgemaLocale.ENGLISH).put(result, object);
			//}			
		}
		return result;*/
	}
	
	@Override
	public void onPOSTComplete(String data, OgemaHttpRequest req) {
		if(saveOptionMode == OptionSavingMode.GENERAL) {
			preSelectionGeneralEnglish = LocaleHelper.getLabel(getSelectedItem(req).optionLabel(), null);
			GenericFilterOption<A> defaultItem = getFilterOption(preSelectionGeneralEnglish);
			selectDefaultItem(defaultItem);
		} else if (saveOptionMode == OptionSavingMode.PER_USER) {
			GenericFilterOption<A> selected = getSelectedItem(req);
			String user = GUIUtilHelper.getUserLoggedIn(req);
			preSelectionPerUser.put(user, LocaleHelper.getLabel(selected.optionLabel(), null));
		}
	}
	
	
	protected long lastOptionsUpdate = -1;
	@Override
	public void onGET(OgemaHttpRequest req) {
		if(optionSetUpdateRate >= 0) {
			long now = getFrameworkTime();
			if(now == 0 || ((now-lastOptionsUpdate) > optionSetUpdateRate)) {
				List<GenericFilterOption<A>> dynOpts = getOptionsDynamic(req);
				if(dynOpts != null) {
					if(addAllOption) {
						filteringOptions = new ArrayList<>();
						filteringOptions.add(getAllOption(req));
						filteringOptions.addAll(dynOpts);
					} else
						filteringOptions = dynOpts;
					GenericFilterOption<A> defaultItem = getFilterOption(preSelectionGeneralEnglish);
					if(filteringOptions.isEmpty()) {
						if(defaultItem == null)
							defaultItem = NONE_OPTION;
						filteringOptions = new ArrayList<>();
						filteringOptions.add(defaultItem);
					}
					else {
						defaultItem = getPreselectedItem(defaultItem, req);
					}
					checkFilteringOptions();
					setDefaultItems(filteringOptions);
					selectDefaultItem(defaultItem);
					update(filteringOptions, defaultItem, req);
				}
				lastOptionsUpdate = now;
			}
		}
		if(saveOptionMode == OptionSavingMode.PER_USER) {
			String user = GUIUtilHelper.getUserLoggedIn(req);
			GenericFilterOption<A> presel = getFilterOption(preSelectionPerUser.get(user));
			presel = getPreselectedItem(presel, req);
			selectItem(presel, req);
		} else if(saveOptionMode == OptionSavingMode.GENERAL) {
			GenericFilterOption<A> presel = getFilterOption(preSelectionGeneralEnglish);
			presel = getPreselectedItem(presel, req);
			selectItem(presel, req);
		}
	}
	
	protected GenericFilterOption<A> getPreselectedItem(GenericFilterOption<A> initialGuess,
			OgemaHttpRequest req) {
		if(initialGuess == ALL_OPTION && (filteringOptions.size()>1) && (!isAllOptionAllowed(req))) {
			return filteringOptions.get(1);
		}
		return initialGuess;
	}
	
	protected GenericFilterOption<A> getFilterOption(String englishLabel) {
		for(GenericFilterOption<A> item: filteringOptions) {
			if(LocaleHelper.getLabel(item.optionLabel(), null).equals(englishLabel)) {
				return item;
			}
		}
		return null;
	}
	
	protected void checkFilteringOptions() {
		Map<OgemaLocale, List<String>> knownLabels = new HashMap<>();
		for(GenericFilterOption<A> option: filteringOptions) {
			for(Entry<OgemaLocale, String> labelsLoc: option.optionLabel().entrySet()) {
				List<String> known = knownLabels.get(labelsLoc.getKey());
				if(known == null) {
					known = new ArrayList<>();
					known.add(labelsLoc.getValue());
					knownLabels.put(labelsLoc.getKey(), known);
				} else {
					if(known.contains(labelsLoc.getValue())) {
						String newLabel = labelsLoc.getValue()+"(*)";
						option.optionLabel().put(labelsLoc.getKey(), newLabel);
						known.add(newLabel);
					} else
						known.add(labelsLoc.getValue());
				}
			}
		}
	}
	
	protected final Map<String, List<T>> destinationObjects = new HashMap<>();
	private final Map<T, Long> destinationObjectsChecked2 = new HashMap<>();
	private Long getLastDestUpdateTime(T object) {
		return destinationObjectsChecked2.get(object);
	}
	private void setLastDestUpdateTime(T object, long time) {
		destinationObjectsChecked2.put(object, time);
	}
	List<T> getDestinationList(GenericFilterOption<A> attribute) {
		String stdLabel = LocaleHelper.getLabel(attribute.optionLabel(), null);
		List<T> alist = destinationObjects.get(stdLabel);
		if(alist == null) {
			alist = new ArrayList<>();
			destinationObjects.put(stdLabel, alist);
		}
		return alist;
	}
	
	@Override
	public boolean isInSelection(T object, OgemaHttpRequest req) {
		GenericFilterOption<A> selected = getSelectedItem(req);
		if(selected == null) {
			//empty option = All
			return true;
		}
		//Long lastUpdate = destinationObjectsChecked.get(object);
		final Long lastUpdate = getLastDestUpdateTime(object);
		final long now = getFrameworkTime();
		if((lastUpdate == null) ||
				((optionSetUpdateRate >= 0) && ((now-lastUpdate) > optionSetUpdateRate))) {
			List<A> tlist = getAttributes(object);
			for(GenericFilterOption<A> option: filteringOptions) {
				boolean found = false;
				for(A attr: tlist) {
					if(option.isInSelection(attr, null)) {
						found = true;
						break;
					}
				}
				List<T> alistsub = getDestinationList(option);
				if(!found) {
					if(alistsub.contains(object))
						alistsub.remove(object);
					continue;
				}
				if(!alistsub.contains(object))
					alistsub.add(object);
			}
			//destinationObjectsChecked.put(object, now);
			setLastDestUpdateTime(object, now);
		}
		List<T> alist = getDestinationList(selected);
		return alist.contains(object);
	}
	
	@Override
	public GenericFilterOption<A> getSelectedItem(OgemaHttpRequest req) {
		GenericFilterOption<A> result = super.getSelectedItem(req);
		if(result != null)
			return result;
		onGET(req);
		return super.getSelectedItem(req);
	}
}
