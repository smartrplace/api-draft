package de.smartrplace.app.heatcontrol.overview.gui;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.model.units.TemperatureResource;
import org.ogema.model.devices.buildingtechnology.Thermostat;
import org.ogema.model.locations.Room;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtPoint;
import org.smartrplace.apps.heatcontrol.extensionapi.HeatControlExtRoomData;
import org.smartrplace.util.directobjectgui.ObjectGUITablePage;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.ValueFormat;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.format.StringFormatHelper;
import de.iwes.widgets.api.extended.WidgetData;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.html.StaticTable;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.button.Button;
import de.iwes.widgets.html.form.checkbox.SimpleCheckbox;
import de.iwes.widgets.html.form.label.Header;
import de.iwes.widgets.html.form.label.Label;
import de.iwes.widgets.resource.widget.textfield.ValueResourceTextField;
import de.smartrplace.app.heatcontrol.common.util.RoomDataUtil;
import de.smartrplace.app.heatcontrol.overview.HeatControlOverviewController;
import de.smartrplace.app.heatcontrol.overview.config.GlobalHeatcontrolOverviewData;
import de.smartrplace.app.heatcontrol.overview.config.HeatcontrolOverviewData;


/**
 * An HTML page, generated from the Java code.
 */
public class MainPage extends ObjectGUITablePage<HeatControlExtRoomData, Room>{
	public static final float MIN_COMFORT_TEMP = 4;
	public static final float MAX_COMFORT_TEMP = 30;
	public static final float DEFAULT_COMFORT_TEMP = 21;
	
	final private HeatControlExtPoint heatExtPoint;
	final private GlobalHeatcontrolOverviewData myGlobalData;
	private final HeatControlOverviewController app;
	
	ValueResourceTextField<TimeResource> updateInterval;

	public MainPage(final WidgetPage<?> page, final HeatControlOverviewController app,
			HeatControlExtRoomData initData) {
		super(page, app.appMan, initData);
		this.app = app;
		this.heatExtPoint = app.serviceAccess.heatExtPoint;
		this.myGlobalData = heatExtPoint.extensionData(true, GlobalHeatcontrolOverviewData.class);
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
		
		updateInterval = new ValueResourceTextField<TimeResource>(page, "updateInterval") {
			private static final long serialVersionUID = 1L;
			@Override
			public void onGET(OgemaHttpRequest req) {
				selectItem(myGlobalData.updateRate(), req);
			}
		};
		
		StaticTable topTable = new StaticTable(1, 3);
		topTable.setContent(0, 0, ecoModeCheck).setContent(0, 1, "Update Rate (minutes):").setContent(0, 2, updateInterval);
		page.append(topTable);
	}
	
	@Override
	public Collection<HeatControlExtRoomData> getObjectsInTable(OgemaHttpRequest req) {
		List<HeatControlExtRoomData> providers = heatExtPoint.getRoomsControlled(); 
		return providers;
	}

