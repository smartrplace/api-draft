package org.smartrplace.apps.heatcontrol.extensionapi;

import java.util.List;

import org.ogema.core.model.Resource;

/** Service offered by the app org.smartrplace.apps.smartrplace-heatcontrol-v2
 * 
 */
public interface HeatControlExtPoint {
	boolean getEcoModeState();
	void setEcoModeState(boolean state);
	
	/** Get access to rooms controlled and the relevant data*/
	List<HeatControlExtRoomData> getRoomsControlled();
	
	/** The heating core application shall take care of additional global configuration
	 * resources for applications to make the management
	 * of room resources more efficient
	 * 
	 * @param forceCreation if true the subresource will be created if not available yet
	 * if false the method will return false if the resource is not yet available
	 */
	public abstract <R extends Resource> R extensionData(boolean forceCreation,
			Class<R> resourceType);

	
	public interface HeatControlExtRoomListener {
		public enum CallbackReason {
			/** Room is newly found*/
			NEW,
			/** For each known room a callback is made on
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
