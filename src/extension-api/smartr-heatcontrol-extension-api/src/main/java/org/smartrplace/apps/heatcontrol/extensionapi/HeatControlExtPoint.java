package org.smartrplace.apps.heatcontrol.extensionapi;

import java.util.List;

/** Service offered by the app org.smartrplace.apps.smartrplace-heatcontrol-v2
 * 
 */
public interface HeatControlExtPoint {
	boolean getEcoModeState();
	void setEcoModeState(boolean state);
	
	/** Get access to rooms controlled and the relevant data*/
	List<HeatControlExtRoomData> getRoomsControlled();
	
	public interface HeatControlExtRoomListener {
		public enum CallbackReason {
			/** Room is newly found, this is also used if a new room is found 
			 * on startup*/
			NEW,
			/** Room is known, but for each known room a callback is made on
			 * call of {@link HeatControlExtPoint#registerRoomListener(HeatControlExtRoomListener)}
			 */
			STARTUP,
			/** When the devices attached to the room change a listener call is
			 * issued. In most cases the extension app will restart its operation
			 * on the room for simplified implementation. This is feasible if no
			 * non-persistent states have to be maintained even over changes in
			 * device configuration.<br>
			 * Note that this reason may be used with roomUnavailble if a room management was
			 * disabled and the device configuration changed
			 */
			UPDATE,
			/** Used with {@link HeatControlExtPoint#unregisterRoomListener(HeatControlExtRoomListener)}. 
			 *  Called when a room is entirely removed from the system or the user requests to
			 *  forget all heat control settings for a room
			 */
			DELETED,
			/** Used with {@link HeatControlExtPoint#unregisterRoomListener(HeatControlExtRoomListener)}.
			 * Called when the heat control management of a room shall be disabled, but peristent
			 * settings shall be kept in the system.
			 */
			DISABLED
		}
		void roomAvailable(HeatControlExtRoomData roomData, CallbackReason reason);
		void roomUnavailable(HeatControlExtRoomData roomData, CallbackReason reason);
	}
	void registerRoomListener(HeatControlExtRoomListener listener);
	void unregisterRoomListener(HeatControlExtRoomListener listener);
}