	@Override
	public void addWidgets(HeatControlExtRoomData object, ObjectResourceGUIHelper<HeatControlExtRoomData, Room> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {
		HeatcontrolOverviewData configRes = object.getRoomExtensionData(true, HeatcontrolOverviewData.class);
		if (object.getRoom() == null)
			return;
		String roomName = (req != null) ? ResourceUtils.getHumanReadableShortName(object.getRoom()) : "";
		if (configRes != null) id = roomName + id;
		Label sl = vh.stringLabel("Room name", id, roomName, row);
if(configRes != null) try {Thread.sleep(1000);} catch (InterruptedException e) {e.printStackTrace();}
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
	
			String columnId = ResourceUtils.getValidResourceName("T/Setp/Valve/H/Open/Motion/Manu");
			String widgetId = columnId + id;
			Label dataLabel = new Label(vh.getParent(), widgetId, req) {
				private static final long serialVersionUID = 4515005761380513466L;
				@Override
				public void onGET(OgemaHttpRequest req) {
					long remaining = object.getRemainingDirectThermostatManualDuration();
					if(remaining > 0 && remaining < 1000) remaining = remaining * 60000;
					String text = ValueFormat.celsius(RoomDataUtil.getRoomTemperatureMeasurement(object.getRoomTemperatureSensors(), object.getThermostats()), 1)+" / "+
							ValueFormat.celsius(object.getCurrentTemperatureSetpoint())+" / "+
							ValueFormat.floatVal(RoomDataUtil.getTotalValveOpening(object.getThermostats()), "%.2f")+" / "+
							ValueFormat.humidity(RoomDataUtil.getAverageRoomHumditiyMeasurement(object.getRoomHumiditySensors()))+" / "+
							RoomDataUtil.getNumberOpenWindows(object.getWindowSensors()) +" / "+
							(object.isUserPresent()?"1":"0") +" / "+
							getFormattedDurationValue(remaining, 300);
					setText(text, req);
				}
			};
			if(myGlobalData.updateRate().getValue() > 0)
				dataLabel.setPollingInterval(myGlobalData.updateRate().getValue(), req);
			row.addCell(columnId, dataLabel);
		} else {
			vh.registerHeaderEntry("Therm/TH/Win");
			vh.registerHeaderEntry("T/Setp/Valve/H/Open/Motion/Manu");
		}
		if(configRes != null)
			vh.floatEdit("Comfort Temp.", id, configRes.comfortTemperature(), row, alert, MIN_COMFORT_TEMP, MAX_COMFORT_TEMP, "Value not allowed");
		else
			vh.registerHeaderEntry("Comfort Temp.");
		new ServiceValueEdit("At-Thermostat Duration", id, row, alert, 0, 99999, "Value not allowed", vh) {

			@Override
			protected String getValue(OgemaHttpRequest req) {
				return object.getAtThermostatManualSettingDuration()/60000+" min";
			}

			@Override
			protected void setValue(float value, OgemaHttpRequest req) {
				object.setAtThermostatManualSettingDuration((long) (value*60000));
			}
		};
		
		if(object.getThermostats() != null) {
			boolean hasManualModeControl = false;
			boolean allManualModeControl = true;
			boolean hasModeFeedback = false;
			boolean allModeFeedback = true;
			boolean hasBoostMode = false;
			boolean allBoostMode = true;
				for(Thermostat th: object.getThermostats()) {
				if(getActiveManualModeControl(th) != null) {
					hasManualModeControl = true;
				} else {
					allManualModeControl = false;
				}
				if(getActiveModeFeedback(th) != null) {
					hasModeFeedback = true;
				} else {
					allModeFeedback = false;
				}
				if(getBoostControl(th) != null) {
					hasBoostMode = true;
				} else {
					allBoostMode = false;
				}
			}
			if(hasManualModeControl) {
				String adder = "";
				if(hasManualModeControl && !allManualModeControl) adder = "*";
				Map<String, String> valuesToSet = new HashMap<>();
				valuesToSet.put("0", "No control"+adder);
				valuesToSet.put("1", "Set Manual"+adder);
				if(hasModeFeedback) {
					String adderFB = "";
					if(!allModeFeedback) adderFB = "!!";
					valuesToSet.put("2", "Thermostat button switch detection"+adderFB+adder);
					//TODO: Boosting like this does not really work. This seems to interfere with
					//mode switching and temperature setting
					/*if(hasBoostMode) {
						String adderBoost = "";
						if(!allBoostMode) adderBoost = "??";
						valuesToSet.put("3", "Thermostat button switch detection with boost"+adderBoost+adder);
					}*/
				}
				vh.dropdown("Control Mode", id, configRes.controlManualMode(), row, valuesToSet );
			} else {
				vh.stringLabel("Control Mode", id, "Control Mode not supported", row);
			}
			if(hasBoostMode) {
				Button boostButton = new Button(vh.getParent(), "boostButton"+id, "Boost->CT"+(allBoostMode?"":"*"), req) {
					private static final long serialVersionUID = 2903972124650772289L;
					@Override
					public void onPOSTComplete(String data, OgemaHttpRequest req) {
						app.startBoost(object);
					}
				};
				row.addCell("Boost", boostButton);				
			} else {
				vh.stringLabel("Boost", id, "Boost not supported", row);
			}
		} else {
			vh.registerHeaderEntry("Control Mode");
			vh.registerHeaderEntry("Boost");
		}
		
	}
	
	@Override
	public String getLineId(HeatControlExtRoomData object) {
		String roomName = WidgetHelper.getValidWidgetId(ResourceUtils.getHumanReadableShortName(object.getRoom()));
		return roomName + super.getLineId(object);
	}
	
	public static TemperatureResource getActiveManualModeControl(Thermostat th) {
		TemperatureResource setManualMode = th.getSubResource("setManuMode", TemperatureResource.class);
		if((setManualMode != null)&&setManualMode.isActive()) return setManualMode;
		else return null;
	}
	public static IntegerResource getActiveModeFeedback(Thermostat th) {
		IntegerResource setManualMode = th.getSubResource("controlMode", IntegerResource.class);
		if((setManualMode != null)&&setManualMode.isActive()) return setManualMode;
		else return null;
	}
	public static BooleanResource getBoostControl(Thermostat th) {
		BooleanResource setBoostMode = th.getSubResource("setBoostMode",BooleanResource.class);
		if((setBoostMode != null)&&setBoostMode.isActive()) return setBoostMode;
		else return null;
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
	
	public static String getFormattedDurationValue(long deltaT, int maxMinutesSecond) {
    	if(deltaT < 0) {
    		return "--";
    	}
		deltaT = deltaT / 1000;
		if(deltaT < maxMinutesSecond) {
			return String.format("%d sec", deltaT);
		}
		deltaT /= 60;
		if(deltaT < maxMinutesSecond) {
			return String.format("%d min", deltaT);
		}
		return StringFormatHelper.getFormattedValue(deltaT);
	}
}
