package org.ogema.recordreplay.testing;

import java.util.ArrayList;
import java.util.List;

import org.ogema.core.channelmanager.measurements.SampledValue;
import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.core.model.simple.IntegerResource;
import org.ogema.devicefinder.util.AlarmingConfigUtil;
import org.ogema.model.extended.alarming.AlarmConfiguration;
import org.ogema.model.prototypes.Data;
import org.ogema.model.recplay.testing.RecReplayDeviation;
import org.ogema.model.recplay.testing.RecReplayObserverData;
import org.ogema.recordreplay.testing.RecReplayObserver.EventRecorded;

import de.iwes.util.resource.ValueResourceHelper;
import de.iwes.widgets.template.LabelledItem;

/** An observer for Recording-Replay Testing can record certain behaviour e.g. of elements in a database or
 * the OGEMA alarming and check whether a new software version shows the the same behaviour as the recorded
 * reference for a certain set of input.<br>
 * Usually each observer shall be started after the relevant system setup on the testing instance has been finished
 * e.g. after all resources have been loaded and configurations that are only performed on the development system
 * are made. If also the configuration system shall be tested then start may be performed earlier. Usually each
 * observer shall be started by the relevant configuration bundle, e.g. alarming-app for the alarming observers. This
 * also limits timing deviations due to differences in startup-process although this does not eliminate the issue as
 * parallel or later startup processes may still consume processor time.<br>
 * Usually each observer is not a separate app or service but just a class instantiated and started from a suitable
 * app. For each observer a property starting with org.ogema.recordreplay.testing shall be defined which defines
 * whether it is a test systen starting the observer.<br>
 * In recording mode the observer works on its own. The RecReplay app is not required in this mode, but may provide a
 * GUI for some control and to access some statistical evaluation provided by the app. In replay-testing mode the
 * RecReplay app controls the testing (to discuss).<br>
 * For now the entireRecReply system is either in recording or in replay-test mode. The limitation of this approach is
 * that the Recplay-system itself cannot be tested by its own mechanism. For the initial phase this is acceptable, although
 * we may add the functionality later on.<br>
 * <br>
 ***********************<br>
 *  Timing<br>
 ***********************<br>
 * The property org.ogema.defaultclock.timestamp shall be set to the same value for recording and for replay-test. All
 * timestamps are stored as absolute values fitting this framework time start value. If external systems are connected
 * they also need to run with this simulated time if relevant (calendar systems just must maintain the relevant calendar
 * events). A correction due to differing startup times may be introduced in the future, but for now we just rely on
 * using the same framework time start values.
 */
public interface RecReplayObserver extends LabelledItem {

	Class<? extends RecReplayObserverData> resourceType();
	
	public static abstract class EventRecorded {
		public long timeExpected;
		public String description;
		
		/** overwrite this with your reference type*/
		public abstract Resource reference();

		public RecReplayObserver observer;

		//********************************************
		//The following elements are for replay only
		//********************************************
		
		/** null: not expired
		 * true: successful
		 * false: failed (entry for {@link RecReplayDeviation} must be set)
		 */
		public Boolean success = null;
		/** If true the recorded event was assigned to a real event even if the timing and/or values were not correct*/
		public boolean isFound = false;
		
		public long timeLatest;
		public long timeEarliest;
	}
	
	/** To display all events recorded / scheduled for replay*/
	List<EventRecorded> events();
	
	/** The following methods are just added for documentation purposes. They are called by applications
	 * using specific implementations and not this interface.
	 */
	public List<RecReplayDeviation> checkInitialReplay();

	/** Report new event. Note that the signature of this method depends on the implementation so it is
	 * not part of the interface
	 * @param ac
	 * @return null if alarm is expected as is
	 */
	//public RecReplayDeviation newEvent(AlarmConfiguration ac, long now);

	/** Generate deviation objects for all alarms that were expected by now, but did not occur
	 */
	public List<RecReplayDeviation> consolidateMissingEvents(long now);
}
