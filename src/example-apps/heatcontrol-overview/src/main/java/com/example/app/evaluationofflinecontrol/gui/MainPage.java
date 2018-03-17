package com.example.app.evaluationofflinecontrol.gui;

import java.util.List;

import org.ogema.core.application.ApplicationManager;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtPoint;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtRoomData;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import com.example.app.evaluationofflinecontrol.HeatControlOverviewController;

import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.checkbox.SimpleCheckbox;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;


/**
 * An HTML page, generated from the Java code.
 */
public class MainPage extends ObjectGUITablePage<HeatControlExtRoomData, Room>{
	
	final private HeatControlExtPoint heatExtPoint;

	public MainPage(final WidgetPage<?> page, final HeatControlOverviewController app,
			HeatControlExtRoomData initData) {
		super(page, app.appMan, initData);
		this.heatExtPoint = app.serviceAccess.heatExtPoint;
	}
	
	@Override
	public void addWidgetsAboveTable() {
		Header header = new Header(page, "header", "Heat Control Overview");
		header.addDefaultStyle(WidgetData.TEXT_ALIGNMENT_LEFT);
		page.append(header);
		
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
	
	@Override
	public List<HeatControlExtRoomData> getObjectsInTable(OgemaHttpRequest req) {
		List<HeatControlExtRoomData> providers = heatExtPoint.getRoomsControlled(); 
		return providers;
	}

	@Override
	public void addWidgets(HeatControlExtRoomData object, ObjectResourceGUIHelper<HeatControlExtRoomData, Room> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		Label sl = vh.stringLabel("Room name", id, ResourceUtils.getHumanReadableShortName(object.getRoom()), row);
if(sl != null) System.out.println("Room name "+ResourceUtils.getHumanReadableShortName(object.getRoom())+" in "+sl.getId());
else System.out.println("Room name for "+id);
		if(object.getThermostats() != null) {
			int thDif = object.getRoomTemperatureSensors().size() - object.getRoomHumiditySensors().size();
			String addSymbol = "";
			if(thDif > 0) addSymbol = "-";
			else if(thDif < 0) addSymbol = "+";
			String text = ""+object.getThermostats().size()+" / "+object.getRoomTemperatureSensors().size()+addSymbol+" / "+object.getWindowSensors().size();
			sl = vh.stringLabel("Therm/TH/Win", id, text, row);
if(sl != null) System.out.println("Therm/TH/Win "+text+" in "+sl.getId());
else System.out.println("Therm/TH/Win for "+id);
		} else vh.registerHeaderEntry("Therm/TH/Win");
	}

	@Override
	public Room getResource(HeatControlExtRoomData object, OgemaHttpRequest req) {
		return object.getRoom();
	}
			
}