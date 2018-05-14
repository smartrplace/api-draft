package org.ogema.util.directresourcegui.kpi;

import java.util.Collection;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.tools.resource.util.TimeUtils;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.widgets.OgemaWidget;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.dynamics.TriggeredAction;
import de.iwes.widgets.api.widgets.dynamics.TriggeringAction;
import de.iwes.widgets.api.widgets.html.Linebreak;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;

public abstract class KPIMonitoringReport extends ObjectGUITablePage<KPIResultType, Resource> {
	private Header header;
	private IntervalTypeDropdown singleTimeInterval;
	private StaticTable footerTable;
	protected Button updateButton;
	private Label dateOfReport;
	
	protected final Collection<Integer> intervalTypes;
	protected final int pastColumnNum;
	protected final boolean registerDependentWidgets;
	
	//TODO: This is not thread-safe. Every session should have its own currentTime, but
	//this requires a special widget data
	protected long currentTime;
	
	public KPIMonitoringReport(WidgetPage<?> page, ApplicationManager appMan,
			Collection<Integer> intervalTypes, int pastColumnNum, boolean registerDependentWidgets) {
		super(page, appMan, null, false);
		this.intervalTypes = intervalTypes;
		this.pastColumnNum = pastColumnNum;
		this.registerDependentWidgets = registerDependentWidgets;
		triggerPageBuild();
	}
	
	@Override
	protected KPIResultType getHeaderObject() {
		return new KPIResultType(true);
	}
	@Override
	protected String getHeaderText(String columnId, final ObjectResourceGUIHelper<KPIResultType, Resource> vh, OgemaHttpRequest req) {
		vh.setDoRegisterDependentWidgets(registerDependentWidgets);
		return null;
	}

	@Override
	public void addWidgets(KPIResultType object, ObjectResourceGUIHelper<KPIResultType, Resource> vh, String id,
			OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		if(req != null) {
			vh.stringLabel("Provider", id, object.provider.label(req.getLocale()), row);
			vh.stringLabel("Result", id, object.resultType.label(req.getLocale()), row);
		} else {
			vh.registerHeaderEntry("Provider");
			vh.registerHeaderEntry("Result");
		}
		for(int pastIdx = pastColumnNum; pastIdx > 0; pastIdx--) {
			if(req == null) {
				vh.registerHeaderEntry("Minus"+pastIdx);
				continue;
			}
			final int localPastIdx = pastIdx;
			Label valueLabel = new Label(mainTable, "Minus"+pastIdx+"_"+id, req) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					int intervalType = singleTimeInterval.getSelectedItem(req);
					SampledValue sv = object.getValueNonAligned(intervalType, currentTime, localPastIdx);
					String text = String.format("%.2f", sv.getValue().getFloatValue());
					setText(text, req);
				}
			};
			row.addCell("Minus"+pastIdx, valueLabel);
			vh.triggerOnPost(singleTimeInterval, valueLabel);
		}
		if(req == null) {
			vh.registerHeaderEntry("Current");
		} else {
			Label valueLabel = new Label(mainTable, "Current_"+id, req) {
				private static final long serialVersionUID = 1L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					int intervalType = singleTimeInterval.getSelectedItem(req);
					SampledValue sv = object.getValueNonAligned(intervalType, currentTime, 0);
					String text = String.format("%.2f", sv.getValue().getFloatValue());
					setText(text, req);
				}
			};
			row.addCell("Current", valueLabel);
			vh.triggerOnPost(singleTimeInterval, valueLabel);
		}
	}

	@Override
	public Resource getResource(KPIResultType object, OgemaHttpRequest req) {
		return null;
		//throw new IllegalStateException("should not be used!");
	}

	@Override
	public void addWidgetsAboveTable() {
		this.singleTimeInterval = new IntervalTypeDropdown(page, "singleTimeInterval", false, intervalTypes);
		this.dateOfReport = new Label(page, "dateOfReport") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				currentTime = appMan.getFrameworkTime();
				String timeOfReport = "Time of Report: " + TimeUtils.getDateAndTimeString(currentTime);
				setText(timeOfReport, req);
			}
		};
		triggerOnPost(dateOfReport, singleTimeInterval);
		
		this.updateButton = new Button(page, "updateButton", "Update All") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onPOSTComplete(String data, OgemaHttpRequest req) {
				//TODO: Update all statistics
			}
		};
		footerTable = new StaticTable(1, 3);
		footerTable.setContent(0, 0, updateButton);
		footerTable.setContent(0, 1, "");
		footerTable.setContent(0, 2, "");
		this.header = new Header(page, "header", "Load Monitoring Report: Web View");
		page.append(header);
		page.append(Linebreak.getInstance());
		page.append(dateOfReport);
		page.append(singleTimeInterval);
	}
	@Override
	protected void addWidgetsBelowTable() {
		page.append(footerTable);
	}
	
	@SuppressWarnings("deprecation")
	public void triggerOnPost(OgemaWidget governor, OgemaWidget target) {
		if(registerDependentWidgets) governor.registerDependentWidget(target);
		else governor.triggerAction(target, TriggeringAction.POST_REQUEST, TriggeredAction.GET_REQUEST);
	}
}
