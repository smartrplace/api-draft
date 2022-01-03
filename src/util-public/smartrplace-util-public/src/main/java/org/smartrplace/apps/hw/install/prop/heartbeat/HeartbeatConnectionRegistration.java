package org.smartrplace.apps.hw.install.prop.heartbeat;

import java.util.Collection;

/** This is a data transfer connection that is registered e.g. by a certain application.
 * A connection with the same id must be registered on both sides of the client/server connection.
 * More than one connection can be registered for one heartbeat connection.
 * These configurations mainly synchronize resources, they are not intended for datapoint timeseries
 * transmission.<br>
 * Usually the application has to be prepared for client/server operation. On the server most of the logic has
 * to be switched off, only a data mirror and the GUI/servlet is operated. The GUI
 */
public interface HeartbeatConnectionRegistration {
	String id();
	Collection<HeartbeatResourceSyncConfig> resourceSynchronizations();
	
	/** Receive actual connection when started.*/
	void start(HeartbeatConnection connection);
}
