package org.smartrplace.apps.hw.install.prop.heartbeat;

/** This represents an established connection on a server or client.
 */
public interface HeartbeatConnection extends HeartbeatConnectionRegistration {
	boolean isServer();
}
