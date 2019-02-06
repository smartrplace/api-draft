/**
 * ﻿Copyright 2018 Smartrplace UG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
