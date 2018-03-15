package com.example.app.evaluationofflinecontrol.gui;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtPoint;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtRoomData;

import com.example.app.evaluationofflinecontrol.HeatControlOverviewController;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.DynamicTable;
import de.iwes.widgets.html.complextable.RowTemplate;
import de.iwes.widgets.html.form.checkbox.SimpleCheckbox;
import de.iwes.widgets.html.form.label.Header;


/**
 * An HTML page, generated from the Java code.
 */
public class MainPage {
	
	private final DynamicTable<HeatControlExtRoomData> table;
	final private HeatControlExtPoint heatExtPoint;

	public MainPage(final WidgetPage<?> page, final HeatControlOverviewController app) {
		this.heatExtPoint = app.serviceAccess.heatExtPoint;
		
		Header header = new Header(page, "header", "Multi Service Page Example");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
		
		table = new DynamicTable<HeatControlExtRoomData>(page, "evalviewtable") {

			private static final long serialVersionUID = 1L;
			
			@Override
			public void onGET(OgemaHttpRequest req) {
				Collection<HeatControlExtRoomData> providers = heatExtPoint.getRoomsControlled(); 
				updateRows(providers, req);
			}
		};
		
		table.setRowTemplate(new RowTemplate<HeatControlExtRoomData>() {

			@Override
			public Row addRow(HeatControlExtRoomData eval, OgemaHttpRequest req) {
				Row row = new Row();
				String lineId = getLineId(eval);
				/*row.addCell("name", eval.id());
				row.addCell("description", eval.description(OgemaLocale.ENGLISH));
				TemplateRedirectButton<EvaluationProvider> detailPageButton = new TemplateRedirectButton<EvaluationProvider>(
						table, "detailPageButton"+lineId, "Details", "", req) {

					private static final long serialVersionUID = 1L;
					
					@Override
					public void onPrePOST(String data, OgemaHttpRequest req) {
						selectItem(eval, req);
						setUrl("Details.html", req);
					}
					@Override
					protected String getConfigId(EvaluationProvider object) {
						return object.id();
					}
				};
												
				row.addCell("detailPageButton", detailPageButton);
				*/
				return row;
			}

			@Override
			public Map<String, Object> getHeader() {
				final Map<String, Object> header = new LinkedHashMap<>();
				header.put("name", "Name/ID");
				header.put("description", "Description");
				header.put("detailPageButton", "Open Detail Page");
				return header;
			}

			@Override
			public String getLineId(HeatControlExtRoomData arg0) {
				return ResourceUtils.getValidResourceName(arg0.getRoom().getLocation());
			}
		});
		
		page.append(table).linebreak();
		
		SimpleCheckbox ecoModeCheck = new SimpleCheckbox(page, "ecoModeCheck", "Eco-Modus") {
			private static final long serialVersionUID = 4762334737747120383L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				setValue(heatExtPoint.getEcoModeState(), req);
			}
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				heatExtPoint.setEcoModeState(getValue(req));
			}
		};
		page.append(ecoModeCheck);
	}
}