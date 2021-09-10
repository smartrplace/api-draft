package org.ogema.devicefinder.api;

import java.util.Collection;

import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.extended.alarming.AlarmGroupData;

import de.iwes.widgets.template.LabelledItem;

/** Group of ongoing alarms that shall be handeled as a single alarm towards the user. The
 * ongoing alarms are not stored persistently, but some information is still stored in {@link AlarmGroupData}<br>
 * Note that such a group is created and fully controlled by the creating {@link AlarmingExtension}.*/
public interface AlarmOngoingGroup extends LabelledItem {
	/** List of alarm configurations that are in alarm state. The base alarms are stored with
	 * very limited information*/
	Collection<AlarmConfiguration> baseAlarms();
	
	/** If true then no virtual AlarmOngoingGroup is created for the base alarms, otherwise
	 * the base alarm must be shown as a separate alarm to the user
	 */
	default boolean consumesBaseAlarms() {return true;}
	
	/** Persistent data of the object. Note that resources for which no object is created anymore
	 * shall be deleted
	 */
	AlarmGroupData getResource(boolean forceToCreate);
	
	AlarmingExtension sourceOfGroup();
	
	/** TODO: for now we do not define contributors, each alarming group is just generated by a single extension*/
	//Set<AlarmingExtension> contributors();
	
	AlarmingGroupType getType();
	
	/** See {@link AlarmingService#finishOngoingGroup(String)}
	 */
	boolean isFinished();
}
