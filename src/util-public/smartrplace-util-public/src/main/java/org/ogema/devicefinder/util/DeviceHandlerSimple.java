package org.ogema.devicefinder.util;

import java.util.Collection;
import java.util.List;

import org.ogema.accessadmin.api.ApplicationManagerPlus;
import org.ogema.core.application.ApplicationManager;
import org.ogema.core.model.Resource;
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
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;
import org.smartrplace.util.format.WidgetHelper;

import de.iwes.widgets.api.widgets.WidgetPage;
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
	
	private final ApplicationManagerPlus appMan;
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
	}
	
	/** Provide a sensor value of the device that usually should be updated most frequently and that also
	 * should be most relevant for the user to check whether the device works as it should.
	 * @param device device resource
	 * @param deviceConfiguration configuration resource that contains additional configuration information
	 * @return sensor resource to be displayed in the overview table. If the device has no sensor this should be
	 * 		a feedback or status value of an actor or most relevant configuration value
	 */
	protected abstract SingleValueResource getMainSensorValue(T device, InstallAppDevice deviceConfiguration);
	protected void addMoreValueWidgets(InstallAppDevice object, T device, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
			String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {}
	
	/** See {@link DeviceHandlerProvider#getDatapoints(InstallAppDevice, DatapointService)}.
	 * Note that you should call {@link #addDatapoint(SingleValueResource, java.util.List)} for each datapoint. This
	 * makes sure only datapoints that really exist are registered
	 *
	 * @param device
	 * @param deviceConfiguration
	 * @return
	 */
	protected abstract Collection<Datapoint> getDatapoints(T device, InstallAppDevice deviceConfiguration);
	
	/** Set title of table listing the devices processed by the DeviceHandlerProvider*/
	protected abstract String getTableTitle();
	
	/** The value and last contact labels are polled. You can adapt the poll rate here*/
	protected long getLabelPollRate() {
		return DEFAULT_POLL_RATE;
	}
	
	@Override
	public DeviceTableBase getDeviceTable(WidgetPage<?> page, Alert alert,
			InstalledAppsSelector appSelector) {
		return new DeviceTableBase(page, appMan, alert, appSelector, this) {
			@Override
			protected String pid() {
				return WidgetHelper.getValidWidgetId(DeviceHandlerSimple.this.getClass().getName());
			}
			
			@Override
			public void addWidgets(InstallAppDevice object, ObjectResourceGUIHelper<InstallAppDevice, InstallAppDevice> vh,
					String id, OgemaHttpRequest req, Row row, ApplicationManager appMan) {

				@SuppressWarnings("unchecked")
				final T box = (T)addNameWidget(object, vh, id, req, row, appMan);

				SingleValueResource sampleSensor = getMainSensorValue(box, object);
				Label valueLabel;
				if(sampleSensor instanceof FloatResource)
					valueLabel = vh.floatLabel("Value", id, (FloatResource)sampleSensor, row, "%.1f");
				else if(sampleSensor instanceof IntegerResource)
					valueLabel = vh.intLabel("Value", id, (IntegerResource)sampleSensor, row, 0);
				else if(sampleSensor instanceof TimeResource)
					valueLabel = vh.timeLabel("Value", id, (TimeResource)sampleSensor, row, 0);
				else if(sampleSensor instanceof BooleanResource)
					valueLabel = vh.booleanLabel("Value", id, (BooleanResource)sampleSensor, row, 0);
				else
					throw new IllegalStateException("Unsupported sensor type: "+sampleSensor.getResourceType().getName());
				
				Label lastContact = addLastContact(vh, id, req, row, sampleSensor);
				
				addMoreValueWidgets(object, box, vh, id, req, row, appMan);
				
				if(req != null) {
					valueLabel.setPollingInterval(DEFAULT_POLL_RATE, req);
					lastContact.setPollingInterval(DEFAULT_POLL_RATE, req);
				}

				if(isInRoom) {
					Room deviceRoom = box.location().room();
					addRoomWidget(vh, id, req, row, appMan, deviceRoom);
				}
				addSubLocation(object, vh, id, req, row);
				addInstallationStatus(object, vh, id, req, row);
				addComment(object, vh, id, req, row);

				appSelector.addWidgetsExpert(object, vh, id, req, row, appMan);
			}
			
			@Override
			protected Class<? extends Resource> getResourceType() {
				return DeviceHandlerSimple.this.getResourceType();
			}
			
			@Override
			protected String id() {
				return DeviceHandlerSimple.this.id();
			}

			@Override
			protected String getTableTitle() {
				return DeviceHandlerSimple.this.getTableTitle();
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
	
	@SuppressWarnings("unchecked")
	@Override
	public Collection<Datapoint> getDatapoints(InstallAppDevice installDeviceRes, DatapointService dpService) {
		T device = (T)installDeviceRes.device();
		Collection<Datapoint> result = getDatapoints(device, installDeviceRes);
		checkDpSubLocations(installDeviceRes, result);
		return result;
	}
	
	/** Wrapper for {@link #addDatapoint(SingleValueResource, List, String, DatapointService)}
	 * 
	 * @param res
	 * @param result
	 * @return
	 */
	protected Datapoint addDatapoint(SingleValueResource res, List<Datapoint> result) {
		return super.addDatapoint(res, result, dpService);
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
}
