package org.smartrplace.apps.hw.install.prop.heartbeat;

import org.ogema.core.model.Resource;

public interface ViaHeartbeatService {
	/** A connection must be registered with the same id both on client and server side. Usually this is done
	 * by an application that runs both client and server side and that uses the same method call on both sides.
	 * So on standard applications (e.g. roomcontrol, hardware-installation)
	 * the same data of {@link HeartbeatResourceSyncConfig}s is available on server and
	 * client side, but server side shall be most relevant. For some applications it may only be provided
	 * on the server and transferred to the client.
	 * @param registrationData
	 * @param registerAsServer on systems that connect via heartbeat as client and as server for each
	 * 		connection may have to be defined whether a client or a server role is taken. Otherwise based
	 * 		on the genereal gateway settings (TODO: define here) the server/client status is determined.
	 * @return
	 */
	HeartbeatConnection registerConnection(HeartbeatConnectionRegistration registrationData,
			boolean registerAsServer);
	HeartbeatConnection registerConnection(HeartbeatConnectionRegistration registrationData);
	
	/** On the client this simplified registration may be used for applications that work via
	 * server-side-only registration.
	 * TODO: This may be available in the future*/
	//HeartbeatConnection registerConnection(String id);
	
	public enum ChangeType {
		/**IF the resource is created and activated this should be notified as a single change*/
		CREATED_INACTIVE,
		CREATED_ACTIVE,
		CREATED_REFERENCE,
		//ACTIVATED,
		DELETED,
		//DEACTIVATED
	}
	/** If a resource and sub resources are changed, then only the most toplevel resource
	 * shall be notified here. If a new reference is set then no changes in the resource referenced
	 * are checked. Before a reference is set the resource referenced should be provided in a
	 * {@link HeartbeatConnectionRegistration} so that the right reference can be found.
	 * @param res
	 * @param changeType can usually be set null as this is determined automatically
	 * @return The return type identified
	 */
	ChangeType resourceStructureChangeNotification(Resource res, ChangeType changeType);
}
