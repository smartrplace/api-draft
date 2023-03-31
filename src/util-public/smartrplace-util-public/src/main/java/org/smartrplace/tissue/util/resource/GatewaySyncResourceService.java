package org.smartrplace.tissue.util.resource;

import java.util.Arrays;
import java.util.concurrent.CompletionStage;

import org.ogema.core.model.Resource;
import org.ogema.core.model.ResourceList;
import org.ogema.model.locations.Room;
import org.ogema.model.prototypes.PhysicalElement;
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
 * for each cascading connection.
 */
public interface GatewaySyncResourceService {
	
	public static enum Reason {
		RESOURCE_NOT_SYNCHRONIZED("NOT_SYNCHRONIZED"),
		RESOURCE_NOT_FOUND("NOT_FOUND"),
		INVALID("INVALID"),
		INTERNAL_ERROR("INTERNAL_ERROR");
		
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
	 * Deactivate resource locally and the respective resource on the other side of cascading if the
	 * resource is inside a cascading tree.
	 * @param res
	 * @param recursive
	 * @return 
	 */
	<R extends Resource> CompletionStage<RemoteResourceStatus<R>> delete(R res);
	/**
	 * Create a resource locally, and the corresponding resource on the other side of cascading 
	 * if the resource is inside a cascading tree; For non-toplevel resources, a virtual resource can be passed.
	 * @param <R>
	 * @param resource
	 * @return 
	 */
	<R extends Resource> CompletionStage<RemoteResourceStatus<R>> create(R resource);
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
	
	
	CompletionStage<RemoteStatus> setRoomAsReference(PhysicalElement device, Room Room);
	CompletionStage<RemoteStatus> setDeviceAsReference(PhysicalElement newReference, PhysicalElement deviceToBeReferenced);
	
	CompletionStage<RemoteStatus> restartBundle(BundleType type);
	CompletionStage<RemoteStatus> restartBundle(String bundleSymbolicName);
}
