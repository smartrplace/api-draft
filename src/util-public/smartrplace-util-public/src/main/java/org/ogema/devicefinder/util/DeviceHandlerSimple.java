package org.ogema.devicefinder.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
import org.ogema.core.model.schedule.AbsoluteSchedule;
import org.ogema.core.model.simple.BooleanResource;
import org.ogema.core.model.simple.FloatResource;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.model.simple.TimeResource;
import org.ogema.core.resourcemanager.pattern.ResourcePattern;
import org.ogema.core.resourcemanager.pattern.ResourcePatternAccess;
import org.ogema.devicefinder.api.Datapoint;
import org.ogema.devicefinder.api.DatapointService;
import org.ogema.devicefinder.api.DeviceHandlerProvider;
import org.ogema.devicefinder.api.InstalledAppsSelector;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.ogema.tools.resource.util.ResourceUtils;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.tissue.util.resource.GatewaySyncUtil;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.util.resource.ResourceHelper;
import de.iwes.widgets.api.widgets.WidgetPage;
import de.iwes.widgets.api.widgets.localisation.OgemaLocale;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.html.form.label.Label;

/** Standard base implementation for a {@link DeviceHandlerProvider} providing just one value in the device table overview.
 * Note that the package and class name of a {@link DeviceHandlerProvider} should not be refactored after productive usage
 * started as the {@link #id()} method by default uses the class and package name and this id is stored persistently in the
 * {@link InstallAppDevice} resource of the device.<br>
 * For more complex {@link DeviceHandlerProvider}s you may also implement {@link DeviceHandlerBase} directly.<br>
 * 
 * Do not forget to register the DeviceHandler implementation as OSGi service! For examples (where most implementations
 * are registered) see class MonitoringServiceBaseApp.
 *
 * @param <T> device resource type of the DeviceHandlerProvider
 */
public abstract class DeviceHandlerSimple<T extends PhysicalElement> extends DeviceHandlerBase<T> {
	public static final long DEFAULT_POLL_RATE = 5000;
	
	protected final ApplicationManagerPlus appMan;
	protected final DatapointService dpService;
	protected final boolean isInRoom;
	
	/**
	 * 
	 * @param appMan
	 * @param dpService
	 * @param isInRoom if true a configuration will be should that allows to select a room for the device. If false
	 * 		this means that setting a room for the device would not make sense and this no room selector shall be shown
	 */
	public DeviceHandlerSimple(ApplicationManager appMan, DatapointService dpService, boolean isInRoom) {
		this(new ApplicationManagerPlus(appMan, null, dpService, null), isInRoom);
	}
	/**
	 * 
	 * @param appMan
	 * @param isInRoom if true a configuration will be should that allows to select a room for the device. If false
	 * 		this means that setting a room for the device would not make sense and this no room selector shall be shown
	 */
	public DeviceHandlerSimple(ApplicationManagerPlus appMan, boolean isInRoom) {
		this.appMan = appMan;
		this.dpService = appMan.dpService();
		this.isInRoom = isInRoom;
if(Boolean.getBoolean("jobdebug")) {
	Runtime rt = Runtime.getRuntime();
	System.out.println(this.getClass().getSimpleName()+" created with free memory: "+rt.freeMemory()/(1024*1024));
}
	}
		
	/** Additional widgets for the standard table can be added here. To add widgets for the expert table,
	 * overwrite the method {@link #addMoreWidgetsExpert(InstallAppDevice, Resource, ObjectResourceGUIHelper, String, OgemaHttpRequest, Row, ApplicationManager)}
	 * @param object
	 * @param device
	 * @param vh
	 * @param id
	 * @param req
	 * @param row
	 * @param appMan
	 */
	protected void addMoreValueWidgets(InstallAppDevice object, T device, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan, Alert alert) {}
	
	/** See {@link DeviceHandlerProvider#getDatapoints(InstallAppDevice, DatapointService)}.
	 * Note that you should call {@link #addDatapoint(SingleValueResource, java.util.List)} for each datapoint. This
	 * makes sure only datapoints that really exist are registered
	 *
	 * @param device
	 * @param deviceConfiguration
	 * @return
	 */
	protected abstract Collection<Datapoint> getDatapoints(T device, InstallAppDevice deviceConfiguration);
	
