package org.smartrplace.util.directresourcegui;

import java.util.ArrayList;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.smartrplace.util.directobjectgui.ObjectGUIHelperBase;

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
import de.iwes.widgets.resource.widget.table.ResourceTable;

@Deprecated
public class DetailPopupButtonBak<T extends Resource> extends Button {
	final ClosingPopup<T> popMore1;
	final T object;
	final ApplicationManager appMan;
	final String tableId;
	final KnownWidgetHolder<T> knownWidgets;
	final ResourceGUITableProvider<T> widgetProvider;
	
	public DetailPopupButtonBak(ResourceTable<T> mainTable, String id, String text, OgemaHttpRequest req,
			final ClosingPopup<T> popMore1, final T object, ApplicationManager appMan,
			final String tableId, final KnownWidgetHolder<T> knownWidgets,
			ResourceGUITableProvider<T> widgetProvider) {
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
	
	@Override
	public void onPrePOST(String data, OgemaHttpRequest req) {
		popMore1.setItem(object, req);
		
		popMore1.getPopupSnippet().clear(req);
		KnownWidgetHolderData<T> clData = (KnownWidgetHolderData<T>)(knownWidgets.getData(req));
			ResourceGUIHelper<T> popvh = new ResourceGUIHelper<T>(popMore1.getPopupSnippet(), req, object, appMan, false);
			//popvh.pageSnippet = popMore1.getPopupSnippet();
			popvh.popTableData = new ArrayList<>();
			popvh.evaluteForDetailsPopup(ObjectGUIHelperBase.WidgetsToAdd.ALL);
			widgetProvider.addWidgets(object, popvh, tableId+"pup"+clData.counter++, req, null, appMan);
			
			final DynamicTable<WidgetEntryData> popTableLoc = new DynamicTable<WidgetEntryData>(popMore1.getPopupSnippet(), "popTable_"+tableId, req);
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
					subRow.addCell(popHeader);
					subRow.addCell(object.widget);
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
