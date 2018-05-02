package org.ogema.util.directresourcegui.kpi;

import java.util.Collection;

import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.html.form.dropdown.TemplateDropdown;
import de.iwes.widgets.template.DisplayTemplate;

public class IntervalTypeDropdown extends TemplateDropdown<Integer> {
	private static final long serialVersionUID = 1L;

	public IntervalTypeDropdown(WidgetPage<?> page, String id, boolean globalWidget,
			Collection<Integer> intervalTypes) {
		super(page, id, globalWidget);
		setTemplate(new DisplayTemplate<Integer>() {
			
			@Override
			public String getLabel(Integer object, OgemaLocale locale) {
				return getIntervalTypeName(object, locale);
			}
			
			@Override
			public String getId(Integer object) {
				return ""+object;
			}
		});
		setDefaultItems(intervalTypes);
	}

	public static String getIntervalTypeName(int intervalType, OgemaLocale locale) {
		if(locale == OgemaLocale.GERMAN) switch(intervalType) {
		case 1:
			return "Jahre";
		case 2:
			return "Quartale";
		case 3:
			return "Monate";
		case 6:
			return "Wochen";
		case 10:
			return "Tage";
		case 15:
			return "4-Stunden-Intervalle";
		case 100:
			return "Stunden";
		case 101:
			return "Minuten";
		case 220:
			return "15-Minuten-Intervalle";
		case 240:
			return "10-Minuten-Intervalle";
		case 320:
			return "5-Minuten-Intervalle";
		case 1000:
			return "30-Sekunden-Intervalle";
		case 1020:
			return "10-Sekunden-Intervalle";
		default:
			throw new UnsupportedOperationException("Interval type "+intervalType+" not supported!");
		}
		
		//English
		switch(intervalType) {
		case 1:
			return "Years";
		case 2:
			return "Quarter Years";
		case 3:
			return "Months";
		case 6:
			return "Weeks";
		case 10:
			return "Days";
		case 15:
			return "Four Hour Intervals";
		case 100:
			return "Hours";
		case 101:
			return "Minutes";
		case 220:
			return "15 Minute Intervals";
		case 240:
			return "10 Minute Intervals";
		case 320:
			return "5 Minute Intervals";
		case 1000:
			return "30 seconds";
		case 1020:
			return "10 seconds";
		default:
			throw new UnsupportedOperationException("Interval type "+intervalType+" not supported!");
		}
	}
}