	/** Overwrite this method to set own column titles. If you define new titles you should add respective row-widgets via
	 * {@link #addMoreValueWidgets(InstallAppDevice, PhysicalElement, ObjectResourceGUIHelper, String, OgemaHttpRequest, Row, ApplicationManager, Alert)}
	 * @return if true the device handler has set own titles, otherwise the default titles will be used. Note that additional titles can just be
	 * 		provided via addMoreValueWidgets, this method is only required if standard columns shall be omitted or new entries shall be put before the
	 * 		standard columns
	 */
	protected boolean setColumnTitlesToUse(ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh) {
		return false;
	}
	/** Overwrite this to provide a special value title e.g. including unit information */
	protected String getValueTitle() {
		return "Value";
	}
	
	/** The value and last contact labels are polled. You can adapt the poll rate here*/
	protected long getLabelPollRate() {
		return DEFAULT_POLL_RATE;
	}
	
	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector) {
		throw new IllegalStateException("Only with config supported for DeviceHandlerSimple!");
	}
	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector, DeviceTableConfig config) {
		final boolean emptyExternal = config.emptyStateControlledExternally();
		return new DeviceTableBase(page, appMan, alert, appSelector, this, emptyExternal) {
			@Override
			protected String pid() {
				return WidgetHelper.getValidWidgetId(DeviceHandlerSimple.this.getClass().getName());
			}
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				if(req == null) {
					if(setColumnTitlesToUse(vh)) {
						appSelector.addWidgetsExpert(DeviceHandlerSimple.this, object, vh, id, req, row, appMan);
						return;
					}
				}
				
				@SuppressWarnings("unchecked")
				final T box = (T)addNameWidget(object, vh, id, req, row, appMan, config.showOnlyBaseCols()).getLocationResource();

				if(req == null) {
					vh.registerHeaderEntry(getValueTitle());
					vh.registerHeaderEntry("Last Contact");
				} else {
					Label valueLabel;
					try {
						valueLabel = getMainSensorLabel(box, object, vh, id);
					} catch(ClassCastException e) {
						e.printStackTrace();
						return;
					}
					SingleValueResource sampleSensor = getMainSensorValue(box, object);
					if((valueLabel == null) && (sampleSensor != null)) {
						if(sampleSensor instanceof FloatResource)
							valueLabel = vh.floatLabel(getValueTitle(), id, (FloatResource)sampleSensor, row, "%.1f");
						else if(sampleSensor instanceof IntegerResource)
							valueLabel = vh.intLabel(getValueTitle(), id, (IntegerResource)sampleSensor, row, 0);
						else if(sampleSensor instanceof TimeResource) {
							if(sampleSensor.getName().contains("cnt") || sampleSensor.getName().contains("count"))
								valueLabel = vh.timeLabel(getValueTitle(), id, (TimeResource)sampleSensor, row, 7);
							else
								valueLabel = vh.timeLabel(getValueTitle(), id, (TimeResource)sampleSensor, row, 0);
						} else if(sampleSensor instanceof BooleanResource)
							valueLabel = vh.booleanLabel(getValueTitle(), id, (BooleanResource)sampleSensor, row, 0);
						else
							throw new IllegalStateException("Unsupported sensor type: "+sampleSensor.getResourceType().getName());
					} else {
						row.addCell(getValueTitle(), valueLabel);
					}
					if(sampleSensor != null) {
						Label lastContact = addLastContact(vh, id, req, row, sampleSensor);
						
						valueLabel.setPollingInterval(DEFAULT_POLL_RATE, req);
						lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
					}
				}
				
				if(!config.showOnlyBaseCols())
					addMoreValueWidgets(object, box, vh, id, req, row, appMan, alert);
				
				if(isInRoom) {
					Room deviceRoom =  box.location().room();
					addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				}
				addSubLocation(object, vh, id, req, row);
				if(!config.showOnlyBaseCols())
					addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);

				appSelector.addWidgetsExpert(DeviceHandlerSimple.this, object, vh, id, req, row, appMan);
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandlerSimple.this.getResourceType();
			}
			
			@Override
			protected String id() {
				return DeviceHandlerSimple.this.id();
			}
		};
	}
	
	/** You have to provide a resource pattern to find the devices that shall be processed by the
	 * {@link DeviceHandlerProvider}. If there are also other DeviceHandlerProviders working on the
	 * same device ResourceType then you have to make sure that the {@link ResourcePattern#accept()} method
	 * of the patterns make sure that each device of the type is assigned to exactly one DeviceHandlerProvider.
	 */
	@Override
	protected abstract Class<? extends ResourcePattern<T>> getPatternClass() ;

	@Override
	protected ResourcePatternAccess advAcc() {
		return appMan.getResourcePatternAccess();
	}
	
	Map<String, Collection<Datapoint>> knownDpsMap = new HashMap<>();
	@SuppressWarnings("unchecked")
	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService) {
		T device = (T)installDeviceRes.device().getLocationResource();
		if(Boolean.getBoolean("org.ogema.devicefinder.util.updateDatapoints")) {
			Collection<Datapoint> result = getDatapoints(device, installDeviceRes);
			for(Datapoint dp: result) {
				DpGroupUtil.setFinalDeviceForDatapoint(dp, installDeviceRes);
			}
			return result;
		}
		Collection<Datapoint> knownDps = knownDpsMap.get(installDeviceRes.getLocation());
		if(knownDps == null) {
			try {
				knownDps = getDatapoints(device, installDeviceRes);
				knownDpsMap.put(installDeviceRes.getLocation(), knownDps);
				
				for(Datapoint dp: knownDps) {
					DpGroupUtil.setFinalDeviceForDatapoint(dp, installDeviceRes);
				}
			} catch(ClassCastException e) {
				(new IllegalStateException("Wrong device type: "+installDeviceRes.getName(), e)).printStackTrace();
				return Collections.emptyList();
			}
			if(Boolean.getBoolean("org.smartrplace.apps.subgateway") && addDeviceOrResourceListToSync()) {
				if(device.getLocation().startsWith("EvalCollection"))
					return knownDps;
				if(DeviceTableBase.isHomematic(device.getLocation()))
					return knownDps;
				Resource topLevel = ResourceHelper.getToplevelResource(device);
				GatewaySyncUtil.registerToplevelDeviceForSyncAsClient(topLevel, appMan.appMan());
			}
		}
		return knownDps;			
	}
	
	/** Wrapper for {@link #addDatapoint(SingleValueResource, List, String, DatapointService)}
	 * 
	 * @param absoluteSchedule
	 * @param result
	 * @return
	 */
	public Datapoint addDatapoint(SingleValueResource res, List<Datapoint> result) {
		return super.addDatapoint(res, result, dpService);
	}
	protected Datapoint addDatapoint(SingleValueResource res, String subLocation, List<Datapoint> result) {
		return super.addDatapoint(res, result, subLocation, dpService);
	}
	protected Datapoint addDatapoint(AbsoluteSchedule res, List<Datapoint> result) {
		return super.addDatapoint(res, result, dpService);
	}
	
	protected Datapoint addDatapointWithResOrSensorName(SingleValueResource res, List<Datapoint> result) {
		String resName = res.getName();
		final String subLocation;
		if(!(resName.equals("reading") || resName.equals("stateFeedback") || resName.equals("stateControl")))
			subLocation = resName;
		else {
			Resource parent = res.getParent();
			if(parent != null)
				subLocation = ResourceUtils.getHumanReadableShortName(parent);
			else
				subLocation = "TOP";
		}
		return addDatapoint(res, subLocation, result);
	}
	

	/** Doubled from {@link DeviceTableRaw}
	 */
	protected Label addLastContact(String columnLabel, ObjectResourceGUIHelper<?,?> vh, String id,
			OgemaHttpRequest req, Row row, 
			SingleValueResource reading) {
		if(columnLabel == null)
			columnLabel = "Last Contact";
		Label lastContact = null;
		if(req != null) {
			lastContact = new LastContactLabel(reading, appMan.appMan(), vh.getParent(), WidgetHelper.getValidWidgetId(columnLabel)+id, req);
			row.addCell(WidgetHelper.getValidWidgetId(columnLabel), lastContact);
			return lastContact;
		} else
			vh.registerHeaderEntry(columnLabel);
		return null;
	}
	
	@Override
	public String label(OgemaLocale locale) {
		return getTableTitle();
	}
}
