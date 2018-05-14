package org.smartrplace.util.directresourcegui;

import java.util.Map;

import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.emptywidget.EmptyData;
import de.iwes.widgets.object.widget.popup.WidgetEntryData;

public class KnownWidgetHolderData<T> extends EmptyData {
	
	public Map<T, DynamicTable<WidgetEntryData>> popTable;
	//public Map<T, List<WidgetEntryData>> existingWidgetsInTable;
	//public Map<String, Label> existingLabels;
	public int counter = 0;

	public KnownWidgetHolderData(KnownWidgetHolder<T> knownWidgetHolder) {
		super(knownWidgetHolder);
	}
	
}
