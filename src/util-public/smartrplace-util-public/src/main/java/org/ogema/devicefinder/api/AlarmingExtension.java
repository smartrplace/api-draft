package org.ogema.devicefinder.api;

import java.util.List;

import org.ogema.core.model.simple.SingleValueResource;
import org.ogema.core.resourcemanager.ResourceValueListener;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.locations.Room;
import org.smartrplace.apps.alarmingconfig.model.eval.EvaluationByAlarmingOption;
import org.smartrplace.apps.hw.install.config.InstallAppDevice;
import org.smartrplace.util.directobjectgui.ObjectResourceGUIHelper;

import de.iwes.widgets.api.messaging.MessagePriority;
import de.iwes.widgets.api.widgets.sessionmanagement.OgemaHttpRequest;
import de.iwes.widgets.html.alert.Alert;
import de.iwes.widgets.html.complextable.RowTemplate.Row;
import de.iwes.widgets.template.LabelledItem;

/** An alarming extension defines an additional alarming evaluation that can be applied
 * to any {@link SingleValueResource}. AlarmingExtensions can have two different functions, which
 * can be combined in principal, but usually only of them is used:<br>
 * - Provision of an alternative {@link AlarmingExtensionListener}, check there for details. Gets notifications on all new
 * values, cannot stop/change the standard value limit/no-value alarm, separate listener object for each SingleValueResource<br>
 * - Generation of alarming groups. Gets notifications on new standard alarms, can stop them to create own alarms,
 * common object receives notifications for all alarms
 * 
 * TODO: The registration mechanism for value listeners for group/room/gateway alarming via {@link #onStartAlarming()} could have an explicit API
 * method to make this task easier and more transparent for developers. Maybe it would make more sense to let
 * such alarming evaluations register their own {@link ResourceValueListener}s themselves anyways, then we do not
 * need any additional API.<br>
 */
public interface AlarmingExtension extends LabelledItem {
	/** The method shall be able to handle a null argument asking whether the extension
	 * shall be offered at all on the general page
	 * @param res
	 * @return
	 */
	boolean offerInGeneralAlarmingConfiguration(AlarmConfiguration ac);
	default boolean offerInDeviceConfiguration(InstallAppDevice device) {return false;}
	default boolean offerInRoomConfiguration(Room room) {return false;}
	default boolean offerInGatewayConfiguration(String gwId) {return false;}
	
	/** Provide listener for a certain SingleValueResource. This is only called for
	 * alarming configurations for which the extension is registered via {@link AlarmConfiguration#alarmingExtensions()}.
	 * Registration can be done manualle via GUI if {@link #offerInGeneralAlarmingConfiguration(AlarmConfiguration)}
	 * is true for the AlarmConfiguration or could be done by the extension itself in {@link #onStartAlarming()}*/
	AlarmingExtensionListener getListener(SingleValueResource res, AlarmConfiguration ac);
	
	/** Called when alarming is started. The extension may define an own timer here that
	 * shall be closed on stop. This can be used to perform actions when no alarm occurs. Also
	 * registration for {@link AlarmingExtensionListener}s via {@link AlarmConfiguration#alarmingExtensions()} could
	 * be done here.
	 * @param baseAlarm can be used to send alarms e.g. based on timer*/
	default void onStartAlarming(BaseAlarmI baseAlarm) {}

	/** Called when alarming is stopped*/
	default void onStopAlarming() {}
	
	/**Return true if the extension shall receive notifications, which is usually relevant
	 * if alarm groups shall be generated
	 */
	default boolean getAlarmNotifications( ) {return false;}
	
	public static class AlarmNotificationResult {
		/** If true the alarm shall be generated into a message by the framework directly.
		 * Otherwise the {@link AlarmingExtension} will generate one or more messages for
		 * the groups affected via {@link BaseAlarm#sendMessage(String, String, MessagePriority)}
		 */
		public boolean sendAlarmDirectly;
	}
	
	public enum MessageDestination {
		PROVIDER_FIRST,
		CUSTOMER_FIRST,
		BOTH_IMMEDIATELY
	}
	public static interface BaseAlarmI {
		/** Send out message via AlarmingManager
		 * 
		 * @param title
		 * @param message
		 * @param prio
		 * @param md if null default or the setting of the alarming config of the {@link SingleValueResource} is
		 * 		used if such is available in context
		 */
		void sendMessage(String title, String message, MessagePriority prio, MessageDestination md);		
	}
	public static abstract class BaseAlarm implements BaseAlarmI {
		public AlarmConfiguration ac;
		public String title;
		public String message;
		/** null if generated by framework*/
		public AlarmingExtension source;
		public boolean isNoValueAlarm;
		public boolean isRelease;
		
		//@Override
		//public abstract void sendMessage(String title, String message, MessagePriority prio, MessageDestination md);
	}
	
	/** Notification on a new alarm generated by another extension*/
	default AlarmNotificationResult newAlarmNotification(BaseAlarm baseAlarm) {return null;}
	
	/** Extension for device evaluation and configuration. Device alarming is implemented via the methods as
	 * explained above.
	 *
	 * @param <T> resource type storing general alarming and evaluation configurations
	 */
	public static interface EvaluationConfigurationExtensionDevice<T extends EvaluationByAlarmingOption> {
		void addWidgetsConfigRow(T object,
				ObjectResourceGUIHelper<T, T> vh, String id, OgemaHttpRequest req, Row row, Alert alert) ;
		
		Class<T> getType();
		
		/** Update evaluation. We provide a list of devices here, but only a single configuration and interval
		 * 
		 * @param devices
		 * @param config
		 * @param start the interval may not be given, then everything shall be recalcuated
		 * @param end
		 * @param useDefaultDatapoints if false then a new set of datapoints shall be created to avoid
		 * 		overriding previous calculation results. If true then the evaluation (re)-uses its default
		 * 		datapoints.
		 * @return
		 */
		List<Datapoint> updateEvaluation(List<InstallAppDevice> devices, T config, Long start, Long end,
				boolean useDefaultDatapoints);
		
		/** Only relevant if {@link #updateEvaluation(List, EvaluationByAlarmingOption, Long, Long, boolean)} with
		 * useDefaultDatapoints=false is used. This resets the internal datapoint counter which should then
		 * start to overwrite previously used datapoints.
		 * @return number of datapoints that were used up to now
		 */
		int resetInternalDatapoints();
	}
	
	/** If the extension offers a configuration table and
	 * evaluation support then return implementation here*/
	default EvaluationConfigurationExtensionDevice<?> getEvalConfigExtension() {return null;}
}
