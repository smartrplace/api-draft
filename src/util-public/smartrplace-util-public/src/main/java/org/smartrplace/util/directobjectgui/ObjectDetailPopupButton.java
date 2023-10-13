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
package org.smartrplace.util.directobjectgui;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.smartrplace.util.directresourcegui.KnownWidgetHolder;
import org.smartrplace.util.directresourcegui.KnownWidgetHolderData;

import de.iwes.widgets.api.extended.html.bricks.PageSnippet;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.object.widget.popup.ClosingPopup;
import de.iwes.widgets.object.widget.popup.WidgetEntryData;
import de.iwes.widgets.object.widget.table.DefaultObjectRowTemplate;

public class ObjectDetailPopupButton<T, R extends Resource> extends Button {
	protected final ClosingPopup<T> popMore1;
	protected final T object;
	protected final ApplicationManager appMan;
	protected final String tableId;
	protected final KnownWidgetHolder<T> knownWidgets;
	protected final ObjectGUITableProvider<T, R> widgetProvider;
	private final AtomicInteger tableCnt = new AtomicInteger(0);
	
	public ObjectDetailPopupButton(DynamicTable<T> mainTable, String id, String text, OgemaHttpRequest req,
			final ClosingPopup<T> popMore1, final T object, ApplicationManager appMan,
			final String tableId, final KnownWidgetHolder<T> knownWidgets,
			ObjectGUITableProvider<T, R> widgetProvider) {
		super(mainTable, id, text, req);
		this.popMore1 = popMore1;
		this.object = object;
		this.appMan = appMan;
		this.tableId = tableId;
		this.knownWidgets = knownWidgets;
		this.widgetProvider = widgetProvider;
		
		PageSnippet snippet = popMore1.getPopupSnippet();
		this.triggerAction(snippet, TriggeringAction.POST_REQUEST,
				TriggeredAction.GET_REQUEST);
		this.triggerAction(popMore1, TriggeringAction.POST_REQUEST,
				TriggeredAction.SHOW_WIDGET);
	}
	private static final long serialVersionUID = 1L;
	
	protected ObjectGUIHelperBase<T> getObjectGUIHelperBase(KnownWidgetHolderData<T> clData, OgemaHttpRequest req) {
		ObjectResourceGUIHelper<T, R> popvh = new ObjectResourceGUIHelper<T, R>(popMore1.getPopupSnippet(), req, object, appMan, false) {

			@Override
			protected R getResource(T object, OgemaHttpRequest req) {
				return widgetProvider.getResource(object, req);
			}
			
		};
		//popvh.pageSnippet = popMore1.getPopupSnippet();
		popvh.popTableData = new ArrayList<>();
		popvh.evaluteForDetailsPopup(ObjectGUIHelperBase.WidgetsToAdd.ALL);
		widgetProvider.addWidgets(object, popvh, tableId+"pup"+clData.counter++, req, null, appMan);
		return popvh;
	}
	
	@Override
	public void onPrePOST(String data, OgemaHttpRequest req) {
		popMore1.setItem(object, req);
		popMore1.getPopupSnippet().clear(req);
		KnownWidgetHolderData<T> clData = (KnownWidgetHolderData<T>)(knownWidgets.getData(req));
		ObjectGUIHelperBase<T> popvh = getObjectGUIHelperBase(clData, req);
		final DynamicTable<WidgetEntryData> popTableLoc 
			= new DynamicTable<WidgetEntryData>(popMore1.getPopupSnippet(), "popTable_"+tableId + tableCnt.getAndIncrement(), req);
		DynamicTable<WidgetEntryData> popTable = popTableLoc;

		DefaultObjectRowTemplate<WidgetEntryData> popTableTemplate = new DefaultObjectRowTemplate<WidgetEntryData>() {
			@Override
			public String getLineId(WidgetEntryData object) {
				clData.counter++;
				return String.format("I%08d", clData.counter) + "_" + super.getLineId(object);
			}
			@Override
			public Row addRow(WidgetEntryData object, OgemaHttpRequest req) {
				Row subRow = new Row();
				String lineId = getLineId(object);
				//Label popHeader = clData.existingLabels.get(object.headerName);
				//if(popHeader == null) {
				Label popHeader = new Label(popMore1.getPopupSnippet(), "popHeader"+tableId+lineId+clData.counter++, object.headerName, req);
				//clData.existingLabels.put(object.headerName, popHeader);
				//popTableLoc.registerDependentWidget(popHeader);
				//}
				subRow.addCell(popHeader, 4);
				subRow.addCell(object.widget, 12);
				//popTableLoc.registerDependentWidget(object.widget);
				return subRow;
			}
		};
		popTable.setRowTemplate(popTableTemplate);
		popTable.updateRows(popvh.popTableData, req);
			//existing.put(object, popTable);
			//clData.existingWidgetsInTable.put(object, popvh.popTableData);
		//} else {
		//	popTable.updateRows(clData.existingWidgetsInTable.get(object), req);
		//}
		popMore1.getPopupSnippet().append(popTable, req);
		
		//List<OgemaWidget> popWidgets = new ArrayList<>();
		//popWidgets.add(new Label(popUsed, "testLabel_"+id, "My Message!", req));
		//popUsed.updateWidgets(popWidgets, req);
		//popUsed.getPopupSnippet().append(vh.intLabel("downloads", id, object.numberDownloads(), row, 0), req);
	}
}
