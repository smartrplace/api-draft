package org.smartrplace.apps.hw.install.prop.heartbeat;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ValueResource;

public interface HeartbeatResourceSyncConfig {
	/** The parent resource path is the same on the client and on the server. On the server the real
	 * resource path depends on the type and the application path
	 */
	String parentResourcePath();
	
	
	public enum ResourceType {
		/** All resources below the parentResourcePath are synchronized from the server to all
		 * clients with the same resource paths on server and client.
		 */
		APPLICATION_GENERAL,
		
		/** All resources below parentResourcePath are synchronized bidirectional, but the server generates
		 * sub resources different for each gateway. So the actual sub resources are determined by the
		 * server, but synchronization is done two-way as default.
		 */
		APPLICATION_GATEWAY_ELEMENT,
		
		/** The sub resources synchronized are determined by the client. The respective resource
		 * on the server is put into serverMirror/<gwDirectory>. Synchronization is done from gateway
		 * to server only as default.
		 */
		DEVICE
	}
	ResourceType getResourceType();
	String applicationPath();
	
	public enum SynchType {
		TwoWay_ServerPriority,
		TwoWay_ClientPriority,
		FromClientToServer,
		FromServerToClient
	}
	default SynchType getSynchType() {
		switch(getResourceType()) {
		case APPLICATION_GENERAL:
			return SynchType.TwoWay_ServerPriority;
		case APPLICATION_GATEWAY_ELEMENT:
			return SynchType.TwoWay_ClientPriority;
		case DEVICE:
			return SynchType.TwoWay_ClientPriority;
		default:
			throw new IllegalStateException("Unknown type:"+getResourceType());
		}
	}
	
	/** All resources that exist either on client or on server are synchronized. This method returns the
	 * actual parent resource on the system the method is called.*/
	Resource parentResource();
	
	/** ValueResources that are not synchronized are not created via heartbeat.
	 * @param resource
	 * @return
	 */
	boolean synchronizeValue(ValueResource resource);
	
	/** If a non-ValueResource has no children that shall be sychronized then this method determines
	 * whether it shall still be created via heartbeat 
	 * @param resource
	 * @return
	 */
	default boolean createResource(Resource resource) {
		return false;
	}
}
