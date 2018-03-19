package de.smartrplace.app.heatcontrol.overview.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtPoint;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtRoomData;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import ch.qos.logback.core.db.dialect.MySQLDialect;
import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.checkbox.SimpleCheckbox;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.smartrplace.app.heatcontrol.overview.HeatControlOverviewController;
import de.smartrplace.app.heatcontrol.overview.config.HeatcontrolOverviewData;


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
		HeatcontrolOverviewData configRes = object.getRoomExtensionData(true, HeatcontrolOverviewData.class);
		
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
		if(object.getThermostats() != null) {
			boolean hasManualModeControl = false;
			boolean allManualModeControl = true;
			Room room = object.getRoom();
			for(Thermostat th: object.getThermostats()) {
				BooleanResource setManualMode = th.getSubResource("setManualMode", BooleanResource.class);
				if((setManualMode != null)&&setManualMode.isActive()) {
					hasManualModeControl = true;
				} else {
					allManualModeControl = false;
				}
			}
			if(hasManualModeControl) {
				String adder = "";
				if(hasManualModeControl && !allManualModeControl) adder = "*";
				Map<String, String> valuesToSet = new HashMap<>();
				valuesToSet.put("0", "No control"+adder);
				valuesToSet.put("1", "Set Manual"+adder);
				valuesToSet.put("2", "Thermostat button switch detection"+adder);
				vh.dropdown("Control Mode", id, configRes.controlManualMode(), row, valuesToSet );
			} else {
				vh.stringLabel("Control Mode", id, "Control Mode not supported", row);
			}
		} else {
			vh.registerHeaderEntry("Control Mode");
		}
		
	}

	@Override
	public Room getResource(HeatControlExtRoomData object, OgemaHttpRequest req) {
		return object.getRoom();
	}
	
	public static abstract class ResourceWidgetAdder<R extends Resource> {
		public ResourceWidgetAdder(Resource parent, String subResourceName, Class<R> resourceType, String widgetName,
				ObjectResourceGUIHelper<HeatControlExtRoomData, Room> vh) {
			if((parent == null)||(subResourceName == null)) {
				vh.registerHeaderEntry(widgetName);
				return;
			}
			R resource = parent.getSubResource(subResourceName, resourceType);
			if((resource == null)||(!resource.isActive())) {
				return;
			}
			addWidget(resource, widgetName);
			return;
		}
		protected abstract void addWidget(R resource, String widgetName);
	}
}
