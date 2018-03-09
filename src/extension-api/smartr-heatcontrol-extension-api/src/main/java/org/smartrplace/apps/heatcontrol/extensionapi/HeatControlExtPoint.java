package org.smartrplace.apps.heatcontrol.extensionapi;

import java.util.List;

import org.ogema.model.locations.Room;

/** Service offered by the app org.smartrplace.apps.smartrplace-heatcontrol-v2
 * 
 */
public interface HeatControlExtPoint {
	boolean getEcoModeState();
	void setEcoModeState(boolean state);
	/**TODO: This is just an initial idea*/
	List<Room> getRoomsControlled();
}
