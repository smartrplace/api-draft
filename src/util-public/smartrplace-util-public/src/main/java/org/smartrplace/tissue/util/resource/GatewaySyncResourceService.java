package org.smartrplace.tissue.util.resource;

import java.util.Arrays;
import java.util.concurrent.CompletionStage;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.drivers.homematic.xmlrpc.hl.types.HmLogicInterface;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
import org.smartrplace.apps.hw.install.prop.ViaHeartbeatUtil;
import org.smartrplace.model.sync.mqtt.GatewaySyncData;
import org.smartrplace.os.util.OSGiBundleUtil.BundleType;

/** Note: This service shall use the fields in {@link GatewaySyncData#commandsGwToServer()} etc.
 * It shall determine itself whether it is running on server or gateway by properties and in
 * some cases by arguments.<br>
 * The property org.smartplace.app.srcmon.server.issuperior indicates the main superior instance,
 * which is always server.
 * Otherwise if the property org.ogema.devicefinder.util.supportcascadedccu is NOT set true
 * we always assume the system acting as gateway. 
 * If neither condition applies then the destination has to be determined by the position in
 * the cascading tree. The Cascading trees are defined in {@link GatewaySyncData#toplevelResourcesToBeSynchronized()}
 * for each cascading connection. Whether a GatewaySyncData entry is relevant for you as client or as
 * server can be determined based on the own gatewayId. This can be determined by
 * {@link GatewayUtil#getGatewayId(org.ogema.core.resourcemanager.ResourceAccess)},
 * to make sure got get only 5-digit-id process the result by {@link ViaHeartbeatUtil#getBaseGwId(String)}.
 */
public interface GatewaySyncResourceService {
	
	public static enum Reason {
		RESOURCE_NOT_SYNCHRONIZED("NOT_SYNCHRONIZED"),
		RESOURCE_NOT_FOUND("NOT_FOUND"),
		INVALID("INVALID"),
		INTERNAL_ERROR("INTERNAL_ERROR"),
		SYNCHRONIZATION_DISABLED("SYNCHRONIZATION_DISABLED"),;
		
		
		private final String value;
		
		private Reason(String value) {
			this.value = value;
		}
		
		public static Reason fromValue(String value) {
			return Arrays.stream(Reason.values())
				.filter(en -> en.value.equalsIgnoreCase(value))
				.findAny()
				.orElseThrow(() -> new IllegalArgumentException("Invalid response type " + value));
		}
		
		public String getValue() {
			return value;
		}
		
	}
	
	static interface RemoteStatus {
		
		boolean success();
	
		/**
		 * Null if success is true
		 */
		Reason reason();
		/**
		 * Typically null if success is true.
		 */
		String details();
		
	}
	
	static interface RemoteResourceStatus<R extends Resource> extends RemoteStatus {
		
		/**
		 * The local resource.
		 * May be null if success if false
		 */
		R resource();
		
	}
	
	/** 
	 * Activate resource locally and the respective resource on the other side of cascading if the
	 * resource is inside a cascading tree.
	 * @param res
	 * @param recursive
	 * @return 
	 */
	<R extends Resource> CompletionStage<RemoteResourceStatus<R>> activateResource(R res, boolean recursive);
	/** 
	 * Deactivate resource locally and the respective resource on the other side of cascading if the
	 * resource is inside a cascading tree.
	 * @param res
	 * @param recursive
	 * @return 
	 */
	<R extends Resource> CompletionStage<RemoteResourceStatus<R>> deactivateResource(R res, boolean recursive);
	/** 
	 * Delete resource locally and the respective resource on the other side of cascading if the
	 * resource is inside a cascading tree.
	 * @param res
	 * @param recursive
	 * @return 
	 */
	<R extends Resource> CompletionStage<RemoteResourceStatus<R>> delete(R res);
	/**
	 * Create a resource locally, and the corresponding resource on the other side of cascading 
	 * if the resource is inside a cascading tree; For non-toplevel resources, a virtual resource can be passed.
	 * If the resource exists then its value and active status are transferred, as well. Hence, this method can
	 * also be used for out-of-band updates of the remote resource.
	 * 
	 * @param <R>
	 * @param resource
	 * @return 
	 */
	<R extends Resource> CompletionStage<RemoteResourceStatus<R>> create(R resource);
	/**
	 * Relevant for resources that already exist on the calling side. It will copy also all subresources
	 * to the other side of cascading, including their active status. 
	 * @param <R>
	 * @param resource
	 * @param recursive
	 * @return
	 */
	<R extends Resource> CompletionStage<RemoteResourceStatus<R>> create(R resource, boolean recursive);
	/**
	 * Relevant for resources that already exist on the calling side. It will copy also all subresources
	 * to the other side of cascading, including their active status. 
	 * @param <R>
	 * @param resource
	 * @param recursive
	 * @param maxDepth
	 * @param includeSchedules
	 * @param strict if set to true and the creation of some subresource fails, then the whole transaction 
	 * on the other side of the cascading is considered as failed and will be rolled back. 
	 * Otherwise (the default), a partial transfer of the resource is realized. TODO rollback is not implemented,
	 * instead the operation simply terminates upon the first subresource that cannot be created.
	 * @return
	 */
	<R extends Resource> CompletionStage<RemoteResourceStatus<R>> create(R resource, boolean recursive, 
			int maxDepth, boolean includeSchedules, boolean strict);
	/**
	 * Add an element to a resource list locally, and the corresponding resource on the other side of cascading 
	 * if the resource is inside a cascading tree
	 * @param <R>
	 * @param parent
	 * @param name
	 * @param type
	 * @return 
	 */
	<R extends Resource> CompletionStage<RemoteResourceStatus<R>> addToResourceList(ResourceList<R> list, String name);
	/**
	 * See {@link #addToResourceList(ResourceList, String)}
	 */
	<R extends Resource> CompletionStage<RemoteResourceStatus<R>> addToResourceList(ResourceList<R> list);
	
	/**
	 * Send a request to the remote side to provide send the current subresource structure of the provided resource. 
	 * @param <R>
	 * @param resource
	 * @return
	 */
	<R extends Resource> CompletionStage<RemoteResourceStatus<R>> requestSync(R resource);
	
    /** Room must be identified by name*/
	CompletionStage<RemoteStatus> setRoomAsReference(PhysicalElement device, Room Room);
	CompletionStage<RemoteStatus> setDeviceAsReference(PhysicalElement newReference, PhysicalElement deviceToBeReferenced);
	
	CompletionStage<RemoteStatus> restartBundle(BundleType type, String gatewayId);
	CompletionStage<RemoteStatus> restartBundle(String bundleSymbolicName, String gatewayId);

	/** Reboot CCU device */
	default CompletionStage<RemoteStatus> rebootCCU(HmLogicInterface ccu, String gatewayId) {
		return null;
	}
}
